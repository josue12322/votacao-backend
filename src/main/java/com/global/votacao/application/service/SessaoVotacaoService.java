package com.global.votacao.application.service;

import com.global.votacao.domain.model.StatusSessaoVotacao;
import com.global.votacao.application.dto.AtualizarSessaoRequest;
import com.global.votacao.application.dto.CriarSessaoRequest;
import com.global.votacao.application.dto.SessaoVotacaoResponse;
import com.global.votacao.domain.entity.PautaEntity;
import com.global.votacao.domain.entity.SessaoVotacaoEntity;
import com.global.votacao.infrastructure.persistence.repository.SessaoVotacaoRepository;
import com.global.votacao.infrastructure.persistence.repository.VotoRepository;
import com.global.votacao.application.mapper.SessaoMapper;
import com.global.votacao.shared.exception.ConflitoException;
import com.global.votacao.shared.exception.RecursoNaoEncontradoException;
import com.global.votacao.shared.exception.RegraNegocioException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessaoVotacaoService {

    private static final Logger log = LoggerFactory.getLogger(SessaoVotacaoService.class);

    private final PautaService pautaService;
    private final SessaoVotacaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final SessaoMapper sessaoMapper;

    @Value("${votacao.duracao-voto}")
    private int defaultDuracao;

    @Transactional
    public SessaoVotacaoResponse criar(Long pautaId, CriarSessaoRequest request) {
        Integer duracaoEmSegundos = request == null || request.duracaoEmSegundos() == null
                ? defaultDuracao
                : request.duracaoEmSegundos();
        return criar(pautaId, duracaoEmSegundos);
    }

    @Transactional
    public SessaoVotacaoResponse criar(Long pautaId, Integer duracaoEmSegundos) {
        if (duracaoEmSegundos == null || duracaoEmSegundos <= 0) {
            throw new IllegalArgumentException("Duração da sessão deve ser maior que zero");
        }
        PautaEntity pautaEntity = pautaService.buscarEntidade(pautaId);
        if (sessaoRepository.existsByPautaEntityId(pautaId)) {
            throw new ConflitoException("Pauta já possui sessão de votação; a regra é 1 sessão para 1 pauta, independente do status da sessão");
        }
        SessaoVotacaoEntity sessao = sessaoRepository.save(sessaoMapper.toMapperSessaoEntity(pautaEntity, duracaoEmSegundos));
        log.info("Sessão criada sessaoId={} pautaId={} status={}", sessao.getId(), pautaId, sessao.getStatus());
        return sessaoMapper.toSessaoResponse(sessao);
    }

    @Transactional(readOnly = true)
    public List<SessaoVotacaoResponse> listar() {
        return sessaoRepository.findAll().stream().map(sessaoMapper::toSessaoResponse).toList();
    }

    @Transactional(readOnly = true)
    public SessaoVotacaoResponse buscarPorId(Long sessaoId) {
        return sessaoMapper.toSessaoResponse(buscarEntidade(sessaoId));
    }

    @Transactional(readOnly = true)
    public SessaoVotacaoResponse buscarPorPautaId(Long pautaId) {
        pautaService.buscarEntidade(pautaId);
        SessaoVotacaoEntity sessao = sessaoRepository.findByPautaEntityId(pautaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão não encontrada para a pauta " + pautaId));
        return sessaoMapper.toSessaoResponse(sessao);
    }

    @Transactional
    public SessaoVotacaoResponse atualizar(Long sessaoId, AtualizarSessaoRequest request) {
        SessaoVotacaoEntity sessao = buscarEntidade(sessaoId);
        validarSessaoPodeSerAlterada(sessao);
        sessao.atualizarDuracao(request.duracaoEmSegundos());
        return sessaoMapper.toSessaoResponse(sessaoRepository.save(sessao));
    }

    @Transactional
    public void deletar(Long sessaoId) {
        SessaoVotacaoEntity sessao = buscarEntidade(sessaoId);
        validarSessaoPodeSerAlterada(sessao);
        sessaoRepository.delete(sessao);
    }

    @Transactional
    public SessaoVotacaoResponse disponibilizar(Long sessaoId) {
        SessaoVotacaoEntity sessao = buscarEntidade(sessaoId);
        if (sessao.getStatus() != StatusSessaoVotacao.CRIADA) {
            throw new ConflitoException("Sessão só pode ser disponibilizada quando estiver criada");
        }
        sessao.disponibilizar(LocalDateTime.now());
        log.info("Sessão disponibilizada para votação sessaoId={} pautaId={}", sessao.getId(), sessao.getPautaEntity().getId());
        return sessaoMapper.toSessaoResponse(sessaoRepository.save(sessao));
    }

    @Transactional
    public SessaoVotacaoResponse encerrar(Long sessaoId) {
        SessaoVotacaoEntity sessao = buscarEntidade(sessaoId);
        if (sessao.getStatus() == StatusSessaoVotacao.PUBLICADA || sessao.getStatus() == StatusSessaoVotacao.ENCERRADA) {
            return sessaoMapper.toSessaoResponse(sessao);
        }
        if (sessao.getStatus() == StatusSessaoVotacao.CRIADA) {
            throw new RegraNegocioException("Sessão criada e ainda não disponibilizada não pode ser encerrada como votação");
        }
        sessao.encerrar(LocalDateTime.now());
        return sessaoMapper.toSessaoResponse(sessaoRepository.save(sessao));
    }

    SessaoVotacaoEntity buscarEntidade(Long sessaoId) {
        return sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão não encontrada para o id " + sessaoId));
    }

    private void validarSessaoPodeSerAlterada(SessaoVotacaoEntity sessao) {
        if (!sessao.podeSerAlterada()) {
            throw new ConflitoException("Sessão não pode ser alterada ou removida após ser disponibilizada para votação");
        }
        if (votoRepository.existsByPautaEntityId(sessao.getPautaEntity().getId())) {
            throw new ConflitoException("Sessão não pode ser alterada ou removida porque já possui voto");
        }
    }


}
