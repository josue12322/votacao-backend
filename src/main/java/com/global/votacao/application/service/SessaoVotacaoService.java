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
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class SessaoVotacaoService {

    private static final Logger log = LoggerFactory.getLogger(SessaoVotacaoService.class);

    private final PautaService pautaService;
    private final SessaoVotacaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final SessaoMapper sessaoMapper;

    @Transactional
    public SessaoVotacaoResponse criar(Long pautaId, CriarSessaoRequest request) {
        return criar(pautaId, request == null ? 60 : request.duracaoOuDefault());
    }

    @Transactional
    public SessaoVotacaoResponse criar(Long pautaId, Integer duracaoEmSegundos) {
        if (duracaoEmSegundos == null || duracaoEmSegundos <= 0) {
            throw new IllegalArgumentException("DuraÃ§Ã£o da sessÃ£o deve ser maior que zero");
        }
        PautaEntity pautaEntity = pautaService.buscarEntidade(pautaId);
        if (sessaoRepository.existsByPautaEntityId(pautaId)) {
            throw new ConflitoException("Pauta jÃ¡ possui sessÃ£o de votaÃ§Ã£o");
        }
        SessaoVotacaoEntity sessao = sessaoRepository.save(sessaoMapper.toMapperSessaoEntity(pautaEntity, duracaoEmSegundos));
        log.info("Sessão criada sessaoId={} pautaId={} status={}", sessao.getId(), pautaId, sessao.getStatus());
        return toResponse(sessao);
    }

    @Transactional(readOnly = true)
    public List<SessaoVotacaoResponse> listar() {
        return sessaoRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SessaoVotacaoResponse buscarPorId(Long sessaoId) {
        return toResponse(buscarEntidade(sessaoId));
    }

    @Transactional
    public SessaoVotacaoResponse atualizar(Long sessaoId, AtualizarSessaoRequest request) {
        SessaoVotacaoEntity sessao = buscarEntidade(sessaoId);
        validarSessaoPodeSerAlterada(sessao);
        sessao.atualizarDuracao(request.duracaoEmSegundos());
        return toResponse(sessaoRepository.save(sessao));
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
            throw new ConflitoException("SessÃ£o sÃ³ pode ser disponibilizada quando estiver criada");
        }
        sessao.disponibilizar(LocalDateTime.now());
        log.info("Sessão disponibilizada para votação sessaoId={} pautaId={}", sessao.getId(), sessao.getPautaEntity().getId());
        return toResponse(sessaoRepository.save(sessao));
    }

    @Transactional
    public SessaoVotacaoResponse encerrar(Long sessaoId) {
        SessaoVotacaoEntity sessao = buscarEntidade(sessaoId);
        if (sessao.getStatus() == StatusSessaoVotacao.PUBLICADA || sessao.getStatus() == StatusSessaoVotacao.ENCERRADA) {
            return toResponse(sessao);
        }
        if (sessao.getStatus() == StatusSessaoVotacao.CRIADA) {
            throw new RegraNegocioException("SessÃ£o criada e ainda nÃ£o disponibilizada nÃ£o pode ser encerrada como votaÃ§Ã£o");
        }
        sessao.encerrar(LocalDateTime.now());
        return toResponse(sessaoRepository.save(sessao));
    }

    SessaoVotacaoEntity buscarEntidade(Long sessaoId) {
        return sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("SessÃ£o nÃ£o encontrada para o id " + sessaoId));
    }

    private void validarSessaoPodeSerAlterada(SessaoVotacaoEntity sessao) {
        if (!sessao.podeSerAlterada()) {
            throw new ConflitoException("SessÃ£o nÃ£o pode ser alterada ou removida apÃ³s ser disponibilizada para votaÃ§Ã£o");
        }
        if (votoRepository.existsByPautaEntityId(sessao.getPautaEntity().getId())) {
            throw new ConflitoException("SessÃ£o nÃ£o pode ser alterada ou removida porque jÃ¡ possui voto");
        }
    }

    SessaoVotacaoResponse toResponse(SessaoVotacaoEntity sessao) {
        return new SessaoVotacaoResponse(
                sessao.getId(),
                sessao.getPautaEntity().getId(),
                sessao.getAbertaEm(),
                sessao.getFechaEm(),
                sessao.getEncerradaEm(),
                sessao.getStatus()
        );
    }
}


