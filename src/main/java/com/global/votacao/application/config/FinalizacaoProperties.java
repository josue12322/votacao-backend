package com.global.votacao.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "votacao.finalizacao")
public record FinalizacaoProperties(
        boolean habilitada,
        int poolSize
) {
    public FinalizacaoProperties {
        if (poolSize <= 0) {
            poolSize = 4;
        }
    }
}
