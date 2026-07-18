package com.global.votacao.application.dto;

import lombok.*;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PautaResponse{

    private Long id;
    private String titulo;
    private String descricao;
    private LocalDateTime criadaEm;
}


