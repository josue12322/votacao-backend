package com.global.votacao.application.dto;

import com.global.votacao.domain.model.StatusResultado;
import com.global.votacao.domain.model.VencedorVotacao;

public record ResultadoVotacaoResponse(
        Long pautaId,
        long votosSim,
        long votosNao,
        long totalVotos,
        StatusResultado status,
        VencedorVotacao vencedor
) {
}


