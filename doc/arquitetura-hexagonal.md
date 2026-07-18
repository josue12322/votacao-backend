# Arquitetura Hexagonal e Clean Architecture

## Objetivo

Organizar a aplicação de votação separando regras de negócio, casos de uso e detalhes de infraestrutura. A dependência principal sempre aponta para dentro: adaptadores externos usam a aplicação, e a aplicação usa domínio e portas.

## Estrutura de pacotes

```text
com.global.votacao
├── adapter
│   └── in
│       └── web
│           ├── api
│           ├── exception
│           └── rest
├── application
│   ├── config
│   ├── dto
│   ├── event
│   ├── mapper
│   ├── port
│   │   └── out
│   └── service
├── domain
│   ├── entity
│   └── model
├── infrastructure
│   ├── client
│   │   └── cpfcnpj
│   ├── messaging
│   │   └── kafka
│   ├── persistence
│   │   └── repository
│   └── scheduler
└── shared
    └── exception
```

## Responsabilidades

- `adapter.in.web.api`: contratos REST documentados no Swagger/OpenAPI.
- `adapter.in.web.rest`: controllers HTTP que delegam para os casos de uso.
- `adapter.in.web.exception`: tratamento centralizado de erros da API.
- `application.service`: casos de uso de pauta, sessão, voto, resultado e finalização.
- `application.port.out`: portas de saída para integrações externas, como elegibilidade e publicação de resultado.
- `application.dto`: contratos de entrada e saída da aplicação.
- `application.mapper`: conversões entre DTOs e entidades de domínio.
- `application.event`: eventos de aplicação publicados para outras plataformas.
- `domain.entity`: entidades persistidas e regras próprias do domínio.
- `domain.model`: enums e modelos de domínio.
- `infrastructure.persistence.repository`: adaptadores de persistência com Spring Data JPA.
- `infrastructure.client.cpfcnpj`: adaptador Feign para consulta CPF/CNPJ.
- `infrastructure.messaging.kafka`: adaptador Kafka para publicação do encerramento.
- `infrastructure.scheduler`: cron job de finalização automática de sessões.
- `shared.exception`: exceções comuns usadas pelas camadas internas.

## Regras de dependência

- Controllers conhecem apenas DTOs e services da aplicação.
- Services orquestram regras e dependem de portas, repositórios e domínio.
- Domínio não depende de controllers, infraestrutura, Swagger, Feign ou Kafka.
- Infraestrutura implementa portas de saída e encapsula detalhes externos.
- Novas integrações externas devem entrar por `application.port.out` e ter implementação em `infrastructure`.

## Swagger

O Swagger escaneia os contratos REST em `com.global.votacao.adapter.in.web`, preservando a separação entre documentação da API e regras de negócio.
