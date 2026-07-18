# Scripts de banco de dados

Scripts PostgreSQL para provisionar a base local da API de votação.

## Ordem de execução

Conectado ao banco administrativo, por exemplo `postgres`:

```bash
psql -U postgres -h localhost -f scripts/database/00-create-database.sql
```

Conectado ao banco `votacao`:

```bash
psql -U postgres -h localhost -d votacao -f scripts/database/01-create-schema.sql
```

Opcionalmente, para inserir dados locais de exemplo:

```bash
psql -U postgres -h localhost -d votacao -f scripts/database/03-seed-local.sql
```

## Remover tabelas locais

Use apenas em ambiente local/desenvolvimento:

```bash
psql -U postgres -h localhost -d votacao -f scripts/database/02-drop-schema.sql
```

## Objetos criados

- Tabela `pautas`.
- Tabela `sessoes_votacao`.
- Tabela `votos`.
- Foreign keys entre pautas, sessões e votos.
- Constraint única de uma sessão por pauta.
- Constraint única de um voto por pauta e documento.
- Checks para status de sessão, tipo de documento e tipo de voto.
- Índices para finalização performática e contabilização de votos.

