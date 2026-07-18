package com.global.votacao.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AtualizarSessaoRequest(
        @NotNull @Positive Integer duracaoEmSegundos
) {
}


