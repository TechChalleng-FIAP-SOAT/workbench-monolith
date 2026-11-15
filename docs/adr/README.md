# ADRs — Architecture Decision Records

Registro das decisões arquiteturais **permanentes** do projeto OS Management —
decisões estruturais que, uma vez tomadas, moldam como o código e a
infraestrutura evoluem daqui para frente. Para o processo de avaliação de
alternativas que levou a uma decisão técnica pontual, ver os [RFCs](../rfc/README.md).

## Índice

| ADR | Título | Status |
|---|---|---|
| [ADR-0001](ADR-0001-padrao-comunicacao.md) | Padrão de comunicação entre componentes | Aceito |
| [ADR-0002](ADR-0002-uso-hpa.md) | Uso de HPA para escalabilidade horizontal | Aceito |
| [ADR-0003](ADR-0003-split-multiplos-repositorios.md) | Split em múltiplos repositórios com CI/CD independente | Aceito |
| [ADR-0004](ADR-0004-arquitetura-hexagonal.md) | Arquitetura Hexagonal (Ports & Adapters) no domínio | Aceito |
| [ADR-0005](ADR-0005-fluxo-os-estoque-pagamento.md) | Fluxo da OS, estoque (reserva/baixa/retorno) e pagamento | Aceito |
| [ADR-0006](ADR-0006-arquitetura-microsservicos-fase4.md) | Arquitetura de microsserviços e Saga orquestrada (Fase 4) | Aceito |
| [ADR-0007](ADR-0007-rede-e-segredos.md) | Rede e segredos: serviços fora da internet e falha fechada | Aceito |
| [ADR-0008](ADR-0008-robustez-da-saga.md) | Robustez da Saga: rodadas, concorrência e confirmação do pagamento | Aceito |

## Convenção

Cada ADR segue a estrutura:

1. **Contexto** — a força/restrição que motivou a decisão.
2. **Decisão** — o que foi decidido, de forma direta.
3. **Consequências** — o que passa a ser verdade (positivo e negativo) por
   causa da decisão.
4. **Alternativas Rejeitadas** — o que foi descartado e por quê.
