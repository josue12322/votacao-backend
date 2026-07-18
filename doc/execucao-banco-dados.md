# Execução do banco de dados

## Docker Compose

Subir PostgreSQL local:

```bash
docker compose up -d postgres
```

O container cria o banco `votacao` e executa automaticamente:

```text
scripts/database/01-create-schema.sql
```

Subir PostgreSQL e Kafka:

```bash
docker compose --profile kafka up -d
```

## Execução manual dos scripts

Criar o banco:

```bash
psql -U postgres -h localhost -f scripts/database/00-create-database.sql
```

Criar tabelas, constraints e índices:

```bash
psql -U postgres -h localhost -d votacao -f scripts/database/01-create-schema.sql
```

Inserir dados locais opcionais:

```bash
psql -U postgres -h localhost -d votacao -f scripts/database/03-seed-local.sql
```

Remover tabelas locais:

```bash
psql -U postgres -h localhost -d votacao -f scripts/database/02-drop-schema.sql
```

## Objetos criados

- `pautas`
- `sessoes_votacao`
- `votos`
- Constraints de integridade referencial.
- Checks para enums persistidos como texto.
- Índices para finalização de sessões e contabilização de votos.

