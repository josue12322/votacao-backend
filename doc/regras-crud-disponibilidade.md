# Regras de CRUD e disponibilidade

## Objetivo

Este documento complementa as regras da aplicação para separar claramente o momento de cadastro/configuração da sessão do momento em que ela fica disponível para votação.

## Estados da sessão

- `CRIADA`: sessão criada, configurável e ainda sem votação liberada.
- `DISPONIVEL`: sessão liberada para receber votos até `fechaEm`.
- `ENCERRADA`: sessão finalizada, sem receber novos votos.
- `PUBLICADA`: resultado final publicado no Kafka.

## Fluxo da sessão

```text
CRIADA
    -> atualizar duração
    -> deletar sessão
    -> disponibilizar para votação
        -> DISPONIVEL
            -> receber votos
            -> encerrar automaticamente por cron
            -> encerrar manualmente por endpoint
                -> ENCERRADA
                    -> publicar resultado Kafka
                        -> PUBLICADA
```

## Regras de pauta

- Pauta pode ser criada, listada, consultada, atualizada e removida.
- Pauta pode ser atualizada enquanto não houver voto e enquanto a sessão vinculada ainda não tiver sido disponibilizada.
- Pauta não pode ser atualizada após sessão `DISPONIVEL`, `ENCERRADA` ou `PUBLICADA`.
- Pauta não pode ser removida se possuir votos.
- Pauta não pode ser removida se possuir sessão vinculada; primeiro a sessão `CRIADA` deve ser removida.

## Regras de sessão

- Sessão pode ser criada para uma pauta existente.
- Sessão nasce como `CRIADA`.
- Sessão `CRIADA` não recebe votos.
- Sessão `CRIADA` pode ser atualizada.
- Sessão `CRIADA` pode ser removida.
- Sessão é liberada para votação por `POST /api/v1/sessoes/{sessaoId}/disponibilizar`.
- Após disponibilizar, a sessão passa para `DISPONIVEL`.
- Sessão `DISPONIVEL` não pode voltar para `CRIADA`.
- Sessão `DISPONIVEL` não pode ser atualizada.
- Sessão `DISPONIVEL` não pode ser removida.
- Sessão `DISPONIVEL` pode ser encerrada automaticamente pelo cron ao vencer.
- Sessão `DISPONIVEL` pode ser encerrada manualmente por `POST /api/v1/sessoes/{sessaoId}/encerrar`.

## Regras de voto

- Voto só é aceito se a sessão estiver `DISPONIVEL`.
- Voto só é aceito enquanto `agora < fechaEm`.
- Voto é único por pauta e documento.
- O documento pode ser CPF ou CNPJ.
- Após votar, o documento não pode refazer o voto naquela pauta.
- Não deve existir endpoint para atualizar voto.
- Não deve existir endpoint para deletar voto.

## Endpoints adicionados

### Pautas

```http
GET /api/v1/pautas
PUT /api/v1/pautas/{pautaId}
DELETE /api/v1/pautas/{pautaId}
```

### Sessões

```http
POST /api/v1/pautas/{pautaId}/sessoes
POST /api/v1/pautas/{pautaId}/sessoes/{duracaoEmSegundos}
GET /api/v1/sessoes
GET /api/v1/sessoes/{sessaoId}
PUT /api/v1/sessoes/{sessaoId}
DELETE /api/v1/sessoes/{sessaoId}
POST /api/v1/sessoes/{sessaoId}/disponibilizar
POST /api/v1/sessoes/{sessaoId}/encerrar
POST /api/v1/sessoes/finalizar
```
