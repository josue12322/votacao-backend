package com.global.votacao.infrastructure.scheduler;

import com.global.votacao.application.config.FinalizacaoProperties;
import com.global.votacao.application.service.FinalizacaoSessaoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FinalizacaoSessaoJob {

    private final FinalizacaoSessaoService finalizacaoSessaoService;
    private final FinalizacaoProperties properties;

    public FinalizacaoSessaoJob(FinalizacaoSessaoService finalizacaoSessaoService, FinalizacaoProperties properties) {
        this.finalizacaoSessaoService = finalizacaoSessaoService;
        this.properties = properties;
    }

    @Scheduled(cron = "${votacao.finalizacao.cron:*/10 * * * * *}")
    public void executar() {
        if (properties.habilitada()) {
            finalizacaoSessaoService.finalizarPendentes();
        }
    }
}


