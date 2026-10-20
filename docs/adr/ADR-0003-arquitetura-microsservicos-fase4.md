# ADR-0003: Arquitetura de Microsserviços e Saga (Fase 4)

- **Status:** Aceito
- **Data:** 2026-10-02
- **Supersede parcialmente:** [ADR-0001](./ADR-0001-padrao-comunicacao.md) (comunicação entre domínios passa a usar broker externo)

## Contexto

A Fase 4 do Tech Challenge exige refatorar a aplicação (hoje um monólito
modular hexagonal — `workbench-monolith`) para uma arquitetura distribuída com, no
mínimo, 3 microsserviços independentes, cada um com repositório, infraestrutura
e banco de dados próprios. São requisitos obrigatórios:

- Pelo menos um banco relacional (SQL) e pelo menos um não relacional (NoSQL).
- Nenhum serviço pode acessar diretamente o banco de outro serviço.
- Comunicação por REST síncrono (quando necessário) e mensageria assíncrona.
- Saga Pattern para coordenar o fluxo transacional (abertura da OS → orçamento
  → aprovação → execução), com rollback e compensação em caso de falha.

O monólito atual já favorece a divisão: os agregados são acoplados por
identificador (OS→cliente por `cpfCnpj`, OS→veículo por placa, reserva→OS por
`serviceOrderId`), não por associação de objeto, e cada módulo tem fronteiras
hexagonais espelhadas (domain / application / adapter).

## Decisão

### 1. Divisão em 4 microsserviços

| Serviço | Bounded context | Módulos atuais migrados | Banco |
|---|---|---|---|
| **Customer Service** | Clientes + veículos + validações (CPF/CNPJ, placa, vínculo dono-veículo) e auth de cliente por CPF | `client`, `vehicle` | PostgreSQL (SQL) |
| **OS Service** | Ciclo de vida da OS, máquina de estados e **orquestração da Saga** | `serviceorder`, `service` | PostgreSQL (SQL) |
| **Billing Service** | Orçamento + pagamento (Mercado Pago) + atualização de status pós-pagamento | `budget` + novo `payment` | PostgreSQL (SQL) |
| **Execution Service** | Fila de execução, diagnóstico/reparos, estoque e produtos | `stock`, `product` | **MongoDB (NoSQL)** |

Cliente e veículo foram mantidos **no mesmo serviço** por coesão de domínio
(veículo não existe sem dono; FK real `fk_vehicles_clients` hoje) e para evitar
uma chamada cruzada em toda operação de CRUD de veículo. Começar junto e
separar depois, se houver motivo real (ex.: integração FIPE), é mais barato que
o inverso.

### 2. Mensageria: SNS → SQS com DLQ

A comunicação assíncrona entre serviços usa **Amazon SNS (tópico) → Amazon SQS
(fila por consumidor)**, mantendo todo o stack na AWS (coerente com o uso atual
de SNS para notificação). Cada serviço consumidor tem a própria fila inscrita no
tópico (fan-out), com **dead-letter queue (DLQ)** para mensagens que falham após
as tentativas de reprocessamento.

REST síncrono fica reservado para validações pontuais em tempo de request (ex.:
OS Service consultando Customer Service se o cliente/veículo existe e está ativo
na criação da OS).

### 3. Saga Pattern: Orquestrada pelo OS Service

Adotamos **Saga orquestrada**, com o **OS Service como orquestrador** central,
comunicando com os demais serviços **via mensageria (SNS/SQS)** — orquestração
central **com** desacoplamento assíncrono, não chamadas REST em cadeia.

Fluxo transacional (happy path):

1. OS Service abre a OS (`RECEBIDA`) e comanda a reserva de estoque.
2. Execution Service reserva e responde `ReservaConfirmada`.
3. OS Service comanda o Billing a gerar o orçamento.
4. Cliente aprova → Billing cria o pagamento no Mercado Pago e aguarda o webhook.
5. Pagamento confirmado → Billing responde → OS avança para `EM_EXECUCAO`.

Compensações (rollback distribuído):

- Falha no pagamento → OS reverte a aprovação e comanda o Execution a **liberar
  as reservas**; Billing cancela o orçamento.
- Falha na reserva → OS cancela o orçamento e marca a OS como falha.

## Justificativa da Saga orquestrada

- **Decisão humana no meio do fluxo** (aprovação do orçamento): a Saga precisa
  pausar e aguardar um evento externo — natural com um orquestrador que mantém o
  estado da Saga; penoso em coreografia.
- **Rollback/compensação explícitos** (exigidos pelo PDF): um ponto único sabe
  "cheguei ao passo N, preciso desfazer N-1..1", tornando as compensações
  simples de implementar e testar.
- **Observabilidade e demonstração**: o estado da Saga vive num lugar só,
  facilitando o rastreamento distribuído e a demonstração de falha/compensação
  no vídeo.

## Consequências

- Cada serviço ganha repositório, pipeline CI/CD, Dockerfile, manifests K8s e
  meta de cobertura (≥80%) próprios.
- Introdução de broker (SNS/SQS) adiciona infraestrutura a provisionar e
  **contratos de mensagem a versionar** — superfície operacional maior que a do
  monólito (trade-off aceito e exigido pela fase).
- Consistência passa a ser **eventual** entre serviços; o OS Service persiste o
  estado da Saga (padrão outbox/estado de saga) para sobreviver a quedas.
- A auth de cliente por CPF (`workbench-lambda`) deixa de ler a tabela de
  clientes e passa a depender do **Customer Service**.
- Mensagens devem ser **idempotentes** (SQS garante entrega *at-least-once*).

## Alternativas Rejeitadas

- **Saga coreografada:** rejeitada pela dificuldade de coordenar compensação e
  pela ausência de um ponto de visibilidade do estado da Saga, agravada pela
  etapa de aprovação humana.
- **Customer e Vehicle como serviços separados:** rejeitado por baixa coesão de
  escopo (perfis de carga idênticos, vínculo forte de domínio) e maior custo de
  entrega sem ganho real.
- **RabbitMQ/Kafka:** rejeitados para manter o stack AWS (SNS/SQS) já em uso e
  disponível no AWS Academy, sem operar um broker adicional.
