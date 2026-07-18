package com.global.votacao.application.dto;

import com.global.votacao.domain.model.TipoDocumento;
import com.global.votacao.domain.model.TipoVoto;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VotoResponse {

    private Long id;
    private Long pautaId;
    private TipoDocumento tipoDocumento;
    private String documento;
    private TipoVoto voto;
    private LocalDateTime criadoEm;
}


