package com.global.votacao.adapter.in.web.rest;

import com.global.votacao.adapter.in.web.api.IVotoController;
import com.global.votacao.application.dto.RegistrarVotoRequest;
import com.global.votacao.application.dto.VotoResponse;
import com.global.votacao.application.service.VotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VotoController implements IVotoController {

    private final VotoService votoService;

    @Override
    public ResponseEntity<VotoResponse> registrar(Long pautaId, RegistrarVotoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(votoService.registrar(pautaId, request));
    }
}


