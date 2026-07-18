package com.global.votacao.application.service;

import com.global.votacao.domain.model.StatusSessaoVotacao;
import com.global.votacao.domain.model.TipoVoto;
import com.global.votacao.domain.model.VencedorVotacao;
import com.global.votacao.application.dto.ResultadoVotacaoResponse;
import com.global.votacao.domain.entity.PautaEntity;
import com.global.votacao.domain.entity.SessaoVotacaoEntity;
import com.global.votacao.infrastructure.persistence.repository.SessaoVotacaoRepository;
import com.global.votacao.infrastructure.persistence.repository.VotoRepository;
import com.global.votacao.shared.exception.RecursoNaoEncontradoException;
import com.global.votacao.domain.model.StatusResultado;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultadoVotacaoService {

    private final PautaService pautaService;
    private final SessaoVotacaoRepository sessaoRepository;
    private final VotoRepository votoRepository;

    public ResultadoVotacaoService(PautaService pautaService, SessaoVotacaoRepository sessaoRepository, VotoRepository votoRepository) {
        this.pautaService = pautaService;
        this.sessaoRepository = sessaoRepository;
        this.votoRepository = votoRepository;
    }

    @Transactional(readOnly = true)
    public ResultadoVotacaoResponse consultarPorPauta(Long pautaId) {
        PautaEntity pautaEntity = pautaService.buscarEntidade(pautaId);
        SessaoVotacaoEntity sessao = sessaoRepository.findByPautaEntityId(pautaEntity.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("SessÃ£o nÃ£o encontrada para a pauta " + pautaId));
        return calcular(pautaEntity.getId(), sessao);
    }

    @Transactional(readOnly = true)
    public ResultadoVotacaoResponse calcular(Long pautaId, SessaoVotacaoEntity sessao) {
        long votosSim = votoRepository.countByPautaEntityIdAndTipo(pautaId, TipoVoto.SIM);
        long votosNao = votoRepository.countByPautaEntityIdAndTipo(pautaId, TipoVoto.NAO);
        return montarResultado(pautaId, sessao, votosSim, votosNao);
    }

    private ResultadoVotacaoResponse montarResultado(Long pautaId, SessaoVotacaoEntity sessao, long votosSim, long votosNao) {
        long total = votosSim + votosNao;
        VencedorVotacao vencedor = vencedor(votosSim, votosNao);
        StatusResultado status = statusResultado(sessao);
        return new ResultadoVotacaoResponse(pautaId, votosSim, votosNao, total, status, vencedor);
    }

    private VencedorVotacao vencedor(long votosSim, long votosNao) {
        if (votosSim > votosNao) {
            return VencedorVotacao.SIM;
        }
        if (votosNao > votosSim) {
            return VencedorVotacao.NAO;
        }
        return VencedorVotacao.EMPATE;
    }

    private StatusResultado statusResultado(SessaoVotacaoEntity sessao) {
        if (sessao.getStatus() == StatusSessaoVotacao.CRIADA) {
            return StatusResultado.SEM_SESSAO;
        }
        if (sessao.getStatus() == StatusSessaoVotacao.DISPONIVEL && LocalDateTime.now().isBefore(sessao.getFechaEm())) {
            return StatusResultado.SESSAO_ABERTA;
        }
        return StatusResultado.SESSAO_ENCERRADA;
    }
}


