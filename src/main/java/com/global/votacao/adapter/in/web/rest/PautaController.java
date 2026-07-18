package com.global.votacao.adapter.in.web.rest;

import com.global.votacao.adapter.in.web.api.IPautaController;
import com.global.votacao.application.dto.AtualizarPautaRequest;
import com.global.votacao.application.dto.CriarPautaRequest;
import com.global.votacao.application.dto.PautaResponse;
import com.global.votacao.application.service.PautaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PautaController implements IPautaController {

    private final PautaService pautaService;

    @Override
    public ResponseEntity<PautaResponse> criar(CriarPautaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pautaService.criar(request));
    }

    @Override
    public ResponseEntity<List<PautaResponse>> listar() {
        return ResponseEntity.ok(pautaService.listar());
    }

    @Override
    public ResponseEntity<PautaResponse> buscarPorId(Long pautaId) {
        return ResponseEntity.ok(pautaService.buscarPorId(pautaId));
    }

    @Override
    public ResponseEntity<PautaResponse> atualizar(Long pautaId, AtualizarPautaRequest request) {
        return ResponseEntity.ok(pautaService.atualizar(pautaId, request));
    }

    @Override
    public ResponseEntity<Void> deletar(Long pautaId) {
        pautaService.deletar(pautaId);
        return ResponseEntity.noContent().build();
    }
}


