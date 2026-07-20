package com.global.votacao.application.service;

import com.global.votacao.application.mapper.SessaoMapper;
import com.global.votacao.domain.model.StatusSessaoVotacao;
import com.global.votacao.application.dto.AtualizarPautaRequest;
import com.global.votacao.application.dto.CriarPautaRequest;
import com.global.votacao.application.dto.PautaResponse;
import com.global.votacao.application.dto.PautaSessaoAbertaResponse;
import com.global.votacao.application.dto.SessaoVotacaoResponse;
import com.global.votacao.domain.entity.PautaEntity;
import com.global.votacao.domain.entity.SessaoVotacaoEntity;
import com.global.votacao.infrastructure.persistence.repository.PautaRepository;
import com.global.votacao.infrastructure.persistence.repository.SessaoVotacaoRepository;
import com.global.votacao.infrastructure.persistence.repository.VotoRepository;
import com.global.votacao.application.mapper.PautaMapper;
import com.global.votacao.shared.exception.ConflitoException;
import com.global.votacao.shared.exception.RecursoNaoEncontradoException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PautaService {

    private static final Logger log = LoggerFactory.getLogger(PautaService.class);

    private final PautaRepository pautaRepository;
    private final SessaoVotacaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final PautaMapper pautaMapper;
    private final SessaoMapper sessaoMapper;

    @Transactional
    public PautaResponse criar(CriarPautaRequest request) {
        PautaEntity pautaEntity = pautaRepository.save(pautaMapper.toMapperPautaEntity(request));
        log.info("Pauta criada pautaId={}", pautaEntity.getId());
        return pautaMapper.toResponse(pautaEntity);
    }

    @Transactional(readOnly = true)
    public List<PautaResponse> listar() {
        return pautaRepository.findAll().stream().map(pautaMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PautaEntity buscarEntidade(Long pautaId) {
        return pautaRepository.findById(pautaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pauta nÃ£o encontrada para o id " + pautaId));
    }

    @Transactional(readOnly = true)
    public PautaResponse buscarPorId(Long pautaId) {
        return pautaMapper.toResponse(buscarEntidade(pautaId));
    }

    @Transactional(readOnly = true)
    public PautaSessaoAbertaResponse buscarComSessaoAberta(Long pautaId) {
        PautaEntity pautaEntity = buscarEntidade(pautaId);
        SessaoVotacaoEntity sessao = sessaoRepository.findByPautaEntityId(
                        pautaId
                )
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão aberta não encontrada para a pauta " + pautaId));
        return new PautaSessaoAbertaResponse(pautaMapper.toResponse(pautaEntity), sessaoMapper.toSessaoResponse(sessao));
    }

    @Transactional
    public PautaResponse atualizar(Long pautaId, AtualizarPautaRequest request) {
        validarPautaPodeSerAlterada(pautaId);
        PautaEntity pautaEntity = buscarEntidade(pautaId);
        pautaEntity.setTitulo(request.titulo());
        pautaEntity.setDescricao(request.descricao());
        return pautaMapper.toResponse(pautaRepository.save(pautaEntity));
    }

    @Transactional
    public void deletar(Long pautaId) {
        validarPautaPodeSerAlterada(pautaId);
        if (sessaoRepository.existsByPautaEntityId(pautaId)) {
            throw new ConflitoException("Pauta possui sessÃ£o vinculada; remova a sessÃ£o antes de remover a pauta");
        }
        PautaEntity pautaEntity = buscarEntidade(pautaId);
        pautaRepository.delete(pautaEntity);
    }

    private void validarPautaPodeSerAlterada(Long pautaId) {
        if (votoRepository.existsByPautaEntityId(pautaId)) {
            throw new ConflitoException("Pauta nÃ£o pode ser alterada ou removida porque jÃ¡ possui voto");
        }
        boolean possuiSessaoBloqueante = sessaoRepository.existsByPautaEntityIdAndStatusIn(
                pautaId,
                List.of(StatusSessaoVotacao.DISPONIVEL, StatusSessaoVotacao.ENCERRADA, StatusSessaoVotacao.PUBLICADA)
        );
        if (possuiSessaoBloqueante) {
            throw new ConflitoException("Pauta nÃ£o pode ser alterada ou removida porque a sessÃ£o jÃ¡ foi disponibilizada");
        }
    }

}


