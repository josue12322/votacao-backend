SELECT 'CREATE DATABASE votacao'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'votacao'
)\gexec

