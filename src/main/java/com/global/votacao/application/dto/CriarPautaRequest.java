package com.global.votacao.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarPautaRequest(
        @NotBlank @Size(max = 150) String titulo,
        @Size(max = 1000) String descricao
) {
}


