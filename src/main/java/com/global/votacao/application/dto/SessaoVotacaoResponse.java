package com.global.votacao.application.dto;

import com.global.votacao.domain.model.StatusSessaoVotacao;
import java.time.LocalDateTime;

public record SessaoVotacaoResponse(
        Long id,
        Long pautaId,
        LocalDateTime abertaEm,
        LocalDateTime fechaEm,
        LocalDateTime encerradaEm,
        StatusSessaoVotacao status
) {
}


