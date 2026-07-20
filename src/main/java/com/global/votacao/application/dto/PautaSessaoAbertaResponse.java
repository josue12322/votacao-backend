package com.global.votacao.application.dto;

public record PautaSessaoAbertaResponse(
        PautaResponse pauta,
        SessaoVotacaoResponse sessao
) {
}
