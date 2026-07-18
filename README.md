# API de Votação

Aplicação back-end em Java/Spring Boot para gerenciar pautas, sessões de votação, votos de associados, apuração de resultado e publicação do encerramento em Kafka.

## Requisitos

- Java 17.
- Maven Wrapper do projeto.
- PostgreSQL para execução local/produtiva.
- Kafka opcional, habilitado por perfil/propriedade.
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
VOTACAO_FINALIZACAO_LOTE=100

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

Para habilitar Kafka:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=kafka
```

## Testes

```bash
./mvnw test
```

Os testes usam H2 em memória com perfil `test`.

## Swagger/OpenAPI

Com a aplicação em execução, a documentação interativa fica disponível em:

```text
http://localhost:8083/swagger-ui.html
```

O contrato OpenAPI em JSON fica disponível em:

```text
http://localhost:8083/v3/api-docs
```

E em YAML:

```text
http://localhost:8083/v3/api-docs.yaml
```

## Endpoints

### Regras de estado

- Sessão nasce como `CRIADA`; pode ser atualizada ou removida e ainda não recebe votos.
- Sessão passa para `DISPONIVEL` em `POST /api/v1/sessoes/{sessaoId}/disponibilizar`.
- Após `DISPONIVEL`, a sessão não pode mais ser alterada ou removida.
- Voto é único por pauta e documento; não existe atualização ou remoção de voto.
- Sessão `DISPONIVEL` é encerrada pelo cron quando vencer ou manualmente por endpoint.

### Criar pauta

```http
POST /api/v1/pautas
```

```json
{
  "titulo": "Aprovação de nova política de crédito",
  "descricao": "Votação sobre a política proposta"
}
```

### Buscar pauta

```http
GET /api/v1/pautas/{pautaId}
```

### Listar pautas

```http
GET /api/v1/pautas
```

### Atualizar pauta

```http
PUT /api/v1/pautas/{pautaId}
```

Permitido apenas antes de existir voto e antes da sessão vinculada ser disponibilizada.

### Deletar pauta

```http
DELETE /api/v1/pautas/{pautaId}
```

Permitido apenas sem votos e sem sessão vinculada.

### Abrir sessão com duração default

```http
POST /api/v1/pautas/{pautaId}/sessoes
```

Cria a sessão como `CRIADA`. Default: `60 segundos`.

### Abrir sessão com duração customizada

```http
POST /api/v1/pautas/{pautaId}/sessoes/{duracaoEmSegundos}
```

### Listar sessões

```http
GET /api/v1/sessoes
```

### Buscar sessão

```http
GET /api/v1/sessoes/{sessaoId}
```

### Atualizar sessão

```http
PUT /api/v1/sessoes/{sessaoId}
```

Permitido apenas enquanto a sessão estiver `CRIADA`.

### Deletar sessão

```http
DELETE /api/v1/sessoes/{sessaoId}
```

Permitido apenas enquanto a sessão estiver `CRIADA`.

### Disponibilizar sessão para votação

```http
POST /api/v1/sessoes/{sessaoId}/disponibilizar
```

Depois desta transição a sessão não volta para `CRIADA` e passa a aceitar votos até `fechaEm`.

### Forçar encerramento de sessão

```http
POST /api/v1/sessoes/{sessaoId}/encerrar
```

Encerra uma sessão `DISPONIVEL` e publica o resultado no Kafka quando a mensageria estiver habilitada.

### Registrar voto

```http
POST /api/v1/pautas/{pautaId}/votos
```

```json
{
  "associadoId": 123,
  "tipoDocumento": "CPF",
  "documento": "61500421381",
  "voto": "SIM"
}
```

### Consultar resultado

```http
GET /api/v1/pautas/{pautaId}/resultado
```

### Finalizar sessões vencidas manualmente

```http
POST /api/v1/sessoes/finalizar
```

O mesmo caso de uso é executado pelo job agendado quando `VOTACAO_FINALIZACAO_HABILITADA=true`.

## Integração CPF/CNPJ

A validação externa usa OpenFeign através do client declarativo `CpfCnpjFeignClient`.

- CPF usa pacote `9`: `https://api.cpfcnpj.com.br/{token}/9/{cpf}`
- CNPJ usa pacote `6`: `https://api.cpfcnpj.com.br/{token}/6/{cnpj}`

## Regras implementadas

- Pauta persistida em banco.
- Uma sessão por pautaEntity.
- Duração de sessão via path ou default de `60 segundos`.
- Voto apenas enquanto `status=ABERTA` e `agora < fechaEm`.
- Um voto por associado por pautaEntity, protegido por validação e constraint única.
- Resultado por agregação no banco.
- Job de finalização em lote por `status` e `fechaEm`.
- Publicação Kafka opcional com payload sintetizado.
- Retentativa de publicação quando Kafka falha.
- Tratamento centralizado de erros.
