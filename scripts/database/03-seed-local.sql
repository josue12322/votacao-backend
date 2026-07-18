INSERT INTO pautas (titulo, descricao)
VALUES
    ('Aprovação de nova política de crédito', 'Votação sobre a política proposta para o próximo ciclo.')
ON CONFLICT DO NOTHING;

