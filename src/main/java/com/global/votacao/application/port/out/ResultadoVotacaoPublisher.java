package com.global.votacao.application.port.out;

import com.global.votacao.application.event.ResultadoVotacaoEvento;

public interface ResultadoVotacaoPublisher {

    void publicar(ResultadoVotacaoEvento evento);
}


