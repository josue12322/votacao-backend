package com.global.votacao.application.event;

import com.global.votacao.application.dto.ResultadoVotacaoResponse;

public record ResultadoVotacaoEvento(
        Long id,
        String titulo,
        String descricao,
        ResultadoVotacaoResponse pauta
) {
}


