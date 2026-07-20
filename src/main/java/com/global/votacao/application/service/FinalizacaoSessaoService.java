package com.global.votacao.application.service;

import com.global.votacao.application.config.FinalizacaoProperties;
import com.global.votacao.application.dto.ResultadoVotacaoResponse;
import com.global.votacao.application.dto.SessaoVotacaoResponse;
import com.global.votacao.application.event.ResultadoVotacaoEvento;
import com.global.votacao.application.mapper.SessaoMapper;
import com.global.votacao.application.port.out.ResultadoVotacaoPublisher;
import com.global.votacao.domain.entity.SessaoVotacaoEntity;
import com.global.votacao.domain.model.StatusSessaoVotacao;
import com.global.votacao.infrastructure.persistence.repository.SessaoVotacaoRepository;
import com.global.votacao.shared.exception.RecursoNaoEncontradoException;
import com.global.votacao.shared.exception.RegraNegocioException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@AllArgsConstructor
public class FinalizacaoSessaoService {

    private static final Logger log = LoggerFactory.getLogger(FinalizacaoSessaoService.class);
    private static final int LIMITE_CONSULTA_BATCH = 10_000;

    private final SessaoVotacaoRepository sessaoRepository;
    private final ResultadoVotacaoService resultadoService;
    private final ResultadoVotacaoPublisher publisher;
    private final FinalizacaoProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final SessaoMapper sessaoMapper;


    public int finalizarPendentes() {
        LocalDateTime dtAtual = LocalDateTime.now();
        List<SessaoVotacaoEntity> sessoesDisponiveisVencidas = sessaoRepository.findByStatusAndFechaEmLessThanEqualOrderByFechaEmAsc(
                StatusSessaoVotacao.DISPONIVEL,
                dtAtual,
                PageRequest.of(0, LIMITE_CONSULTA_BATCH)
        );
        List<SessaoVotacaoEntity> sessoesEncerradasNaoPublicadas = sessaoRepository.findByStatusAndResultadoPublicadoFalseOrderByFechaEmAsc(
                StatusSessaoVotacao.ENCERRADA,
                PageRequest.of(0, LIMITE_CONSULTA_BATCH)
        );

        List<Long> sessoesIds = java.util.stream.Stream.concat(
                        sessoesDisponiveisVencidas.stream(),
                        sessoesEncerradasNaoPublicadas.stream()
                )
                .map(SessaoVotacaoEntity::getId)
                .toList();

        int processadas = finalizarSessoes(sessoesIds, dtAtual);

        log.info(
                "Finalizações de sessões concluidas sessoesDisponiveisVencidas={} sessoesEncerradasNaoPublicadas={} poolSize={} limiteConsultaBatch={} sessoesProcessadas={}",
                sessoesDisponiveisVencidas.size(),
                sessoesEncerradasNaoPublicadas.size(),
                properties.poolSize(),
                LIMITE_CONSULTA_BATCH,
                processadas
        );
        return processadas;
    }

    private int finalizarSessoes(List<Long> sessoesIds, LocalDateTime dtAtual) {
        if (sessoesIds.isEmpty()) {
            return 0;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(properties.poolSize());
        try {
            List<Callable<Boolean>> tarefas = sessoesIds.stream()
                    .map(sessaoId -> (Callable<Boolean>) () -> finalizarUmaSessaoEmTransacao(sessaoId, dtAtual))
                    .toList();
            int processadas = 0;
            for (Future<Boolean> resultado : executorService.invokeAll(tarefas)) {
                try {
                    if (Boolean.TRUE.equals(resultado.get())) {
                        processadas++;
                    }
                } catch (ExecutionException exception) {
                    log.warn("Falha ao finalizar sessão pendente", exception);
                }
            }
            return processadas;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Finalização de sessões interrompida", exception);
            return 0;
        } finally {
            executorService.shutdown();
        }
    }
    private boolean finalizarUmaSessaoEmTransacao(Long sessaoId, LocalDateTime dtAtual) {
        Boolean finalizada = transactionTemplate.execute(status -> finalizarUmaSessao(sessaoId, dtAtual));
        return Boolean.TRUE.equals(finalizada);
    }
    boolean finalizarUmaSessao(Long sessaoId, LocalDateTime dtAtual) {
        SessaoVotacaoEntity sessao = sessaoRepository.findById(sessaoId).orElse(null);
        if (sessao == null || sessao.getStatus() == StatusSessaoVotacao.PUBLICADA || sessao.isResultadoPublicado()) {
            return false;
        }
        if (sessao.getStatus() == StatusSessaoVotacao.DISPONIVEL && !sessao.estaVencida(dtAtual)) {
            return false;
        }
        if (sessao.getStatus() == StatusSessaoVotacao.DISPONIVEL) {
            sessao.encerrar(dtAtual);
            sessaoRepository.save(sessao);
        } else if (sessao.getStatus() != StatusSessaoVotacao.ENCERRADA) {
            return false;
        }
        ResultadoVotacaoResponse resultado = resultadoService.calcular(sessao.getPautaEntity().getId(), sessao);
        ResultadoVotacaoEvento evento = new ResultadoVotacaoEvento(
                sessao.getPautaEntity().getId(),
                sessao.getPautaEntity().getTitulo(),
                sessao.getPautaEntity().getDescricao(),
                resultado
        );
        try {
            publisher.publicar(evento);
            sessao.marcarPublicada();
            sessaoRepository.save(sessao);
            log.info("Resultado publicado pautaId={} sessaoId={}", sessao.getPautaEntity().getId(), sessao.getId());
        } catch (RuntimeException exception) {
            log.warn("Falha ao publicar resultado pautaId={} sessaoId={}", sessao.getPautaEntity().getId(), sessao.getId(), exception);
        }
        return true;
    }

    @Transactional
    public SessaoVotacaoResponse encerrarForcado(Long sessaoId) {
        SessaoVotacaoEntity sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão não encontrada para o id " + sessaoId));
        if (sessao.getStatus() == StatusSessaoVotacao.CRIADA) {
            throw new RegraNegocioException("Sessão não e ainda não disponibilizada não pode ser encerrada como votação");
        }
        finalizarUmaSessaoForcada(sessao, LocalDateTime.now());
        SessaoVotacaoEntity atualizada = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão não encontrada para o id " + sessaoId));
        return sessaoMapper.toSessaoResponse(atualizada);
    }

    private void finalizarUmaSessaoForcada(SessaoVotacaoEntity sessao, LocalDateTime dtAtual) {
        if (sessao.getStatus() == StatusSessaoVotacao.PUBLICADA || sessao.isResultadoPublicado()) {
            return;
        }
        if (sessao.getStatus() == StatusSessaoVotacao.DISPONIVEL) {
            sessao.encerrar(dtAtual);
            sessaoRepository.save(sessao);
        }
        if (sessao.getStatus() == StatusSessaoVotacao.ENCERRADA) {
            ResultadoVotacaoResponse resultado = resultadoService.calcular(sessao.getPautaEntity().getId(), sessao);
            ResultadoVotacaoEvento evento = new ResultadoVotacaoEvento(
                    sessao.getPautaEntity().getId(),
                    sessao.getPautaEntity().getTitulo(),
                    sessao.getPautaEntity().getDescricao(),
                    resultado
            );
            publisher.publicar(evento);
            sessao.marcarPublicada();
            sessaoRepository.save(sessao);
        }
    }

    private SessaoVotacaoResponse toResponse(SessaoVotacaoEntity sessao) {
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



