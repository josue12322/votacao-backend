# Fluxo detalhado do processo - API de Votação

## 1. Visão geral do fluxo

A aplicação gerencia o ciclo completo de uma votação:

```text
Criar pautaEntity
    -> Abrir sessão de votação
        -> Receber votos enquanto a sessão estiver aberta
            -> Finalizar sessão vencida
                -> Contabilizar votos
                    -> Publicar resultado no Kafka
                        -> Permitir consulta do resultado
```

O fluxo deve ser simples, previsível e resiliente. A API recebe comandos dos usuários e o job de finalização garante que sessões vencidas sejam encerradas e publicadas de forma assíncrona.

## 2. Estados principais

### 2.1 Estado da pautaEntity

A pautaEntity não precisa de muitos estados no escopo inicial. Ela existe como o assunto a ser votado.

Estados lógicos:

- `CRIADA`: pautaEntity cadastrada, ainda sem sessão.
- `COM_SESSAO_ABERTA`: pautaEntity possui sessão aberta.
- `COM_SESSAO_ENCERRADA`: pautaEntity possui sessão encerrada.
- `RESULTADO_PUBLICADO`: resultado da pautaEntity já foi publicado no Kafka.

Esses estados podem ser derivados da sessão vinculada.

### 2.2 Estado da sessão de votação

Estados recomendados:

- `ABERTA`: sessão criada e apta a receber votos até `fechaEm`.
- `ENCERRADA`: sessão finalizada formalmente, sem aceitar novos votos.
- `PUBLICADA`: resultado final já foi enviado com sucesso ao Kafka.

Transições válidas:

```text
ABERTA -> ENCERRADA -> PUBLICADA
```

Transições inválidas:

- `PUBLICADA -> ABERTA`
- `ENCERRADA -> ABERTA`
- `ABERTA -> PUBLICADA` sem passar por encerramento lógico.

## 3. Fluxo de criação de pautaEntity

Endpoint:

```http
POST /api/v1/pautas
```

Entrada esperada:

```json
{
  "titulo": "Aprovação de nova política de crédito",
  "descricao": "Votação sobre a política proposta para o próximo ciclo."
}
```

Processo:

1. API recebe o request.
2. Valida se `titulo` foi informado.
3. Cria entidade `Pauta`.
4. Persiste a pautaEntity no banco.
5. Retorna HTTP `201 Created`.

Regras:

- `titulo` é obrigatório.
- `descricao` é opcional.
- A pautaEntity deve ser persistida imediatamente.
- Pauta criada não abre sessão automaticamente.

Erros esperados:

- `400 Bad Request`: título ausente ou inválido.

## 4. Fluxo de consulta de pautaEntity

Endpoint:

```http
GET /api/v1/pautas/{pautaId}
```

Processo:

1. API recebe o `pautaId`.
2. Busca a pautaEntity no banco.
3. Se existir, retorna os dados.
4. Se não existir, retorna erro.

Regras:

- Não deve retornar votos junto com a pautaEntity nesse endpoint.
- Resultado deve ficar em endpoint próprio.

Erros esperados:

- `404 Not Found`: pautaEntity inexistente.

## 5. Fluxo de abertura de sessão

### 5.1 Abertura com duração default

Endpoint:

```http
POST /api/v1/pautas/{pautaId}/sessoes
```

Regra:

- Se `duracaoEmSegundos` não for informado no path, usar `60 segundos`.

### 5.2 Abertura com duração customizada

Endpoint:

```http
POST /api/v1/pautas/{pautaId}/sessoes/{duracaoEmSegundos}
```

Exemplo:

```http
POST /api/v1/pautas/1/sessoes/120
```

Processo:

1. API recebe `pautaId`.
2. API recebe ou define `duracaoEmSegundos`.
3. Busca a pautaEntity no banco.
4. Verifica se já existe sessão para a pautaEntity.
5. Calcula `abertaEm = agora`.
6. Calcula `fechaEm = abertaEm + duracaoEmSegundos`.
7. Cria sessão com status `ABERTA`.
8. Persiste sessão.
9. Retorna HTTP `201 Created`.

Resposta esperada:

```json
{
  "id": 10,
  "pautaId": 1,
  "abertaEm": "2026-07-16T19:30:00",
  "fechaEm": "2026-07-16T19:32:00",
  "status": "ABERTA"
}
```

Regras:

- `duracaoEmSegundos` deve ser maior que zero.
- Se não informado, usar `60`.
- Não pode abrir sessão para pautaEntity inexistente.
- Não pode abrir mais de uma sessão para a mesma pautaEntity.
- Sessão nasce sempre como `ABERTA`.
- O fechamento não ocorre no momento da abertura; ele será feito pelo job ou endpoint de finalização.

Erros esperados:

- `400 Bad Request`: duração inválida.
- `404 Not Found`: pautaEntity inexistente.
- `409 Conflict`: pautaEntity já possui sessão.

## 6. Fluxo de registro de voto

Endpoint:

```http
POST /api/v1/pautas/{pautaId}/votos
```

Entrada esperada:

```json
{
  "associadoId": 123,
  "tipoDocumento": "CPF",
  "documento": "61500421381",
  "voto": "SIM"
}
```

Processo:

1. API recebe `pautaId`, `associadoId`, `tipoDocumento`, `documento` e `voto`.
2. Valida payload obrigatório.
3. Busca pautaEntity.
4. Busca sessão da pautaEntity.
5. Verifica se a sessão está apta a receber voto.
6. Se validação de CPF estiver habilitada, consulta serviço externo.
7. Verifica se associado já votou na pautaEntity.
8. Cria voto.
9. Persiste voto.
10. Retorna HTTP `201 Created`.

### 6.1 Regra de sessão apta

A sessão aceita voto somente se:

```text
status == ABERTA
e
agora < fechaEm
```

Se `agora >= fechaEm`, o voto deve ser negado mesmo que o job ainda não tenha processado a finalização.

Isso evita voto atrasado quando o cron ainda não executou.

### 6.2 Regra de voto único

Um associado pode votar apenas uma vez por pautaEntity.

Validação em duas camadas:

1. Serviço verifica se já existe voto para `pautaId` + `associadoId`.
2. Banco garante constraint única em `pauta_id` + `associado_id`.

A constraint é obrigatória para proteger concorrência.

### 6.3 Regra de CPF

Quando a validação externa estiver habilitada:

1. Aplicação chama:

```http
GET https://api.cpfcnpj.com.br/{token}/{codigo}/{documento}
```

2. Se `tipoDocumento = CPF`, usa o pacote CPF E com código `9`.
3. Se `tipoDocumento = CNPJ`, usa o pacote CNPJ D com código `6`.
4. Se retornar `status = 1`, segue o voto.
5. Se retornar `status = 0`, nega o voto usando a mensagem do campo `erro`.
6. Se houver erro HTTP, considera falha na dependência externa.
5. Se houver indisponibilidade, retorna erro controlado.

Regras:

- CPF inválido não registra voto.
- Associado inapto não registra voto.
- Falha externa não deve salvar voto.
- Logs não devem expor CPF completo.

Erros esperados:

- `400 Bad Request`: payload inválido ou voto diferente de `SIM`/`NAO`.
- `404 Not Found`: pautaEntity ou sessão inexistente.
- `409 Conflict`: associado já votou.
- `422 Unprocessable Entity`: sessão encerrada, CPF inválido ou associado inapto.
- `503 Service Unavailable`: serviço externo indisponível, se a validação for obrigatória.

## 7. Fluxo de consulta de resultado

Endpoint:

```http
GET /api/v1/pautas/{pautaId}/resultado
```

Processo:

1. API recebe `pautaId`.
2. Busca pautaEntity.
3. Busca sessão da pautaEntity.
4. Conta votos `SIM` por agregação no banco.
5. Conta votos `NAO` por agregação no banco.
6. Calcula total.
7. Define vencedor.
8. Retorna resultado.

Resposta esperada:

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

Regras:

- Se votos `SIM` forem maiores que votos `NAO`, vencedor é `SIM`.
- Se votos `NAO` forem maiores que votos `SIM`, vencedor é `NAO`.
- Se houver empate, vencedor é `EMPATE`.
- Se a sessão ainda estiver aberta, resultado pode ser retornado como parcial com status `SESSAO_ABERTA`.
- O endpoint de resultado não deve publicar evento Kafka por padrão, para evitar efeito colateral em consulta.

Erros esperados:

- `404 Not Found`: pautaEntity inexistente.
- `404 Not Found`: sessão inexistente, se a pautaEntity ainda não teve votação aberta.

## 8. Fluxo de finalização por cron job

O cron job é o mecanismo recomendado para finalizar sessões vencidas e publicar o resultado no Kafka.

Componente sugerido:

```text
SessaoVotacaoFinalizacaoJob
```

Responsabilidades:

- Encontrar sessões vencidas.
- Encerrar sessões.
- Calcular resultado.
- Publicar evento Kafka.
- Marcar publicação como concluída.
- Permitir retentativa em caso de falha.

### 8.1 Frequência do job

Sugestão inicial:

```text
A cada 10 segundos ou 30 segundos
```

A frequência deve ser configurável por propriedade:

```text
votacao.finalizacao.cron
```

Exemplo:

```text
*/10 * * * * *
```

### 8.2 Consulta performática

O job deve buscar apenas sessões elegíveis:

```text
status = ABERTA
e
fechaEm <= agora
```

Consulta deve:

- Usar índice em `status` + `fecha_em`.
- Ordenar por `fecha_em`.
- Processar com pool-size configurável e consulta limitada a 10k registros.
- Limitar quantidade por execução.

Propriedade sugerida:

```text
votacao.finalizacao.pool-size=4
```

### 8.3 Processo do job

Fluxo:

```text
Início do job
    -> Buscar até N sessões ABERTAS vencidas
        -> Para cada sessão
            -> Abrir transação
            -> Revalidar status e fechaEm
            -> Contabilizar votos
            -> Marcar sessão como ENCERRADA
            -> Confirmar transação
            -> Publicar evento Kafka
            -> Se publicar com sucesso, marcar como PUBLICADA
            -> Se falhar, manter ENCERRADA e resultadoPublicado=false
Fim do job
```

### 8.4 Por que revalidar dentro da transação

Mesmo que a consulta inicial encontre uma sessão vencida, outro processo pode ter encerrado a mesma sessão antes.

Por isso, antes de encerrar, validar novamente:

```text
status == ABERTA
e
fechaEm <= agora
```

Se não atender, ignorar.

### 8.5 Idempotência

O job deve ser idempotente.

Regras:

- Sessão `PUBLICADA` nunca deve ser publicada novamente.
- Sessão com `resultadoPublicado = true` deve ser ignorada.
- Sessão `ENCERRADA` e `resultadoPublicado = false` pode ser republicada.
- Publicação Kafka deve usar chave estável, preferencialmente `pautaId` ou `sessaoId`.

### 8.6 Falha ao publicar no Kafka

Se o Kafka falhar:

1. Sessão permanece `ENCERRADA`.
2. Campo `resultadoPublicado` permanece `false`.
3. Job tentará publicar novamente em próxima execução.
4. Erro é registrado em log.

O voto não deve ser reaberto por falha de mensageria.

### 8.7 Concorrência entre múltiplas instâncias

Se a aplicação rodar com mais de uma instância:

- Usar atualização condicional por status.
- Ou usar lock pessimista no banco.
- Ou usar mecanismo como ShedLock para garantir execução única do job.

Estratégia simples recomendada para o teste:

```text
UPDATE sessoes_votacao
SET status = 'ENCERRADA', encerrada_em = now()
WHERE id = ?
AND status = 'ABERTA'
AND fecha_em <= now()
```

Se a atualização afetar `0` linhas, outra instância já processou.

## 9. Fluxo alternativo de finalização por endpoint

Caso não seja usado cron job interno, a aplicação pode expor endpoint administrativo.

Endpoint:

```http
POST /api/v1/sessoes/finalizar
```

Processo:

1. Recebe chamada manual ou de scheduler externo.
2. Executa o mesmo caso de uso do job.
3. Busca sessões vencidas.
4. Encerra e publica resultados.
5. Retorna quantidade processada.

Resposta:

```json
{
  "sessoesProcessadas": 25
}
```

Regras:

- Deve reutilizar a mesma lógica do job.
- Não deve duplicar código de finalização.
- Deve respeitar `pool-size` configurável e limite de consulta de `10_000` registros.
- Deve ignorar sessões já publicadas.
- Pode ser protegido futuramente por autenticação/autorização.

Quando usar:

- Ambientes onde o cron interno não é desejado.
- Execução controlada por infraestrutura externa.
- Reprocessamento manual após falha temporária de Kafka.

## 10. Fluxo de publicação Kafka

Componente sugerido:

```text
ResultadoVotacaoPublisher
```

Tópico sugerido:

```text
votacao.resultado.encerrado
```

Chave da mensagem:

```text
pautaId
```

Payload sugerido:

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

Processo:

1. Finalização calcula resultado.
2. Monta evento sintetizado.
3. Envia evento ao tópico Kafka.
4. Aguarda confirmação de envio ou captura falha.
5. Se sucesso, marca sessão como `PUBLICADA`.
6. Se falha, mantém pendente para retentativa.

Regras:

- Evento deve representar o resultado final, não parcial.
- Evento deve ser publicado apenas uma vez com sucesso.
- Consumidores não devem precisar consultar a API para entender o resultado.
- Payload deve conter dados da pautaEntity e síntese da votação.

## 11. Fluxo de dados no banco

### 11.1 Ao criar pautaEntity

Tabela afetada:

```text
pautas
```

Dados gravados:

- `id`
- `titulo`
- `descricao`
- `criada_em`

### 11.2 Ao abrir sessão

Tabela afetada:

```text
sessoes_votacao
```

Dados gravados:

- `id`
- `pauta_id`
- `aberta_em`
- `fecha_em`
- `status = ABERTA`
- `resultado_publicado = false`

### 11.3 Ao votar

Tabela afetada:

```text
votos
```

Dados gravados:

- `id`
- `pauta_id`
- `associado_id`
- `tipo_documento`
- `documento`
- `tipo`
- `criado_em`

### 11.4 Ao finalizar sessão

Tabela afetada:

```text
sessoes_votacao
```

Dados atualizados:

- `status = ENCERRADA`
- `encerrada_em = agora`

### 11.5 Ao publicar resultado

Tabela afetada:

```text
sessoes_votacao
```

Dados atualizados:

- `status = PUBLICADA`
- `resultado_publicado = true`

## 12. Regras de performance

- Nunca carregar todos os votos de uma pautaEntity em memória para calcular resultado.
- Usar `COUNT` no banco por tipo de voto.
- Criar índice em `votos(pauta_id, tipo)`.
- Criar índice único em `votos(pauta_id, associado_id)`.
- Criar índice em `sessoes_votacao(status, fecha_em)`.
- Cron job deve processar sessões em lotes.
- O `pool-size` deve ser configurável e a consulta do batch deve limitar até `10_000` registros.
- Logs do job devem informar quantidade processada, não payload completo de cada sessão.

## 13. Regras de erro e exceção

Tratamento deve ser centralizado.

Mapeamento:

- `400 Bad Request`: entrada inválida.
- `404 Not Found`: recurso inexistente.
- `409 Conflict`: conflito de estado ou duplicidade.
- `422 Unprocessable Entity`: regra de negócio violada.
- `503 Service Unavailable`: dependência externa indisponível.
- `500 Internal Server Error`: erro inesperado.

Formato recomendado:

```json
{
  "timestamp": "2026-07-16T19:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Associado já votou nesta pautaEntity",
  "path": "/api/v1/pautas/1/votos"
}
```

## 14. Regras de logs

Logs obrigatórios:

- Pauta criada.
- Sessão aberta.
- Voto registrado.
- Voto recusado por sessão encerrada.
- Voto recusado por duplicidade.
- Início e fim do job de finalização.
- Quantidade de sessões processadas pelo job.
- Publicação Kafka com sucesso.
- Falha na publicação Kafka.
- Falha no serviço externo de CPF.

Cuidados:

- Não logar CPF completo.
- Não logar payload sensível inteiro.
- Usar identificadores técnicos como `pautaId`, `sessaoId` e `associadoId`.

## 15. Fluxos de borda

### 15.1 Voto chega exatamente no horário de fechamento

Regra:

```text
agora >= fechaEm
```

Resultado:

- Voto negado.

### 15.2 Cron ainda não finalizou, mas sessão já venceu

Resultado:

- API de voto deve negar voto.
- Consulta de resultado pode indicar sessão vencida ou ainda não publicada.
- Job finalizará na próxima execução.

### 15.3 Kafka falha após sessão encerrada

Resultado:

- Sessão fica `ENCERRADA`.
- `resultadoPublicado = false`.
- Job tenta publicar novamente depois.

### 15.4 Duas chamadas de voto simultâneas do mesmo associado

Resultado:

- Uma grava com sucesso.
- A outra falha por constraint única.
- API retorna `409 Conflict`.

### 15.5 Endpoint de finalização chamado enquanto cron executa

Resultado:

- Atualização condicional evita duplicidade.
- Apenas um processo encerra/publica com sucesso.
- O outro ignora sessões já processadas.

## 16. Fluxo recomendado para implementação

Ordem sugerida:

1. Criar entidades e enums.
2. Criar repositories com consultas necessárias.
3. Criar services de pautaEntity, sessão, voto e resultado.
4. Criar controllers REST.
5. Criar handler global de exceções.
6. Criar finalizador de sessões reutilizável.
7. Criar job `@Scheduled`.
8. Criar publisher Kafka.
9. Criar endpoint alternativo de finalização.
10. Criar testes unitários e integração.

## 17. Critérios de aceite do fluxo

- Pauta é criada e persistida.
- Sessão abre com default de `60 segundos`.
- Sessão abre com duração customizada pelo path.
- Voto só é aceito durante sessão aberta.
- Voto duplicado é bloqueado.
- Resultado é calculado por agregação.
- Sessões vencidas são finalizadas por job ou endpoint.
- Resultado final é enviado ao Kafka.
- Falha no Kafka permite retentativa.
- Sessão publicada não é republicada.
