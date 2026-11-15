# ADR-0007: Rede e segredos — serviços fora da internet e falha fechada

- **Status:** Aceito
- **Data:** 2026-11-15
- **Relaciona-se com:** [ADR-0006](./ADR-0006-arquitetura-microsservicos-fase4.md) (microsserviços), [RFC-0003](../rfc/RFC-0003-estrategia-autenticacao.md) (autenticação)

## Contexto

Duas revisões externas da Fase 4 (15/11/2026) apontaram problemas de exposição e de
configuração que não apareciam nos testes:

- Os 4 microsserviços eram `Service type: LoadBalancer` sem anotação. No EKS isso cria um
  LB **público**. Quem tivesse o endereço do LB passava por fora do API Gateway e do
  authorizer JWT; o Swagger, o `/v3/api-docs` e o `/api/users/auth/validate` ficavam
  alcançáveis da internet.
- A Lambda de login de staff envia e-mail e senha ao customer-service. A URL aceitava
  `http://` para um destino público, ou seja, senha em texto claro na internet.
- Os `application.yml` tinham **valor padrão** para `JWT_SECRET` e para a senha do banco, e o
  billing usava `fake` (webhook de pagamento aberto, sem assinatura) quando
  `PAYMENT_PROVIDER` faltava. Uma variável esquecida no deploy deixava o serviço subir
  aberto: com o segredo conhecido do repositório, qualquer um emitia token de ADMIN.
- O customer-service subia com o admin semeado em `admin123` quando
  `ADMIN_INITIAL_PASSWORD` faltava (só avisava no log).

## Decisão

### 1. Serviços só dentro da VPC

- Cada Service vira um **NLB interno** (`service.beta.kubernetes.io/aws-load-balancer-type: nlb`
  e `aws-load-balancer-internal: "true"`).
- O API Gateway (REST) chega a cada serviço por um **VPC Link** (um por NLB, limite do
  VPC Link de REST API). O `workbench-gateway` encontra cada NLB pela tag
  `kubernetes.io/service-name = <namespace>/<service>`, que o controlador da AWS coloca: não
  há URL de origem configurada à mão.
- A Lambda (que já roda na VPC) chama o customer-service pelo NLB interno. O Terraform da
  Lambda de staff só aceita `https://` ou o NLB interno declarado
  (`customer_service_url_internal = true`).
- Uma `NetworkPolicy` por serviço deixa entrar só a porta 8080 (vale quando o VPC CNI tem
  NetworkPolicy ligada).
- A mensageria é cifrada em repouso: tópicos SNS com chave KMS própria (a gerenciada
  `aws/sns` não aceita alarmes do CloudWatch) e filas com a criptografia do SQS. As
  mensagens levam documento e e-mail do cliente.

### 2. Segredos sem valor padrão (falha fechada)

- `JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD` e `PAYMENT_PROVIDER` não têm padrão no
  `application.yml`: sem eles o serviço **não sobe**.
- Os valores de desenvolvimento ficam no perfil **`local`** (`application-local.yml`), que
  o compose do `workbench-messaging` ativa. Os testes têm o seu `application.properties`.
- O webhook simulado de pagamento só existe com `payment.provider=fake` explícito.
- Fora do perfil local, o customer-service não sobe com o admin em `admin123` e sem
  `ADMIN_INITIAL_PASSWORD` válida.

## Consequências

- **Positivo:** o único caminho de fora para dentro é o API Gateway com authorizer;
  Swagger e endpoints internos deixam de ser públicos. Um deploy com variável faltando
  falha no start, em vez de subir inseguro.
- **Positivo:** o gateway não depende mais de 4 secrets de URL; os NLBs são descobertos.
- **Negativo:** o gateway (FASE 7 do deploy) precisa rodar **depois** dos serviços no ar.
  Trocar um LB público por interno exige recriar o Service (o pipeline faz isso uma vez,
  com alguns minutos fora do ar).
- **Negativo:** são 4 VPC Links e 4 NLBs (custo por hora de cada NLB). Um Ingress único
  (ALB interno) reduziria para um, mas exigiria o AWS Load Balancer Controller no cluster.
- **Negativo:** rodar um serviço localmente sem o compose exige ativar o perfil
  (`-Dspring-boot.run.profiles=local`).
- **Não validado na AWS** até a data desta ADR: a busca pela tag e o VPC Link passaram no
  `terraform validate`, mas não num `apply` real.

## Alternativas Rejeitadas

- **Manter LB público e confiar no JWT de cada serviço:** os serviços validam o token, mas
  a superfície exposta (Swagger, validação de credenciais, actuator) continuaria pública e
  sem o throttling do gateway.
- **Ingress com ALB interno e um VPC Link (via NLB na frente do ALB):** menos recursos, mas
  exige instalar e operar o AWS Load Balancer Controller; fica para quando houver mais
  serviços.
- **HTTP API (API Gateway v2), que aceita ALB/NLB/Cloud Map no VPC Link:** obrigaria
  reescrever o gateway e o authorizer (REST TOKEN authorizer → Lambda authorizer de HTTP API).
- **Default seguro gerado em tempo de execução (segredo aleatório por pod):** quebraria a
  validação cruzada do JWT entre serviços; falhar no start é mais claro.
