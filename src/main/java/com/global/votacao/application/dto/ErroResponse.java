package com.global.votacao.application.dto;

import java.time.LocalDateTime;

public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}


