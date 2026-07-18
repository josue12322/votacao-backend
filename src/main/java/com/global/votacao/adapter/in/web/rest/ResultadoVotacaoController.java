package com.global.votacao.adapter.in.web.rest;

import com.global.votacao.adapter.in.web.api.IResultadoVotacaoController;
import com.global.votacao.application.dto.ResultadoVotacaoResponse;
import com.global.votacao.application.service.ResultadoVotacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ResultadoVotacaoController implements IResultadoVotacaoController {

    private final ResultadoVotacaoService resultadoService;

    @Override
    public ResponseEntity<ResultadoVotacaoResponse> consultar(Long pautaId) {
        return ResponseEntity.ok(resultadoService.consultarPorPauta(pautaId));
    }
}


