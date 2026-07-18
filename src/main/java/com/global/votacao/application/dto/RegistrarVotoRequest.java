package com.global.votacao.application.dto;

import com.global.votacao.domain.model.TipoDocumento;
import com.global.votacao.domain.model.TipoVoto;
import jakarta.validation.constraints.NotNull;

public record RegistrarVotoRequest(
        @NotNull TipoDocumento tipoDocumento,
        String documento,
        @NotNull TipoVoto voto
) {}


