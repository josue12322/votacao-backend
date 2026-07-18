CREATE TABLE IF NOT EXISTS pautas (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descricao VARCHAR(1000),
    criada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessoes_votacao (
    id BIGSERIAL PRIMARY KEY,
    pauta_id BIGINT NOT NULL,
    aberta_em TIMESTAMP NOT NULL,
    fecha_em TIMESTAMP NOT NULL,
    encerrada_em TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    resultado_publicado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_sessoes_votacao_pauta
        FOREIGN KEY (pauta_id)
        REFERENCES pautas (id),
    CONSTRAINT uk_sessoes_votacao_pauta
        UNIQUE (pauta_id),
    CONSTRAINT ck_sessoes_votacao_status
        CHECK (status IN ('CRIADA', 'DISPONIVEL', 'ENCERRADA', 'PUBLICADA')),
    CONSTRAINT ck_sessoes_votacao_periodo
        CHECK (fecha_em > aberta_em)
);

CREATE TABLE IF NOT EXISTS votos (
    id BIGSERIAL PRIMARY KEY,
    pauta_id BIGINT NOT NULL,
    tipo_documento VARCHAR(4) NOT NULL,
    documento VARCHAR(14) NOT NULL,
    tipo VARCHAR(3) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_votos_pauta
        FOREIGN KEY (pauta_id)
        REFERENCES pautas (id),
    CONSTRAINT uk_votos_pauta_documento
        UNIQUE (pauta_id, documento),
    CONSTRAINT ck_votos_tipo_documento
        CHECK (tipo_documento IN ('CPF', 'CNPJ')),
    CONSTRAINT ck_votos_documento_tamanho
        CHECK (
            (tipo_documento = 'CPF' AND char_length(documento) = 11)
            OR
            (tipo_documento = 'CNPJ' AND char_length(documento) = 14)
        ),
    CONSTRAINT ck_votos_tipo
        CHECK (tipo IN ('SIM', 'NAO'))
);

CREATE INDEX IF NOT EXISTS idx_sessoes_status_fecha_em
    ON sessoes_votacao (status, fecha_em);

CREATE INDEX IF NOT EXISTS idx_sessoes_resultado_pendente
    ON sessoes_votacao (status, resultado_publicado, fecha_em);

CREATE INDEX IF NOT EXISTS idx_votos_pauta_tipo
    ON votos (pauta_id, tipo);

CREATE INDEX IF NOT EXISTS idx_votos_documento
    ON votos (documento);

