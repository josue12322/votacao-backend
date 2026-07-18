package com.global.votacao.application.dto;

import jakarta.validation.constraints.Positive;

public record CriarSessaoRequest(
        @Positive Integer duracaoEmSegundos
) {
    public int duracaoOuDefault() {
        return duracaoEmSegundos == null ? 60 : duracaoEmSegundos;
    }
}


