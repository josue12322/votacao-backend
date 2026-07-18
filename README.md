# API de Votação

Aplicação back-end em Java/Spring Boot para gerenciar pautas, sessões de votação, votos de associados, apuração de resultado e publicação do encerramento em Kafka.

## Requisitos

- Java 17.
- Maven Wrapper do projeto.
- PostgreSQL para execução local/produtiva.
- Kafka opcional.
- OpenFeign para integração HTTP com a API externa de CPF/CNPJ.

## Configuração

Variáveis principais:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-1-us-west-2.pooler.supabase.com:5432/postgres
SPRING_DATASOURCE_USERNAME=postgres.nykqbeoolgjobxuyvbcn
SPRING_DATASOURCE_PASSWORD=Votac@o2026

CPF_CNPJ_BASE_URL=https://api.cpfcnpj.com.br
CPF_CNPJ_TOKEN=5ae973d7a997af13f0aaf2bf60e65803
CPF_CNPJ_CODIGO_CPF=9
CPF_CNPJ_CODIGO_CNPJ=6

VOTACAO_FINALIZACAO_HABILITADA=true
VOTACAO_FINALIZACAO_CRON=*/10 * * * * *
VOTACAO_FINALIZACAO_POOL_SIZE=4

VOTACAO_KAFKA_HABILITADO=false
VOTACAO_KAFKA_TOPICO_RESULTADO=votacao.resultado.encerrado
KAFKA_BOOTSTRAP_SERVERS=localhost:19092
```

## Execução

```bash
./mvnw spring-boot:run
```

No Windows:

```bat
mvnw.cmd spring-boot:run
```

## Docker

Subir PostgreSQL local:

```bash
docker compose up -d postgres
```

Subir Kafka e Kafka UI:

```bash
docker compose --profile kafka up -d kafka kafka-ui
```

Acessos locais:

- Aplicação: `http://localhost:8083`
- Swagger UI: `http://localhost:8083/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8083/v3/api-docs`
- Kafka UI: `http://localhost:8084`
- Kafka broker para aplicação fora do Docker: `localhost:19092`

## Testes

```bash
./mvnw test
```

Os testes usam H2 em memória com perfil `test`.

## Regras de Estado

- A pauta pode ser criada, consultada, listada, atualizada e removida.
- A pauta só pode ser atualizada se ainda não possuir voto e se a sessão vinculada ainda não tiver sido disponibilizada.
- A pauta só pode ser removida se não possuir votos nem sessão vinculada.
- A sessão nasce como `CRIADA`; nesse estado não recebe votos e pode ser atualizada ou removida.
- A sessão passa para `DISPONIVEL` em `POST /api/v1/sessoes/{sessaoId}/disponibilizar`.
- Depois de `DISPONIVEL`, a sessão não volta para `CRIADA` e não pode mais ser alterada/removida.
- Voto é único por pauta e documento; não existe atualização, remoção ou novo voto para o mesmo documento na mesma pauta.
- Sessão `DISPONIVEL` vencida é encerrada pelo cron job ou manualmente pelo endpoint de finalização.
- Sessão encerrada publica o resultado sintetizado no Kafka quando `VOTACAO_KAFKA_HABILITADO=true`.

## Formato de Erro

Exemplo padrão retornado pelo tratamento centralizado de exceções:

```json
{
  "timestamp": "2026-07-18T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Pauta não encontrada para o id 1",
  "path": "/api/v1/pautas/1"
}
```

## Endpoints

### 1. Criar Pauta

```http
POST /api/v1/pautas
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
  "descricao": "Votação sobre a política proposta para o próximo ciclo.",
  "criadaEm": "2026-07-18T10:00:00"
}
```

Possíveis erros:

- `400 Bad Request`: payload inválido.

### 2. Listar Pautas

```http
GET /api/v1/pautas
```

Response `200 OK`:

```json
[
  {
    "id": 1,
    "titulo": "Aprovação de nova política de crédito",
    "descricao": "Votação sobre a política proposta para o próximo ciclo.",
    "criadaEm": "2026-07-18T10:00:00"
  }
]
```

### 3. Buscar Pauta por ID

```http
GET /api/v1/pautas/{pautaId}
```

Response `200 OK`:

```json
{
  "id": 1,
  "titulo": "Aprovação de nova política de crédito",
  "descricao": "Votação sobre a política proposta para o próximo ciclo.",
  "criadaEm": "2026-07-18T10:00:00"
}
```

Possíveis erros:

- `404 Not Found`: pauta não encontrada.

### 4. Atualizar Pauta

```http
PUT /api/v1/pautas/{pautaId}
```

Request:

```json
{
  "titulo": "Aprovação revisada de política de crédito",
  "descricao": "Descrição ajustada antes da votação."
}
```

Response `200 OK`:

```json
{
  "id": 1,
  "titulo": "Aprovação revisada de política de crédito",
  "descricao": "Descrição ajustada antes da votação.",
  "criadaEm": "2026-07-18T10:00:00"
}
```

Possíveis erros:

- `400 Bad Request`: payload inválido.
- `404 Not Found`: pauta não encontrada.
- `409 Conflict`: pauta já possui voto ou sessão disponibilizada.

### 5. Deletar Pauta

```http
DELETE /api/v1/pautas/{pautaId}
```

Response `204 No Content`.

Possíveis erros:

- `404 Not Found`: pauta não encontrada.
- `409 Conflict`: pauta possui votos ou sessão vinculada.

### 6. Criar Sessão com Duração Default

```http
POST /api/v1/pautas/{pautaId}/sessoes
```

Request opcional:

```json
{
  "duracaoEmSegundos": 120
}
```

Se o body não for enviado, a duração default será `60` segundos.

Response `201 Created`:

```json
{
  "id": 10,
  "pautaId": 1,
  "abertaEm": "2026-07-18T10:00:00",
  "fechaEm": "2026-07-18T10:02:00",
  "encerradaEm": null,
  "status": "CRIADA"
}
```

Possíveis erros:

- `400 Bad Request`: payload inválido.
- `404 Not Found`: pauta não encontrada.
- `409 Conflict`: pauta já possui sessão.

### 7. Criar Sessão com Duração no Path

```http
POST /api/v1/pautas/{pautaId}/sessoes/{duracaoEmSegundos}
```

Exemplo:

```http
POST /api/v1/pautas/1/sessoes/120
```

Response `201 Created`:

```json
{
  "id": 10,
  "pautaId": 1,
  "abertaEm": "2026-07-18T10:00:00",
  "fechaEm": "2026-07-18T10:02:00",
  "encerradaEm": null,
  "status": "CRIADA"
}
```

Possíveis erros:

- `400 Bad Request`: duração inválida.
- `404 Not Found`: pauta não encontrada.
- `409 Conflict`: pauta já possui sessão.

### 8. Listar Sessões

```http
GET /api/v1/sessoes
```

Response `200 OK`:

```json
[
  {
    "id": 10,
    "pautaId": 1,
    "abertaEm": "2026-07-18T10:00:00",
    "fechaEm": "2026-07-18T10:02:00",
    "encerradaEm": null,
    "status": "CRIADA"
  }
]
```

### 9. Buscar Sessão por ID

```http
GET /api/v1/sessoes/{sessaoId}
```

Response `200 OK`:

```json
{
  "id": 10,
  "pautaId": 1,
  "abertaEm": "2026-07-18T10:00:00",
  "fechaEm": "2026-07-18T10:02:00",
  "encerradaEm": null,
  "status": "CRIADA"
}
```

Possíveis erros:

- `404 Not Found`: sessão não encontrada.

### 10. Atualizar Sessão

```http
PUT /api/v1/sessoes/{sessaoId}
```

Request:

```json
{
  "duracaoEmSegundos": 180
}
```

Response `200 OK`:

```json
{
  "id": 10,
  "pautaId": 1,
  "abertaEm": "2026-07-18T10:00:00",
  "fechaEm": "2026-07-18T10:03:00",
  "encerradaEm": null,
  "status": "CRIADA"
}
```

Possíveis erros:

- `400 Bad Request`: payload inválido.
- `404 Not Found`: sessão não encontrada.
- `409 Conflict`: sessão não pode mais ser alterada.

### 11. Deletar Sessão

```http
DELETE /api/v1/sessoes/{sessaoId}
```

Response `204 No Content`.

Possíveis erros:

- `404 Not Found`: sessão não encontrada.
- `409 Conflict`: sessão não pode mais ser removida.

### 12. Disponibilizar Sessão para Votação

```http
POST /api/v1/sessoes/{sessaoId}/disponibilizar
```

Response `200 OK`:

```json
{
  "id": 10,
  "pautaId": 1,
  "abertaEm": "2026-07-18T10:00:00",
  "fechaEm": "2026-07-18T10:02:00",
  "encerradaEm": null,
  "status": "DISPONIVEL"
}
```

Possíveis erros:

- `404 Not Found`: sessão não encontrada.
- `409 Conflict`: sessão não está em estado `CRIADA`.

### 13. Forçar Encerramento de Sessão

```http
POST /api/v1/sessoes/{sessaoId}/encerrar
```

Response `200 OK`:

```json
{
  "id": 10,
  "pautaId": 1,
  "abertaEm": "2026-07-18T10:00:00",
  "fechaEm": "2026-07-18T10:02:00",
  "encerradaEm": "2026-07-18T10:01:30",
  "status": "PUBLICADA"
}
```

Possíveis erros:

- `404 Not Found`: sessão não encontrada.
- `422 Unprocessable Entity`: sessão `CRIADA` ainda não pode ser encerrada como votação.

### 14. Registrar Voto

```http
POST /api/v1/pautas/{pautaId}/votos
```

Request:

```json
{
  "tipoDocumento": "CPF",
  "documento": "61500421381",
  "voto": "SIM"
}
```

Request com CNPJ:

```json
{
  "tipoDocumento": "CNPJ",
  "documento": "11222333000181",
  "voto": "NAO"
}
```

Response `201 Created`:

```json
{
  "id": 100,
  "pautaId": 1,
  "tipoDocumento": "CPF",
  "documento": "61500421381",
  "voto": "SIM",
  "criadoEm": "2026-07-18T10:01:00"
}
```

Possíveis erros:

- `400 Bad Request`: payload inválido.
- `404 Not Found`: pauta ou sessão não encontrada.
- `409 Conflict`: documento já votou nesta pauta.
- `422 Unprocessable Entity`: sessão não disponível, encerrada, CPF/CNPJ inválido ou documento inapto.
- `503 Service Unavailable`: serviço externo de elegibilidade indisponível.

### 15. Consultar Resultado

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

Possíveis valores de `status`:

- `SEM_SESSAO`
- `SESSAO_ABERTA`
- `SESSAO_ENCERRADA`

Possíveis valores de `vencedor`:

- `SIM`
- `NAO`
- `EMPATE`

Possíveis erros:

- `404 Not Found`: pauta ou sessão não encontrada.

### 16. Finalizar Sessões Vencidas Manualmente

```http
POST /api/v1/sessoes/finalizar
```

Executa manualmente o mesmo caso de uso do cron job:

- Busca até `10_000` sessões `DISPONIVEL` vencidas.
- Busca até `10_000` sessões `ENCERRADA` ainda não publicadas.
- Processa com `VOTACAO_FINALIZACAO_POOL_SIZE`.
- Publica o resultado no Kafka quando a mensageria estiver habilitada.

Response `200 OK`:

```json
{
  "sessoesProcessadas": 2
}
```

## Evento Publicado no Kafka

Tópico default:

```text
votacao.resultado.encerrado
```

Payload:

```json
{
  "id": 1,
  "titulo": "Aprovação de nova política de crédito",
  "descricao": "Votação sobre a política proposta para o próximo ciclo.",
  "pauta": {
    "pautaId": 1,
    "votosSim": 150,
    "votosNao": 90,
    "totalVotos": 240,
    "status": "SESSAO_ENCERRADA",
    "vencedor": "SIM"
  }
}
```

## Integração CPF/CNPJ

A validação externa usa OpenFeign através do client declarativo `CpfCnpjFeignClient`.

- CPF usa pacote `9`: `https://api.cpfcnpj.com.br/{token}/9/{cpf}`
- CNPJ usa pacote `6`: `https://api.cpfcnpj.com.br/{token}/6/{cnpj}`

Exemplo de sucesso:

```json
{
  "status": 1,
  "cpf": "615.004.213-83",
  "nome": "Test Token",
  "pacoteUsado": 1,
  "saldo": 123,
  "consultaID": "11bb22cc33dd44ee",
  "delay": 0.3
}
```

Exemplo de erro:

```json
{
  "status": 0,
  "cpf": "",
  "nome": null,
  "erro": "CPF inválido!",
  "pacoteUsado": 1,
  "erroCodigo": 100
}
```

## Regras Implementadas

- Pautas, sessões e votos persistidos em PostgreSQL.
- Uma sessão por pauta.
- Duração de sessão via body, path ou default de `60` segundos.
- Voto apenas enquanto a sessão está `DISPONIVEL` e `agora < fechaEm`.
- Um voto por documento por pauta, protegido por validação e constraint única.
- Resultado calculado por agregação no banco.
- Job de finalização com consulta limitada a `10_000` registros e `pool-size` configurável.
- Publicação Kafka opcional com payload sintetizado.
- Retentativa de publicação de sessões `ENCERRADA` quando Kafka falha.
- Tratamento centralizado de erros.
