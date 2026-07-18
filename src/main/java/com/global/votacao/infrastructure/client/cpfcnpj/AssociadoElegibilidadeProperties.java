package com.global.votacao.infrastructure.client.cpfcnpj;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "votacao.elegibilidade")
public record AssociadoElegibilidadeProperties(
        boolean habilitada,
        String baseUrl,
        String token,
        Integer codigoCpf,
        Integer codigoCnpj
) {}


