# Swagger/OpenAPI

O Swagger da aplicaÃ§Ã£o fica habilitado explicitamente por configuraÃ§Ã£o.

## URLs

Com a aplicaÃ§Ã£o rodando na porta `8083`:

```text
http://localhost:8083/swagger-ui.html
```

Contrato OpenAPI em JSON:

```text
http://localhost:8083/v3/api-docs
```

Contrato OpenAPI em YAML:

```text
http://localhost:8083/v3/api-docs.yaml
```

## Aviso no log

O log abaixo nÃ£o Ã© erro:

```text
SpringDoc /swagger-ui.html endpoint is enabled by default.
```

Ele apenas informa que o endpoint do Swagger estÃ¡ disponÃ­vel. Como esta aplicaÃ§Ã£o precisa disponibilizar a documentaÃ§Ã£o da API, a configuraÃ§Ã£o foi deixada explicitamente habilitada em `application.yaml`.

## ConfiguraÃ§Ã£o aplicada

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
  packages-to-scan: com.global.votacao.adapter.in.web
```

