# Especificações técnicas e de negócio - API de Votação

## 1. Objetivo

Construir uma aplicação back-end para gerenciar pautas e sessões de votação em um contexto cooperativista, onde cada associado possui direito a um único voto por pautaEntity.

A solução deve expor uma API REST, persistir os dados em banco relacional e permitir:

- Cadastro de pautas.
- Abertura de sessões de votação por pautaEntity.
- Registro de votos `Sim` ou `Não`.
- Contabilização e consulta do resultado da votação.
- Integração opcional com serviço externo para validação de CPF.
- Publicação opcional do resultado em mensageria quando a sessão for encerrada.

## 2. Contexto de negócio

Em assembleias cooperativistas, decisões são tomadas por votação. Cada associado tem direito a exatamente um voto por pautaEntity, independentemente de sua participação societária.

A aplicação representa digitalmente esse fluxo:

1. Uma pautaEntity é criada.
2. Uma sessão de votação é aberta para essa pautaEntity.
3. Associados votam enquanto a sessão estiver aberta.
4. Ao fim do período, o resultado pode ser consultado e divulgado.

## 3. Regras de negócio

### 3.1 Pauta

- Uma pautaEntity representa um assunto a ser votado.
- Deve possuir identificador único.
- Deve possuir título obrigatório.
- Pode possuir descrição opcional.
- Deve ser persistida para não ser perdida após reinício da aplicação.
- Uma pautaEntity pode ter no máximo uma sessão de votação ativa ou registrada, considerando o escopo inicial da solução.

### 3.2 Sessão de votação

- Uma sessão de votação pertence obrigatoriamente a uma pautaEntity.
- A abertura da sessão pode receber uma duração customizada.
- A duração customizada deve ser informada no path como `duracaoEmSegundos`.
- Se a duração não for informada no path, a sessão deve ficar aberta por `60 segundos`.
- A sessão possui data/hora de abertura e data/hora de fechamento calculada.
- A sessão deve possuir status para diferenciar sessões abertas, encerradas e já publicadas.
- Não deve ser possível abrir sessão para pautaEntity inexistente.
- Não deve ser possível abrir mais de uma sessão para a mesma pautaEntity, salvo mudança futura explícita da regra.
- A sessão é considerada aberta quando a data/hora atual é anterior à data/hora de fechamento.
- A sessão é considerada encerrada quando a data/hora atual é igual ou posterior à data/hora de fechamento.
- O encerramento formal da sessão deve ser executado por um processo de finalização, preferencialmente um job agendado performático.

### 3.3 Voto

- Um voto pertence obrigatoriamente a uma pautaEntity.
- Cada associado é identificado por um identificador único.
- Cada associado pode votar apenas uma vez por pautaEntity.
- O voto deve aceitar apenas os valores:
  - `SIM`
  - `NAO`
- Não deve ser possível votar em pautaEntity inexistente.
- Não deve ser possível votar em pautaEntity sem sessão aberta.
- Não deve ser possível votar após o encerramento da sessão.
- Não deve ser possível alterar o voto após registrado.
- O voto deve ser persistido.

### 3.4 Resultado

- O resultado deve informar:
  - Identificador da pautaEntity.
  - Total de votos `SIM`.
  - Total de votos `NAO`.
  - Total geral de votos.
  - Situação da sessão.
  - Opção vencedora, quando aplicável.
- Em caso de empate, o resultado deve indicar `EMPATE`.
- O resultado pode ser consultado a qualquer momento.
- Antes do encerramento, o resultado deve ser retornado como parcial ou com status indicando sessão ainda aberta.

### 3.5 Validação de CPF do associado

Regra aplicável à tarefa bônus de integração externa.

- Antes de registrar o voto, a aplicação pode consultar o serviço externo:
  - CPF: `GET https://api.cpfcnpj.com.br/{token}/9/{cpf}`
  - CNPJ: `GET https://api.cpfcnpj.com.br/{token}/6/{cnpj}`
- O token fixo de teste é `5ae973d7a997af13f0aaf2bf60e65803`.
- O código de consulta para CPF é `9`.
- O código de consulta para CNPJ é `6`.
- A aplicação deve escolher automaticamente o código da consulta a partir do campo `tipoDocumento` informado no registro do voto.
- Se o serviço retornar `status = 1`, o CPF é considerado válido e o voto pode prosseguir.
- Se o serviço retornar `status = 0`, o CPF deve ser tratado como inválido e o voto deve ser negado.
- Falhas de comunicação com o serviço externo devem ser tratadas de forma explícita.
- Para evitar forte acoplamento com disponibilidade externa, a integração deve ser isolada em um client próprio.

### 3.6 Publicação do resultado

Regra aplicável à tarefa bônus de mensageria.

- Quando a sessão for encerrada, o resultado final deve ser publicado para o restante da plataforma.
- A publicação pode ser feita via Kafka.
- A mensagem deve conter dados suficientes para consumidores externos entenderem o resultado sem consultar novamente a API.
- A publicação deve ser idempotente para evitar múltiplas mensagens do mesmo encerramento.
- A publicação deve ocorrer apenas depois que a sessão for marcada como encerrada.
- O evento publicado deve sintetizar os dados da pautaEntity e o resultado final da votação.

## 4. Requisitos funcionais

### RF01 - Cadastrar pautaEntity

Como usuário da API, quero cadastrar uma pautaEntity para que ela possa receber uma sessão de votação.

Critérios:

- Deve receber título.
- Deve aceitar descrição.
- Deve retornar HTTP `201 Created`.
- Deve retornar os dados da pautaEntity criada.

### RF02 - Buscar pautaEntity

Como usuário da API, quero consultar uma pautaEntity existente.

Critérios:

- Deve retornar HTTP `200 OK` para pautaEntity existente.
- Deve retornar HTTP `404 Not Found` para pautaEntity inexistente.

### RF03 - Abrir sessão de votação

Como usuário da API, quero abrir uma sessão de votação para uma pautaEntity.

Critérios:

- Deve aceitar duração customizada em segundos pelo path `duracaoEmSegundos`.
- Deve aplicar duração padrão de `60 segundos` quando `duracaoEmSegundos` não for informado.
- Deve retornar HTTP `201 Created`.
- Deve retornar data/hora de abertura e fechamento.
- Deve retornar HTTP `404 Not Found` para pautaEntity inexistente.
- Deve retornar HTTP `409 Conflict` quando já existir sessão para a pautaEntity.

### RF04 - Registrar voto

Como associado, quero votar `SIM` ou `NAO` em uma pautaEntity com sessão aberta.

Critérios:

- Deve aceitar identificador único do associado.
- Deve aceitar CPF quando a validação externa estiver habilitada.
- Deve aceitar apenas votos válidos.
- Deve retornar HTTP `201 Created`.
- Deve impedir voto duplicado por pautaEntity.
- Deve retornar HTTP `409 Conflict` para voto duplicado.
- Deve retornar HTTP `422 Unprocessable Entity` quando a sessão estiver encerrada ou o associado não puder votar.

### RF05 - Consultar resultado

Como usuário da API, quero consultar a contabilização dos votos de uma pautaEntity.

Critérios:

- Deve retornar totais de `SIM`, `NAO` e geral.
- Deve indicar vencedor ou empate.
- Deve retornar HTTP `404 Not Found` para pautaEntity inexistente.

### RF06 - Finalizar sessões encerradas

Como plataforma, quero finalizar sessões cujo horário de fechamento já passou para consolidar o resultado e publicar o encerramento.

Critérios:

- Deve buscar apenas sessões com `fechaEm` menor ou igual à data/hora atual.
- Deve ignorar sessões já encerradas/publicadas.
- Deve processar em lotes para evitar carga excessiva no banco.
- Deve contabilizar os votos usando agregação no banco.
- Deve marcar a sessão como encerrada antes da publicação.
- Deve publicar o resultado final em tópico Kafka.
- Deve ser idempotente para evitar republicação do mesmo resultado.
- Caso o job agendado não seja usado, deve existir endpoint administrativo para disparar a finalização.

## 5. Requisitos não funcionais

### 5.1 Persistência

- Utilizar banco relacional.
- Utilizar PostgreSQL como banco principal.
- Garantir integridade por constraints de banco, não apenas por validação em memória.
- Dados de pautas, sessões e votos não podem ser perdidos em restart da aplicação.

### 5.2 Performance

- A aplicação deve suportar cenários com centenas de milhares de votos.
- A contagem de votos deve ser feita por consultas agregadas no banco.
- Deve existir índice para busca por pautaEntity e tipo de voto.
- Deve existir índice para busca de sessões pendentes de encerramento por status e data de fechamento.
- Deve existir constraint única para impedir voto duplicado por pautaEntity e associado.
- Operações críticas devem ser transacionais.
- O fechamento de sessões deve processar registros em lotes paginados, evitando carregar todas as sessões encerráveis em memória.
- O job de fechamento deve ter limite configurável de itens por execução.

### 5.3 Concorrência

- Dois votos simultâneos do mesmo associado para a mesma pautaEntity não podem gerar duplicidade.
- A regra de voto único deve ser reforçada por índice único.
- Conflitos de integridade devem ser traduzidos para resposta HTTP adequada.

### 5.4 Observabilidade

- Registrar logs de:
  - Criação de pautaEntity.
  - Abertura de sessão.
  - Registro de voto.
  - Tentativas inválidas de voto.
  - Publicação de resultado.
  - Falhas em integrações externas.
- Não registrar dados sensíveis completos em logs, como CPF completo.

### 5.5 Segurança

- Para fins do exercício, autenticação e autorização estão fora do escopo.
- A API deve validar payloads de entrada.
- A API não deve expor stack trace em respostas de erro.

### 5.6 Qualidade

- Código organizado por responsabilidade.
- Testes automatizados para regras principais.
- Tratamento centralizado de exceções.
- Documentação de execução e da API.

## 6. Stack técnica proposta

Considerando a estrutura atual do projeto:

- Linguagem: Java 17.
- Framework: Spring Boot.
- API REST: Spring Web MVC.
- Persistência: Spring Data JPA.
- Banco de dados: PostgreSQL.
- Build: Maven.
- Testes: JUnit e Spring Boot Test.
- Mensageria bônus: Kafka.
- Documentação da API: OpenAPI/Swagger, se adicionado ao projeto.

## 7. Arquitetura proposta

A solução deve seguir uma arquitetura simples em camadas:

```text
Controller REST
    -> Service
        -> Repository
            -> Database
```

Camadas sugeridas:

- `controller`: endpoints REST e contratos HTTP.
- `domain.model`: entidades de domínio persistidas.
- `domain.service`: regras de negócio e transações.
- `domain.repository`: acesso ao banco.
- `domain.client`: integração externa de CPF.
- `mapper`: conversão entre entidades e DTOs, se necessário.
- `config`: configurações de infraestrutura.
- `suporte`: exceções, handlers e utilitários compartilhados.

## 8. Modelo de domínio

### 8.1 Pauta

Campos sugeridos:

- `id`: identificador único.
- `titulo`: título da pautaEntity.
- `descricao`: descrição opcional.
- `criadaEm`: data/hora de criação.

Relacionamentos:

- Pode possuir uma sessão de votação.
- Pode possuir muitos votos.

### 8.2 SessaoVotacao

Campos sugeridos:

- `id`: identificador único.
- `pautaEntity`: pautaEntity vinculada.
- `abertaEm`: data/hora de abertura.
- `fechaEm`: data/hora de fechamento.
- `encerradaEm`: data/hora em que a sessão foi formalmente encerrada.
- `status`: status atual da sessão.
- `resultadoPublicado`: indicador de publicação do resultado.

Relacionamentos:

- Pertence a uma pautaEntity.

### 8.3 Voto

Campos sugeridos:

- `id`: identificador único.
- `pautaEntity`: pautaEntity votada.
- `associadoId`: identificador único do associado.
- `tipoDocumento`: tipo do documento informado pelo associado, `CPF` ou `CNPJ`.
- `documento`: CPF ou CNPJ do associado, somente dígitos ou formatado.
- `tipo`: `SIM` ou `NAO`.
- `criadoEm`: data/hora do voto.

Constraints:

- Unique key em `pauta_id` + `associado_id`.

### 8.4 TipoVoto

Valores:

- `SIM`
- `NAO`

### 8.5 StatusResultado

Valores sugeridos:

- `SESSAO_ABERTA`
- `SESSAO_ENCERRADA`
- `SEM_SESSAO`

### 8.6 StatusSessaoVotacao

Valores sugeridos:

- `ABERTA`
- `ENCERRADA`
- `PUBLICADA`

### 8.7 ResultadoVotacao

Objeto de resposta calculado:

- `pautaId`
- `votosSim`
- `votosNao`
- `totalVotos`
- `status`
- `vencedor`

## 9. Contratos de API

Estratégia de versionamento recomendada: versionamento por path.

Base path:

```text
/api/v1
```

Essa estratégia é simples para consumidores, explícita nos endpoints e adequada para o escopo do teste.

### 9.1 Criar pautaEntity

```http
POST /api/v1/pautas
Content-Type: application/json
```

Request:

```json
{
  "titulo": "Aprovação de nova política de crédito",
  "descricao": "Votação sobre a política proposta para o próximo ciclo."
}
```

Response `201 Created`:

```json
{
  "id": 1,
  "titulo": "Aprovação de nova política de crédito",
  "descricao": "Votação sobre a política proposta para o próximo ciclo."
}
```

### 9.2 Buscar pautaEntity

```http
GET /api/v1/pautas/{pautaId}
```

Response `200 OK`:

```json
{
  "id": 1,
  "titulo": "Aprovação de nova política de crédito",
  "descricao": "Votação sobre a política proposta para o próximo ciclo."
}
```

### 9.3 Abrir sessão

```http
POST /api/v1/pautas/{pautaId}/sessoes
Content-Type: application/json
```

Abre a sessão com duração padrão de `60 segundos`.

```http
POST /api/v1/pautas/{pautaId}/sessoes/{duracaoEmSegundos}
Content-Type: application/json
```

Abre a sessão com duração customizada em segundos.

Exemplo com duração customizada:

```http
POST /api/v1/pautas/1/sessoes/120
Content-Type: application/json
```

Não há body obrigatório para abertura da sessão.

Request usando default:

```json
{}
```

Response `201 Created` usando duração customizada de `120 segundos`:

```json
{
  "id": 10,
  "pautaId": 1,
  "abertaEm": "2026-07-16T19:30:00",
  "fechaEm": "2026-07-16T19:32:00",
  "status": "ABERTA"
}
```

Response `201 Created` usando duração default de `60 segundos`:

```json
{
  "id": 10,
  "pautaId": 1,
  "abertaEm": "2026-07-16T19:30:00",
  "fechaEm": "2026-07-16T19:31:00",
  "status": "ABERTA"
}
```

### 9.4 Registrar voto

```http
POST /api/v1/pautas/{pautaId}/votos
Content-Type: application/json
```

Request:

```json
{
  "associadoId": 123,
  "tipoDocumento": "CPF",
  "documento": "61500421381",
  "voto": "SIM"
}
```

Response `201 Created`:

```json
{
  "pautaId": 1,
  "associadoId": 123,
  "voto": "SIM",
  "criadoEm": "2026-07-16T19:31:00"
}
```

### 9.5 Consultar resultado

```http
GET /api/v1/pautas/{pautaId}/resultado
```

Response `200 OK`:

```json
{
  "pautaId": 1,
  "votosSim": 150,
  "votosNao": 90,
  "totalVotos": 240,
  "status": "SESSAO_ENCERRADA",
  "vencedor": "SIM"
}
```

### 9.6 Finalizar sessões encerradas

Endpoint opcional caso a estratégia escolhida não seja exclusivamente por job agendado.

```http
POST /api/v1/sessoes/finalizar
```

Comportamento:

- Busca sessões abertas com `fechaEm` menor ou igual à data/hora atual.
- Encerra as sessões encontradas.
- Calcula o resultado final.
- Publica uma mensagem por pautaEntity encerrada no tópico Kafka.
- Retorna a quantidade de sessões processadas.

Response `200 OK`:

```json
{
  "sessoesProcessadas": 25
}
```

## 10. Códigos de erro

Formato recomendado:

```json
{
  "timestamp": "2026-07-16T19:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Pauta não encontrada para o id 1",
  "path": "/api/v1/pautas/1"
}
```

Mapeamento:

- `400 Bad Request`: payload inválido, enum inválido ou campo obrigatório ausente.
- `404 Not Found`: pautaEntity ou sessão não encontrada.
- `409 Conflict`: sessão já existente ou voto duplicado.
- `422 Unprocessable Entity`: regra de negócio violada, como sessão encerrada ou associado não apto.
- `500 Internal Server Error`: erro inesperado.
- `503 Service Unavailable`: indisponibilidade de dependência externa crítica, se a decisão for bloquear o voto.

## 11. Banco de dados

### 11.1 Tabela `pautas`

Campos:

- `id`
- `titulo`
- `descricao`
- `criada_em`

### 11.2 Tabela `sessoes_votacao`

Campos:

- `id`
- `pauta_id`
- `aberta_em`
- `fecha_em`
- `encerrada_em`
- `status`
- `resultado_publicado`

Constraints:

- Foreign key `pauta_id`.
- Unique key em `pauta_id`.
- Índice em `status`, `fecha_em`.

### 11.3 Tabela `votos`

Campos:

- `id`
- `pauta_id`
- `associado_id`
- `tipo_documento`
- `documento`
- `tipo`
- `criado_em`

Constraints e índices:

- Foreign key `pauta_id`.
- Unique key em `pauta_id`, `associado_id`.
- Índice em `pauta_id`, `tipo`.

## 12. Transações e consistência

- Criação de pautaEntity deve ser transacional.
- Abertura de sessão deve ser transacional.
- Registro de voto deve ser transacional.
- Consulta de resultado pode ser transação somente leitura.
- Voto duplicado deve ser prevenido por validação de serviço e constraint no banco.
- A constraint de banco é a fonte final de proteção contra concorrência.

## 13. Integração externa de CPF

Componente sugerido:

```text
AssociadoElegibilidadeClient
```

Responsabilidades:

- Usar OpenFeign para executar a chamada HTTP declarativa ao serviço externo.
- Montar a chamada `GET https://api.cpfcnpj.com.br/{token}/{codigo}/{documento}`.
- Traduzir `status = 1` para CPF válido.
- Traduzir `status = 0` para CPF inválido, usando o campo `erro` como mensagem de negócio.
- Tratar erros HTTP como falha de dependência externa.
- Aplicar timeout.
- Registrar logs de falha sem expor CPF completo.

Decisão recomendada:

- Em caso de indisponibilidade do serviço externo, retornar erro controlado e não registrar o voto, pois a regra de elegibilidade não pôde ser confirmada.

## 14. Mensageria

Componente sugerido:

```text
ResultadoVotacaoPublisher
```

Evento sugerido:

```json
{
  "id": 1,
  "titulo": "Aprovação de nova política de crédito",
  "descricao": "Votação sobre a política proposta para o próximo ciclo.",
  "pautaEntity": {
    "pautaId": 1,
    "votosSim": 150,
    "votosNao": 90,
    "totalVotos": 240,
    "status": "SESSAO_ENCERRADA",
    "vencedor": "SIM"
  }
}
```

Estratégia de fechamento:

- Criar um job agendado que verifica sessões abertas com `fechaEm <= agora`.
- Buscar até `10_000` sessões por consulta do batch e processar com `pool-size` configurável, por exemplo `4` ou `8` workers por execução.
- Usar consulta paginada ordenada por `fechaEm`, filtrando por `status = ABERTA`.
- Para cada sessão elegível, calcular o resultado por agregação no banco.
- Marcar a sessão como `ENCERRADA`.
- Publicar o evento sintetizado no Kafka.
- Marcar a sessão como `PUBLICADA` ou `resultadoPublicado = true` após sucesso no envio.
- Em caso de falha no Kafka, manter a sessão encerrada e não publicada para retentativa posterior.
- Como alternativa operacional, disponibilizar endpoint de finalização para disparo manual ou por scheduler externo.

Para simplicidade e previsibilidade no teste técnico, a opção recomendada é implementar um job com `@Scheduled`, `pool-size` configurável, limite de consulta de `10_000` registros e consulta indexada. Caso o uso de cron interno não seja desejado, o endpoint `POST /api/v1/sessoes/finalizar` pode ser usado por um scheduler externo.

## 15. Testes automatizados

### 15.1 Testes unitários

Cobrir:

- Criação de pautaEntity.
- Abertura de sessão com duração default.
- Abertura de sessão com duração customizada.
- Impedimento de sessão duplicada.
- Registro de voto válido.
- Impedimento de voto duplicado.
- Impedimento de voto com sessão encerrada.
- Cálculo de resultado com vitória `SIM`.
- Cálculo de resultado com vitória `NAO`.
- Cálculo de empate.
- Finalização de sessão vencida.
- Não republicação de sessão já publicada.

### 15.2 Testes de integração

Cobrir:

- Fluxo completo via API:
  1. Criar pautaEntity.
  2. Abrir sessão.
  3. Registrar votos.
  4. Consultar resultado.
- Persistência em banco.
- Validações HTTP.
- Tratamento de erros.
- Finalização de sessão e publicação do evento sintetizado.

### 15.3 Testes de performance

Para a tarefa bônus de performance:

- Criar massa com centenas de milhares de votos.
- Medir tempo de registro de voto.
- Medir tempo de consulta de resultado.
- Validar uso de índices.

Ferramentas possíveis:

- JMeter.
- Gatling.
- k6.

## 16. Estratégia de versionamento da API

Estratégia escolhida:

```text
/api/v1
```

Justificativa:

- É simples de entender.
- Facilita coexistência futura entre versões.
- Não exige negociação por header.
- É compatível com documentação OpenAPI.

Critérios para criar uma nova versão:

- Remoção ou renomeação de campos de request/response.
- Mudança incompatível de regra de negócio.
- Mudança de semântica de status HTTP.

Mudanças compatíveis podem permanecer na mesma versão:

- Adição de campos opcionais.
- Novos endpoints.
- Novos filtros opcionais.

## 17. Decisões técnicas

- Usar Spring Boot pela produtividade e ecossistema consolidado para APIs REST.
- Usar PostgreSQL para persistência confiável e suporte a constraints/índices.
- Usar JPA para simplificar o acesso a dados sem perder capacidade de consultas agregadas.
- Usar transações nos casos de escrita para manter consistência.
- Usar constraint única para voto único por pautaEntity, pois validações apenas em aplicação falham sob concorrência.
- Usar contagem agregada no banco para resultado, evitando carregar votos em memória.
- Isolar integração externa em client próprio para facilitar testes e substituição.
- Isolar mensageria em publisher próprio para manter a regra de negócio desacoplada da infraestrutura.

## 18. Fora de escopo inicial

- Autenticação e autorização.
- Interface web.
- Cadastro completo de associados.
- Administração de múltiplas sessões por pautaEntity.
- Alteração ou exclusão de votos.
- Auditoria avançada.
- Escalabilidade horizontal com locks distribuídos.

## 19. Instruções esperadas de execução

A documentação final do projeto deve conter:

- Como subir o banco PostgreSQL.
- Como configurar variáveis de ambiente.
- Como executar a aplicação.
- Como executar os testes.
- Como acessar a documentação da API.
- Como habilitar integrações opcionais, como Kafka e validação externa.

Variáveis esperadas:

```text
SERVER_PORT
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
CPF_CNPJ_BASE_URL
CPF_CNPJ_TOKEN
CPF_CNPJ_CODIGO_CPF
CPF_CNPJ_CODIGO_CNPJ
KAFKA_BOOTSTRAP_SERVERS
```

## 20. Critérios de aceite gerais

- A aplicação sobe localmente sem configuração obscura.
- Pautas e votos persistem após restart.
- Um associado não consegue votar duas vezes na mesma pautaEntity.
- Votos após encerramento são negados.
- Resultado é contabilizado corretamente.
- Erros são claros e padronizados.
- Testes automatizados validam as regras principais.
- Documentação explica escolhas técnicas e como executar a solução.
