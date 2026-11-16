# ADR-0009: JWT assinado com chave assimétrica (RS256)

- **Status:** Aceito
- **Data:** 2026-11-16
- **Substitui parcialmente:** [RFC-0003](../rfc/RFC-0003-estrategia-autenticacao.md) (HS256 com segredo compartilhado)
- **Relaciona-se com:** [ADR-0007](./ADR-0007-rede-e-segredos.md) (segredos sem valor padrão)

## Contexto

Os tokens eram HS256: o mesmo `JWT_SECRET` assinava e verificava. Para verificar, os 4
microsserviços, a Lambda de login e o authorizer precisavam do segredo — e quem tem o segredo
também consegue **emitir** token. Bastava um desses 6 componentes comprometido (ou o segredo num
log, num pod, num secret da org lido por outro repositório) para emitir token de ADMIN aceito
pela plataforma inteira. O os-service, além disso, emitia o próprio token de serviço com o mesmo
segredo para chamar o customer-service.

## Decisão

1. **RS256 (RSA 2048).** A Lambda de login assina com a chave **privada** (`JWT_PRIVATE_KEY`);
   o authorizer e os 4 serviços têm só a **pública** (`JWT_PUBLIC_KEY`): verificam, não emitem.
2. **`kid` no cabeçalho.** `workbench-auth` para os tokens da Lambda. O verificador escolhe a
   chave pelo `kid`; desconhecido, ausente ou algoritmo diferente (ex.: HS256) é recusado.
3. **Token de serviço com outro par.** O os-service assina o token das chamadas ao customer
   com a sua chave privada (`kid=os-service`, `JWT_SERVICE_PRIVATE_KEY`); **só o customer**
   tem a pública correspondente. O token de serviço não vale no billing, no execution nem no
   gateway.
4. **Chaves na configuração** como Base64 do DER numa linha; sem valor padrão (ADR-0007). O par
   de desenvolvimento fica no perfil `local` e em `workbench-messaging/dev-keys/`.

## Consequências

- **Positivo:** só a Lambda (e o os-service, para o seu escopo) consegue emitir token.
  Comprometer um serviço verificador não permite forjar token.
- **Positivo:** os testes cobrem a recusa de HS256 com o segredo antigo, de `kid` desconhecido
  e do token de serviço onde ele não é confiável.
- **Negativo:** 4 secrets de chave em vez de 1, e trocar as chaves exige redeploy da Lambda e
  dos serviços (procedimento no Runbook).
- **Negativo:** as chaves privadas ficam em secrets da organização; é preciso restringir cada
  uma ao repositório que a usa.
- **Em aberto:** publicar as chaves por JWKS (endpoint) permitiria trocar a chave sem redeploy
  dos verificadores; não compensa no escopo atual.

## Alternativas Rejeitadas

- **Manter HS256 e só proteger melhor o segredo:** não resolve o problema de fundo — todo
  verificador continua podendo emitir.
- **JWKS desde já:** exige publicar e cachear as chaves (endpoint ou S3) e rotação com dois
  `kid` ativos; mais peças para o mesmo ganho imediato.
- **Token de serviço com a chave da Lambda:** o os-service precisaria da chave privada da
  plataforma e voltaria a emitir token aceito por todos.
