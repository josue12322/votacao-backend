package com.global.votacao.adapter.in.web.rest;

import com.global.votacao.adapter.in.web.api.ISessaoVotacaoController;
import com.global.votacao.application.dto.AtualizarSessaoRequest;
import com.global.votacao.application.dto.CriarSessaoRequest;
import com.global.votacao.application.dto.FinalizacaoSessoesResponse;
import com.global.votacao.application.dto.SessaoVotacaoResponse;
import com.global.votacao.application.service.FinalizacaoSessaoService;
import com.global.votacao.application.service.SessaoVotacaoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SessaoVotacaoController implements ISessaoVotacaoController {

    private final SessaoVotacaoService sessaoService;
    private final FinalizacaoSessaoService finalizacaoSessaoService;

    @Override
    public ResponseEntity<SessaoVotacaoResponse> criar(Long pautaId, CriarSessaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessaoService.criar(pautaId, request));
    }

    @Override
    public ResponseEntity<List<SessaoVotacaoResponse>> listar() {
        return ResponseEntity.ok(sessaoService.listar());
    }

    @Override
    public ResponseEntity<SessaoVotacaoResponse> buscarPorId(Long sessaoId) {
        return ResponseEntity.ok(sessaoService.buscarPorId(sessaoId));
    }

    @Override
    public ResponseEntity<SessaoVotacaoResponse> atualizar(Long sessaoId, AtualizarSessaoRequest request) {
        return ResponseEntity.ok(sessaoService.atualizar(sessaoId, request));
    }

    @Override
    public ResponseEntity<Void> deletar(Long sessaoId) {
        sessaoService.deletar(sessaoId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SessaoVotacaoResponse> disponibilizar(Long sessaoId) {
        return ResponseEntity.ok(sessaoService.disponibilizar(sessaoId));
    }

    @Override
    public ResponseEntity<SessaoVotacaoResponse> encerrar(Long sessaoId) {
        return ResponseEntity.ok(finalizacaoSessaoService.encerrarForcado(sessaoId));
    }

    @Override
    public ResponseEntity<FinalizacaoSessoesResponse> finalizar() {
        return ResponseEntity.ok(new FinalizacaoSessoesResponse(finalizacaoSessaoService.finalizarPendentes()));
    }
}


