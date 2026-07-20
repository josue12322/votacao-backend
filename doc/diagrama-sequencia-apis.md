# Diagramas de sequência das APIs

Este documento apresenta os principais fluxos de chamada da API de votação usando diagramas de sequência em Mermaid.

## 1. Criar pauta

Endpoint:

```http
POST /api/v1/pautas
```

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as "Cliente/API Consumer"
    participant Controller as PautaController
    participant Service as PautaService
    participant Mapper as PautaMapper
    participant DB as PostgreSQL

    Cliente->>Controller: POST /api/v1/pautas
    Controller->>Controller: Valida payload com @Valid
    Controller->>Service: criar(request)
    Service->>Mapper: toMapperPautaEntity(request)
    Mapper-->>Service: PautaEntity
    Service->>DB: INSERT pautas
    DB-->>Service: PautaEntity persistida
    Service->>Mapper: toResponse(pauta)
    Mapper-->>Service: PautaResponse
    Service-->>Controller: PautaResponse
    Controller-->>Cliente: 201 Created
```

Fluxos de erro:

- `400 Bad Request`: título em branco ou payload inválido.
- `500 Internal Server Error`: erro inesperado de persistência ou aplicação.

## 2. Visualizar pautas

Endpoints:

```http
GET /api/v1/pautas
GET /api/v1/pautas/{pautaId}
GET /api/v1/pautas/{pautaId}/sessao-aberta
```

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as "Cliente/API Consumer"
    participant Controller as PautaController
    participant Service as PautaService
    participant PautaRepo as PautaRepository
    participant SessaoRepo as SessaoVotacaoRepository
    participant Mapper as Mapper
    participant DB as PostgreSQL

    alt Listar todas as pautas
        Cliente->>Controller: GET /api/v1/pautas
        Controller->>Service: listar()
        Service->>PautaRepo: findAll()
        PautaRepo->>DB: SELECT pautas
        DB-->>PautaRepo: pautas
        PautaRepo-->>Service: List<PautaEntity>
        Service->>Mapper: toResponse()
        Service-->>Controller: List<PautaResponse>
        Controller-->>Cliente: 200 OK
    else Buscar pauta por id
        Cliente->>Controller: GET /api/v1/pautas/{pautaId}
        Controller->>Service: buscarPorId(pautaId)
        Service->>PautaRepo: findById(pautaId)
        PautaRepo->>DB: SELECT pauta WHERE id = ?
        DB-->>PautaRepo: pauta ou vazio
        alt Pauta encontrada
            Service->>Mapper: toResponse(pauta)
            Service-->>Controller: PautaResponse
            Controller-->>Cliente: 200 OK
        else Pauta não encontrada
            Service-->>Controller: RecursoNaoEncontradoException
            Controller-->>Cliente: 404 Not Found
        end
    else Buscar pauta com sessão
        Cliente->>Controller: GET /api/v1/pautas/{pautaId}/sessao-aberta
        Controller->>Service: buscarComSessaoAberta(pautaId)
        Service->>PautaRepo: findById(pautaId)
        Service->>SessaoRepo: findByPautaEntityId(pautaId)
        SessaoRepo->>DB: SELECT sessao WHERE pauta_id = ?
        Service->>Mapper: mapear pauta e sessão
        Service-->>Controller: PautaSessaoAbertaResponse
        Controller-->>Cliente: 200 OK
    end
```

## 3. Atualizar e deletar pauta

Endpoints:

```http
PUT /api/v1/pautas/{pautaId}
DELETE /api/v1/pautas/{pautaId}
```

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as "Cliente/API Consumer"
    participant Controller as PautaController
    participant Service as PautaService
    participant VotoRepo as VotoRepository
    participant SessaoRepo as SessaoVotacaoRepository
    participant PautaRepo as PautaRepository
    participant DB as PostgreSQL

    Cliente->>Controller: PUT ou DELETE /api/v1/pautas/{pautaId}
    Controller->>Service: atualizar() ou deletar()
    Service->>VotoRepo: existsByPautaEntityId(pautaId)
    VotoRepo->>DB: SELECT EXISTS votos

    alt Pauta possui voto
        Service-->>Controller: ConflitoException
        Controller-->>Cliente: 409 Conflict
    else Sem votos
        Service->>SessaoRepo: existsByPautaEntityIdAndStatusIn(...)
        SessaoRepo->>DB: SELECT EXISTS sessões bloqueantes
        alt Sessão já disponibilizada/encerrada/publicada
            Service-->>Controller: ConflitoException
            Controller-->>Cliente: 409 Conflict
        else Pauta pode ser alterada/removida
            Service->>PautaRepo: findById(pautaId)
            alt Atualizar
                Service->>PautaRepo: save(pauta atualizada)
                PautaRepo->>DB: UPDATE pautas
                Service-->>Controller: PautaResponse
                Controller-->>Cliente: 200 OK
            else Deletar
                Service->>SessaoRepo: existsByPautaEntityId(pautaId)
                alt Possui sessão vinculada
                    Service-->>Controller: ConflitoException
                    Controller-->>Cliente: 409 Conflict
                else Sem sessão vinculada
                    Service->>PautaRepo: delete(pauta)
                    PautaRepo->>DB: DELETE pautas
                    Controller-->>Cliente: 204 No Content
                end
            end
        end
    end
```

## 4. Criar sessão de votação

Endpoint:

```http
POST /api/v1/pautas/{pautaId}/sessoes
```

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as "Cliente/API Consumer"
    participant Controller as SessaoVotacaoController
    participant Service as SessaoVotacaoService
    participant PautaService as PautaService
    participant SessaoRepo as SessaoVotacaoRepository
    participant Mapper as SessaoMapper
    participant DB as PostgreSQL

    Cliente->>Controller: POST /api/v1/pautas/{pautaId}/sessoes
    Controller->>Controller: Valida duracaoEmSegundos quando informado
    Controller->>Service: criar(pautaId, request)
    Service->>Service: Define duração informada ou votacao.duracao-voto
    Service->>PautaService: buscarEntidade(pautaId)
    PautaService->>DB: SELECT pauta WHERE id = ?
    Service->>SessaoRepo: existsByPautaEntityId(pautaId)
    SessaoRepo->>DB: SELECT EXISTS sessão da pauta

    alt Pauta já possui sessão
        Service-->>Controller: ConflitoException
        Controller-->>Cliente: 409 Conflict
    else Regra 1x1 válida
        Service->>Mapper: toMapperSessaoEntity(pauta, duracao)
        Mapper-->>Service: SessaoVotacaoEntity status CRIADA
        Service->>SessaoRepo: save(sessao)
        SessaoRepo->>DB: INSERT sessoes_votacao
        DB-->>SessaoRepo: sessão persistida
        Service-->>Controller: SessaoVotacaoResponse
        Controller-->>Cliente: 201 Created
    end
```

Observação de negócio:

- A sessão nasce como `CRIADA`.
- O prazo de votação ainda não começa nesse momento.
- A contagem começa apenas quando a sessão é disponibilizada.

## 5. Disponibilizar sessão para votação

Endpoint:

```http
POST /api/v1/sessoes/{sessaoId}/disponibilizar
```

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as "Cliente/API Consumer"
    participant Controller as SessaoVotacaoController
    participant Service as SessaoVotacaoService
    participant SessaoRepo as SessaoVotacaoRepository
    participant Sessao as SessaoVotacaoEntity
    participant DB as PostgreSQL

    Cliente->>Controller: POST /api/v1/sessoes/{sessaoId}/disponibilizar
    Controller->>Service: disponibilizar(sessaoId)
    Service->>SessaoRepo: findById(sessaoId)
    SessaoRepo->>DB: SELECT sessão WHERE id = ?
    DB-->>SessaoRepo: sessão

    alt Sessão não encontrada
        Service-->>Controller: RecursoNaoEncontradoException
        Controller-->>Cliente: 404 Not Found
    else Sessão não está CRIADA
        Service-->>Controller: ConflitoException
        Controller-->>Cliente: 409 Conflict
    else Sessão está CRIADA
        Service->>Sessao: disponibilizar(agora)
        Note over Sessao: Calcula duração original antes de trocar abertaEm
        Note over Sessao: abertaEm = agora
        Note over Sessao: fechaEm = agora + duracaoOriginal
        Note over Sessao: status = DISPONIVEL
        Service->>SessaoRepo: save(sessao)
        SessaoRepo->>DB: UPDATE sessoes_votacao
        Service-->>Controller: SessaoVotacaoResponse
        Controller-->>Cliente: 200 OK
    end
```

Regra importante:

- O prazo de encerramento fica na sessão.
- A votação começa a contar somente após a disponibilização.

## 6. Registrar voto

Endpoint:

```http
POST /api/v1/pautas/{pautaId}/votos
```

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as "Associado/API Consumer"
    participant Controller as VotoController
    participant Service as VotoService
    participant PautaService as PautaService
    participant SessaoRepo as SessaoVotacaoRepository
    participant Elegibilidade as CpfCnpjAssociadoElegibilidadeClient
    participant Feign as CpfCnpjFeignClient
    participant VotoRepo as VotoRepository
    participant Mapper as VotoMapper
    participant DB as PostgreSQL
    participant Externo as API CPF/CNPJ

    Cliente->>Controller: POST /api/v1/pautas/{pautaId}/votos
    Controller->>Controller: Valida payload com @Valid
    Controller->>Service: registrar(pautaId, request)
    Service->>PautaService: buscarEntidade(pautaId)
    PautaService->>DB: SELECT pauta
    Service->>SessaoRepo: findByPautaEntityId(pautaId)
    SessaoRepo->>DB: SELECT sessão da pauta

    alt Sessão inexistente
        Service-->>Controller: RecursoNaoEncontradoException
        Controller-->>Cliente: 404 Not Found
    else Sessão não aceita voto
        Service-->>Controller: RegraNegocioException
        Controller-->>Cliente: 422 Unprocessable Entity
    else Sessão DISPONIVEL dentro do prazo
        Service->>Service: Normaliza documento
        Service->>Elegibilidade: validarElegibilidade(documento, tipoDocumento)
        Elegibilidade->>Feign: consultarDocumento(token, codigo, documento)
        Feign->>Externo: GET /{token}/{codigo}/{documento}
        Externo-->>Feign: status 1 ou erro

        alt Documento inválido ou recusado
            Elegibilidade-->>Service: RegraNegocioException
            Service-->>Controller: RegraNegocioException
            Controller-->>Cliente: 422 Unprocessable Entity
        else Documento válido
            Service->>VotoRepo: existsByPautaEntityIdAndDocumento(pautaId, documento)
            VotoRepo->>DB: SELECT EXISTS voto
            alt Documento já votou na pauta
                Service-->>Controller: ConflitoException
                Controller-->>Cliente: 409 Conflict
            else Voto permitido
                Service->>Mapper: toMapperVoto(...)
                Service->>VotoRepo: save(voto)
                VotoRepo->>DB: INSERT votos
                DB-->>VotoRepo: voto persistido
                Service-->>Controller: VotoResponse
                Controller-->>Cliente: 201 Created
            end
        end
    end
```

## 7. Consultar resultado

Endpoint:

```http
GET /api/v1/pautas/{pautaId}/resultado
```

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as "Cliente/API Consumer"
    participant Controller as ResultadoVotacaoController
    participant Service as ResultadoVotacaoService
    participant PautaService as PautaService
    participant SessaoRepo as SessaoVotacaoRepository
    participant VotoRepo as VotoRepository
    participant DB as PostgreSQL

    Cliente->>Controller: GET /api/v1/pautas/{pautaId}/resultado
    Controller->>Service: consultarPorPauta(pautaId)
    Service->>PautaService: buscarEntidade(pautaId)
    PautaService->>DB: SELECT pauta
    Service->>SessaoRepo: findByPautaEntityId(pautaId)
    SessaoRepo->>DB: SELECT sessão da pauta

    alt Sessão inexistente
        Service-->>Controller: RecursoNaoEncontradoException
        Controller-->>Cliente: 404 Not Found
    else Sessão encontrada
        Service->>VotoRepo: countByPautaEntityIdAndTipo(pautaId, SIM)
        VotoRepo->>DB: SELECT COUNT votos SIM
        DB-->>VotoRepo: total SIM
        Service->>VotoRepo: countByPautaEntityIdAndTipo(pautaId, NAO)
        VotoRepo->>DB: SELECT COUNT votos NAO
        DB-->>VotoRepo: total NAO
        Service->>Service: Calcula total, status e vencedor
        Service-->>Controller: ResultadoVotacaoResponse
        Controller-->>Cliente: 200 OK
    end
```

Observação de performance:

- O resultado é calculado por `COUNT` no banco.
- A aplicação não carrega todos os votos em memória.

## 8. Encerrar sessão manualmente

Endpoint:

```http
POST /api/v1/sessoes/{sessaoId}/encerrar
```

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as "Cliente/API Consumer"
    participant Controller as SessaoVotacaoController
    participant Finalizacao as FinalizacaoSessaoService
    participant SessaoRepo as SessaoVotacaoRepository
    participant Resultado as ResultadoVotacaoService
    participant Publisher as KafkaResultadoVotacaoPublisher
    participant Kafka as Kafka Topic
    participant DB as PostgreSQL

    Cliente->>Controller: POST /api/v1/sessoes/{sessaoId}/encerrar
    Controller->>Finalizacao: encerrarForcado(sessaoId)
    Finalizacao->>SessaoRepo: findById(sessaoId)
    SessaoRepo->>DB: SELECT sessão

    alt Sessão inexistente
        Finalizacao-->>Controller: RecursoNaoEncontradoException
        Controller-->>Cliente: 404 Not Found
    else Sessão ainda CRIADA
        Finalizacao-->>Controller: RegraNegocioException
        Controller-->>Cliente: 422 Unprocessable Entity
    else Sessão pode ser encerrada
        Finalizacao->>SessaoRepo: save(status ENCERRADA)
        SessaoRepo->>DB: UPDATE sessão encerrada
        Finalizacao->>Resultado: calcular(pautaId, sessao)
        Resultado->>DB: COUNT votos SIM/NAO
        Resultado-->>Finalizacao: ResultadoVotacaoResponse
        Finalizacao->>Publisher: publicar(ResultadoVotacaoEvento)
        Publisher->>Publisher: Serializa evento em JSON
        Publisher->>Kafka: send(topic, key=pautaId, payload)
        Kafka-->>Publisher: ACK
        Finalizacao->>SessaoRepo: save(status PUBLICADA)
        SessaoRepo->>DB: UPDATE resultado_publicado = true
        Finalizacao-->>Controller: SessaoVotacaoResponse
        Controller-->>Cliente: 200 OK
    end
```

## 9. Finalizar sessões vencidas por endpoint

Endpoint:

```http
POST /api/v1/sessoes/finalizar
```

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as "Admin/Scheduler externo"
    participant Controller as SessaoVotacaoController
    participant Finalizacao as FinalizacaoSessaoService
    participant SessaoRepo as SessaoVotacaoRepository
    participant Pool as Pool de processamento
    participant Resultado as ResultadoVotacaoService
    participant Publisher as KafkaResultadoVotacaoPublisher
    participant Kafka as Kafka Topic
    participant DB as PostgreSQL

    Cliente->>Controller: POST /api/v1/sessoes/finalizar
    Controller->>Finalizacao: finalizarPendentes()
    Finalizacao->>SessaoRepo: buscar DISPONIVEL com fechaEm <= agora, limit 10k
    SessaoRepo->>DB: SELECT sessões vencidas
    Finalizacao->>SessaoRepo: buscar ENCERRADA e resultadoPublicado=false, limit 10k
    SessaoRepo->>DB: SELECT sessões pendentes de publicação
    Finalizacao->>Pool: Processar ids com pool-size configurável

    loop Para cada sessão elegível
        Pool->>SessaoRepo: findById(sessaoId)
        SessaoRepo->>DB: SELECT sessão
        alt Sessão não elegível ou já publicada
            Pool-->>Finalizacao: false
        else Sessão elegível
            Pool->>SessaoRepo: save(status ENCERRADA)
            SessaoRepo->>DB: UPDATE sessão
            Pool->>Resultado: calcular(pautaId, sessao)
            Resultado->>DB: COUNT votos SIM/NAO
            Pool->>Publisher: publicar(evento)
            Publisher->>Kafka: send(topic, key=pautaId, payload JSON)
            alt Publicação com sucesso
                Pool->>SessaoRepo: save(status PUBLICADA)
                SessaoRepo->>DB: UPDATE resultado_publicado = true
                Pool-->>Finalizacao: true
            else Falha no Kafka
                Pool-->>Finalizacao: true
                Note over Pool: Sessão fica ENCERRADA e resultadoPublicado=false para retentativa
            end
        end
    end

    Finalizacao-->>Controller: FinalizacaoSessoesResponse
    Controller-->>Cliente: 200 OK
```

## 10. Finalização automática por cron job

Componente:

```text
FinalizacaoSessaoJob
```

```mermaid
sequenceDiagram
    autonumber
    participant Scheduler as Spring Scheduler
    participant Job as FinalizacaoSessaoJob
    participant Finalizacao as FinalizacaoSessaoService
    participant SessaoRepo as SessaoVotacaoRepository
    participant Resultado as ResultadoVotacaoService
    participant Publisher as KafkaResultadoVotacaoPublisher
    participant Kafka as Kafka Topic
    participant DB as PostgreSQL

    Scheduler->>Job: Executa cron votacao.finalizacao.cron
    Job->>Job: Verifica votacao.finalizacao.habilitada
    alt Job desabilitado
        Job-->>Scheduler: Ignora execução
    else Job habilitado
        Job->>Finalizacao: finalizarPendentes()
        Finalizacao->>SessaoRepo: Busca vencidas e pendentes
        SessaoRepo->>DB: SELECT com índices por status/fechaEm
        Finalizacao->>Resultado: calcular resultado por sessão
        Resultado->>DB: COUNT votos
        Finalizacao->>Publisher: publicar evento
        Publisher->>Kafka: send payload JSON
        Finalizacao->>SessaoRepo: marcar PUBLICADA quando Kafka confirmar
        SessaoRepo->>DB: UPDATE sessão
        Finalizacao-->>Job: quantidade processada
        Job-->>Scheduler: Log de conclusão
    end
```

## 11. Publicação Kafka

Tópico padrão:

```text
votacao.resultado.encerrado
```

Payload JSON enviado:

```json
{
  "id": 21,
  "titulo": "Aprovação de nova política de crédito",
  "descricao": "Votação sobre a política proposta para o próximo ciclo.",
  "pauta": {
    "pautaId": 21,
    "votosSim": 1,
    "votosNao": 1,
    "totalVotos": 2,
    "status": "SESSAO_ENCERRADA",
    "vencedor": "EMPATE"
  }
}
```

```mermaid
sequenceDiagram
    autonumber
    participant Finalizacao as FinalizacaoSessaoService
    participant Publisher as KafkaResultadoVotacaoPublisher
    participant ObjectMapper as ObjectMapper
    participant Template as "KafkaTemplate<String,String>"
    participant Kafka as "votacao.resultado.encerrado"

    Finalizacao->>Publisher: publicar(ResultadoVotacaoEvento)
    Publisher->>ObjectMapper: writeValueAsString(evento)
    ObjectMapper-->>Publisher: payload JSON
    Publisher->>Template: send(topic, key=evento.id, payload)
    Template->>Kafka: Produz mensagem
    Kafka-->>Template: ACK
    Template-->>Publisher: CompletableFuture concluído
    Publisher-->>Finalizacao: publicação concluída
```

## 12. Tratamento global de erros

Componente:

```text
ApiExceptionHandler
```

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as "Cliente/API Consumer"
    participant Controller as Controller REST
    participant Service as Service
    participant Handler as ApiExceptionHandler

    Cliente->>Controller: Chamada REST
    Controller->>Service: Executa caso de uso

    alt Payload inválido
        Controller-->>Handler: MethodArgumentNotValidException
        Handler-->>Cliente: 400 Bad Request
    else Recurso inexistente
        Service-->>Handler: RecursoNaoEncontradoException
        Handler-->>Cliente: 404 Not Found
    else Conflito de estado ou duplicidade
        Service-->>Handler: ConflitoException
        Handler-->>Cliente: 409 Conflict
    else Regra de negócio violada
        Service-->>Handler: RegraNegocioException
        Handler-->>Cliente: 422 Unprocessable Entity
    else Dependência externa indisponível
        Service-->>Handler: DependenciaExternaException
        Handler-->>Cliente: 503 Service Unavailable
    else Erro inesperado
        Service-->>Handler: Exception
        Handler-->>Cliente: 500 Internal Server Error
    end
```

## Resumo das decisões representadas

- A pauta não controla prazo de votação.
- A sessão controla `abertaEm`, `fechaEm`, `status` e publicação do resultado.
- A duração da votação começa ao disponibilizar a sessão.
- A regra de voto único é validada na aplicação e reforçada no banco.
- Resultado é calculado por agregação no banco.
- Encerramento publica evento Kafka em JSON.
- Sessões não publicadas podem ser reprocessadas pelo cron ou endpoint de finalização.
