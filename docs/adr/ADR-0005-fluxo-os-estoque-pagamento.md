# ADR-0005: Fluxo da OS, Estoque (reserva/baixa/retorno) e Pagamento

- **Status:** Aceito
- **Data:** 2026-10-15
- **Relaciona-se com:** [ADR-0003](./ADR-0003-arquitetura-microsservicos-fase4.md) (microsserviços + Saga)

## Contexto

O PDF da Fase 4 dá apenas um **exemplo** de fluxo transacional ("abrir OS → gerar
orçamento → aguardar aprovação → enviar para execução") e lista o pagamento (Mercado
Pago) nas responsabilidades do Billing, **sem fixar a ordem** entre aprovar o orçamento
e pagar, nem as regras de estoque. Essas lacunas foram decididas pelo time e são
registradas aqui para rastreabilidade — **não são do enunciado**.

## Decisão

### 1. Fluxo (máquina de estados) da OS

```
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → APROVADO → EM_EXECUCAO
  → FINALIZADA → PAGAMENTO_PENDENTE → PAGO → ENTREGUE
                       (ramo de recusa: AGUARDANDO_APROVACAO → RECUSADA)
```

- O **pagamento ocorre no fim**, após a finalização do serviço (não na aprovação).
- **ENTREGUE ("liberar veículo") só após PAGO** — pagamento é pré-condição da entrega.
- Novos estados em relação à Fase 3: `PAGAMENTO_PENDENTE`, `PAGO`.

### 2. Estoque: reserva → baixa → retorno (dono: Execution Service)

- Diagnóstico: cria **reserva `ACTIVE`** — o estoque fica *preso* (conta como
  `reserved`), mas **não** é baixado. Disponível = `quantity - reserved`.
- Aprovação: reserva vira **`CONFIRMED`** → **baixa definitiva** (saída de estoque).
- **Recusa do orçamento** (antes de executar): reserva vira **`RELEASED`** →
  quantidade **volta ao disponível** (o serviço nunca foi feito).
- **Falha de pagamento** (depois de executar): **NÃO há estorno de estoque** — as
  peças já foram aplicadas no veículo. Ver seção 6.
- O estoque é **exclusivo do Execution Service**; nenhum outro serviço acessa seu banco.

### 3. Dois modos de comunicação com o estoque

- **Consulta de disponibilidade** (ao montar o orçamento): **REST síncrono** ao
  Execution (`GET /stock/availability`). Precisa de resposta imediata. Se faltar
  item, a resposta informa **nome da peça e quantidade disponível**.
- **Reserva / baixa / liberação** (efeitos conforme a Saga avança): **mensageria
  assíncrona** (comandos/eventos SNS/SQS).

O PDF permite REST "quando necessário" + mensageria assíncrona — usamos cada um
onde cabe.

### 4. API de estoque

O Execution expõe REST para **adicionar/remover peças** e **entrada/saída** de
estoque, além da consulta de disponibilidade.

### 5. Pagamento (Mercado Pago) via porta

- `PaymentGatewayPort` com adapters intercambiáveis:
  - **fake** (default) — determinístico, offline; usado em testes e dev.
  - **Mercado Pago sandbox** — homologação e demonstração.
  - **produção** — troca apenas de credencial/URL (sem mudança de código).
- **Histórico de pagamento** persistido (append-only) para auditoria e para o
  requisito "registro e verificação de pagamentos".

### 5.1 Compensação: o que de fato se reverte (princípio)

Saga é consistência por **ações compensatórias**, não por rollback de transação
distribuída. O rollback local de um serviço não desfaz o que já foi efetivado em
outro — para desfazer, é preciso uma **ação inversa**. E o ponto central: **nem toda
falha compensa**. Só se compensa uma operação anterior que precise ser revertida
para manter a consistência; se nada precisa ser desfeito, a falha é apenas **tratada**
(retry, registrar, notificar).

No nosso fluxo, a **única** operação que precisa ser revertida é a **reserva de
estoque — e somente enquanto ela ainda não virou baixa** (reserva `ACTIVE`, peça
ainda na prateleira). Depois da aprovação o estoque é **baixado** (`CONFIRMED`) e o
serviço é executado (peça instalada no veículo): a partir daí **não há reversão de
estoque** possível sem criar inconsistência física (o sistema diria "peça disponível"
com a peça já no carro).

Mapa de falhas × ação (ADR):

| Falha | Já efetivado antes | Reverter? | Ação |
|---|---|---|---|
| Reserva falha | nada | não | tratar erro: abortar, marcar falha |
| **Orçamento falha (após reserva)** | reserva `ACTIVE` | **sim** | **retry N vezes; esgotado → compensar liberando a reserva** |
| Recusa do orçamento (humano) | reserva `ACTIVE` | **sim** | compensar: liberar a reserva |
| Pagamento falha (após execução) | estoque baixado + serviço feito | **não** | tratar erro: reter em `PAGAMENTO_PENDENTE` + lembrete |

**Retry antes de compensar.** Falhas podem ser transitórias; o orquestrador reenvia o
comando da etapa atual até `saga.max-retries` (padrão 3) antes de compensar. O
reenvio é disparado tanto pelo evento de falha (ex.: `OrcamentoFalhou`) quanto por
**timeout** (comando/resposta perdido): o `SagaState` guarda o comando pendente,
o nº de tentativas e o instante do último envio; um job agendado (`@Scheduled` +
**ShedLock**, uma réplica por janela) reenvia o que venceu e, esgotado, compensa.
Isso implementa o "se não for possível recuperar após algumas tentativas, então
compensar".

### 6. Falha de pagamento = OS retida (não é rollback)

- Se o pagamento falha, a OS **permanece em `PAGAMENTO_PENDENTE`** (não há estado
  novo). O veículo **nunca é liberado** (não vai para `ENTREGUE`) até a quitação.
- **Não há estorno de estoque** nessa falha — o serviço já foi executado e as peças
  aplicadas no veículo. É um estado de **espera/retenção**, não uma compensação.
- Nova tentativa de pagamento é possível a partir de `PAGAMENTO_PENDENTE` (nova cobrança).
- **Lembrete periódico**: enquanto em `PAGAMENTO_PENDENTE`, um job agendado notifica o
  cliente **a cada ~2 dias** sobre o pagamento pendente e a retenção do veículo.
  Controlado por um campo `last_payment_reminder_at` na OS. Processo recorrente **à
  parte da Saga** (a Saga coordena a transação; o lembrete opera sobre o estado retido).
  - **Implementação: `@Scheduled` no OS Service + ShedLock** (lock distribuído com
    backend no próprio Postgres do serviço). O ShedLock garante que, mesmo com várias
    réplicas (HPA), **apenas uma** execute o job por janela — evitando notificação
    duplicada. Mantém a responsabilidade no dono do status da OS, sem violar o
    isolamento de banco (ninguém externo lê o banco do OS Service).
  - Rejeitado: `@Scheduled` puro (duplica com réplicas) e Lambda agendada (precisaria
    acessar os dados das OS pendentes, acoplando-se ao OS Service com mais infra).
- Distinção-chave: **compensação de estoque** só ocorre na **recusa/falha do orçamento**
  (antes da execução, reserva ainda `ACTIVE`), nunca na falha de pagamento (depois da
  execução). A falha de pagamento é **tratamento de erro** (reter + notificar), não
  compensação — ver a tabela na seção 5.1.

## Consequências

- O `OrderServiceStatusEnum` e o CHECK de status no Flyway do `workbench-os-service`
  precisam incluir `PAGAMENTO_PENDENTE` e `PAGO` (migration V3).
- O `SagaState` rastreia o comando pendente, o nº de tentativas e o instante do último
  envio (migration V7) para suportar retry/timeout; a tabela do **ShedLock** (migration
  V8) sustenta os jobs agendados (retry por timeout e lembrete de pagamento) com uma
  execução por janela entre réplicas.
- O Billing publica **`OrcamentoFalhou`** quando a geração do orçamento falha, para o
  orquestrador decidir retry/compensação.
- **Execution Service vem antes do Billing** na implementação (o Billing depende da
  consulta REST de disponibilidade e das reservas).
- **Banco do Execution: MongoDB (decidido).** O Execution é o único serviço NoSQL
  (os demais são Postgres), então o Mongo precisa ficar aqui para cumprir o requisito
  obrigatório do PDF. Optou-se por **Mongo puro** (um serviço = um banco) em vez de
  Postgres+Mongo, evitando dois datasources no mesmo serviço. A reserva de estoque
  (reserva→baixa→retorno), que é atômica, será garantida com **transações
  multi-documento do MongoDB** (suportadas desde a 4.0) sobre as coleções `stock`
  e `stock_reservation`. Exige rigor na sessão transacional, aceito como trade-off.

## Alternativas rejeitadas

- **Pagar na aprovação do orçamento:** rejeitado — o time definiu pagamento ao final
  (pós-finalização), com a entrega condicionada ao pagamento.
- **Redis para consulta de estoque:** rejeitado — a verdade do estoque é do Execution
  (banco transacional); REST síncrono à API do Execution evita cache/invalidação
  desnecessários e respeita o isolamento de banco.
