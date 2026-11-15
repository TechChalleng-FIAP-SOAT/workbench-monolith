# ADR-0008: Robustez da Saga — rodadas, concorrência e confirmação do pagamento

- **Status:** Aceito
- **Data:** 2026-11-15
- **Complementa:** [ADR-0005](./ADR-0005-fluxo-os-estoque-pagamento.md) (fluxo da OS) e [ADR-0006](./ADR-0006-arquitetura-microsservicos-fase4.md) (Saga orquestrada)

## Contexto

A Saga roda com 2+ réplicas por serviço (HPA) e sobre SQS, que entrega "pelo menos uma
vez". As auditorias de 14 e 15/11/2026 acharam casos em que a OS travava ou o estoque
divergia, todos ligados a concorrência e a mensagens repetidas ou atrasadas:

- Cliente aprovando e equipe recusando o mesmo orçamento ao mesmo tempo: as duas decisões
  gravavam, e o execution recebia baixa **e** liberação da reserva.
- Um `ReservaFalhou` antigo, reentregue depois de um novo diagnóstico, derrubava a Saga nova
  (o `sagaId` era o id da OS em todas as tentativas).
- Duas réplicas com o mesmo `ComandoCobrarPagamento` criavam duas preferências no Mercado Pago.
- Timeout esgotado na cobrança deixava a Saga em `FALHOU`, sem nova cobrança possível.

Além disso, a confirmação do pagamento precisava funcionar sem expor um endpoint público
ao Mercado Pago (ver [ADR-0007](./ADR-0007-rede-e-segredos.md)).

## Decisão

1. **Uma rodada por diagnóstico.** Cada início da Saga, ou novo diagnóstico depois de uma
   falha, gera um `sagaId` novo, gravado no `saga_state` e enviado em todos os comandos. O
   orquestrador descarta resposta com `sagaId` de outra rodada. Linhas antigas recebem o id
   da OS (o valor usado antes), para não perder respostas em trânsito. Todo participante
   devolve o `sagaId` que recebeu. O resultado do pagamento não responde a um comando (sai
   do job de consulta ou do webhook simulado), então o billing **grava o `sagaId` do
   `ComandoCobrarPagamento` na cobrança** (`payments.saga_id`, migration V9) e publica
   `PagamentoConfirmado`/`PagamentoFalhou` com ele.
2. **Trava otimista no estado da Saga** (`@Version` em `saga_state`). Duas transações que
   leram o mesmo estado não gravam as duas; a segunda recebe **409**, e como os comandos só
   saem depois do commit, nada é enviado por ela.
3. **Cada evento só no passo esperado** (já existia) e **participantes idempotentes**:
   reservar, baixar, liberar, gerar orçamento e cobrar duas vezes dão o mesmo resultado. No
   execution, baixar uma reserva já liberada (ou o contrário) é conflito.
4. **Cobrança serializada por OS:** a linha do orçamento é travada (`SELECT ... FOR UPDATE`)
   antes de ler o pagamento. A segunda réplica espera e devolve a cobrança da primeira.
5. **Timeout depois da aprovação não encerra a Saga:** esgotadas as tentativas de
   `ComandoConfirmarReserva` ou `ComandoCobrarPagamento`, a Saga registra o motivo e
   continua em `AGUARDANDO_PAGAMENTO` (nova cobrança e resposta atrasada continuam
   aceitas). O reenvio por timeout é uma transação por OS.
6. **Pagamento confirmado por consulta, não por webhook:** o billing consulta o Mercado
   Pago a cada 30 s (`GET /v1/payments/search?external_reference=`), com ShedLock para uma
   réplica por janela e timeout nas chamadas. O webhook só existe simulado, no perfil local.

## Consequências

- **Positivo:** os casos de OS travada e de estoque divergente encontrados nas auditorias
  têm teste: concorrência com Postgres real (`SagaStateConcurrencyIntegrationTest`,
  `ConcurrentChargeIntegrationTest`) e unitários para rodada antiga e timeouts.
- **Positivo:** nenhum endpoint de pagamento precisa estar na internet.
- **Negativo:** a confirmação tem atraso de até ~30 s, e o job consulta o Mercado Pago mesmo
  sem pagamento novo.
- **Negativo:** a trava da cobrança segura a linha do orçamento durante a chamada ao
  gateway (até o timeout de leitura, 10 s).
- **Em aberto:** a reserva de estoque não tem prazo — uma OS nunca decidida mantém as peças
  reservadas (precisa de regra de negócio); o dedupe por `messageId` continua em memória,
  como otimização, e não como garantia.

## Alternativas Rejeitadas

- **Outbox transacional:** resolveria "gravou mas não publicou" de forma geral, mas o
  reenvio por timeout do orquestrador e a idempotência dos participantes já cobrem esse
  caso com menos infraestrutura.
- **Trava pessimista no estado da Saga:** serializaria todo evento da OS, inclusive os que
  não conflitam; a trava otimista só falha quando há conflito real.
- **Webhook do Mercado Pago:** exigiria endpoint público com validação de assinatura
  (`x-signature`), contrariando a [ADR-0007](./ADR-0007-rede-e-segredos.md).
- **Chave de idempotência no Mercado Pago na criação da preferência:** evita o duplicado lá,
  mas não no banco do billing; a trava resolve os dois.
