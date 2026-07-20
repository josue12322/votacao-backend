package com.global.votacao.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.global.votacao.application.config.FinalizacaoProperties;
import com.global.votacao.application.dto.ResultadoVotacaoResponse;
import com.global.votacao.application.dto.SessaoVotacaoResponse;
import com.global.votacao.application.event.ResultadoVotacaoEvento;
import com.global.votacao.application.mapper.SessaoMapper;
import com.global.votacao.application.port.out.ResultadoVotacaoPublisher;
import com.global.votacao.domain.entity.PautaEntity;
import com.global.votacao.domain.entity.SessaoVotacaoEntity;
import com.global.votacao.domain.model.StatusResultado;
import com.global.votacao.domain.model.StatusSessaoVotacao;
import com.global.votacao.domain.model.VencedorVotacao;
import com.global.votacao.infrastructure.persistence.repository.SessaoVotacaoRepository;
import com.global.votacao.shared.exception.RecursoNaoEncontradoException;
import com.global.votacao.shared.exception.RegraNegocioException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallback;

@ExtendWith(MockitoExtension.class)
class FinalizacaoSessaoServiceTest {

    @Mock
    private SessaoVotacaoRepository sessaoRepository;

    @Mock
    private ResultadoVotacaoService resultadoService;

    @Mock
    private ResultadoVotacaoPublisher publisher;

    @Mock
    private TransactionTemplate transactionTemplate;

    private FinalizacaoSessaoService finalizacaoService;

    @BeforeEach
    void setUp() {
        finalizacaoService = new FinalizacaoSessaoService(
                sessaoRepository,
                resultadoService,
                publisher,
                new FinalizacaoProperties(true, 2),
                transactionTemplate,
                new SessaoMapper()
        );
    }

    @Test
    void deveFinalizarPendentesComSucesso() {
        PautaEntity pauta = pauta(1L);
        SessaoVotacaoEntity sessao = sessao(10L, pauta, StatusSessaoVotacao.DISPONIVEL, LocalDateTime.now().minusSeconds(10));
        when(sessaoRepository.findByStatusAndFechaEmLessThanEqualOrderByFechaEmAsc(eq(StatusSessaoVotacao.DISPONIVEL), any(), any()))
                .thenReturn(List.of(sessao));
        when(sessaoRepository.findByStatusAndResultadoPublicadoFalseOrderByFechaEmAsc(eq(StatusSessaoVotacao.ENCERRADA), any()))
                .thenReturn(List.of());
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(sessaoRepository.findById(10L)).thenReturn(Optional.of(sessao));
        when(resultadoService.calcular(1L, sessao)).thenReturn(resultado());

        int processadas = finalizacaoService.finalizarPendentes();

        assertThat(processadas).isEqualTo(1);
        assertThat(sessao.getStatus()).isEqualTo(StatusSessaoVotacao.PUBLICADA);
        assertThat(sessao.isResultadoPublicado()).isTrue();
        verify(publisher).publicar(any(ResultadoVotacaoEvento.class));
        verify(sessaoRepository, times(2)).save(sessao);
    }

    @Test
    void deveRetornarZeroQuandoNaoExistiremSessoesPendentes() {
        when(sessaoRepository.findByStatusAndFechaEmLessThanEqualOrderByFechaEmAsc(eq(StatusSessaoVotacao.DISPONIVEL), any(), any()))
                .thenReturn(List.of());
        when(sessaoRepository.findByStatusAndResultadoPublicadoFalseOrderByFechaEmAsc(eq(StatusSessaoVotacao.ENCERRADA), any()))
                .thenReturn(List.of());

        int processadas = finalizacaoService.finalizarPendentes();

        assertThat(processadas).isZero();
        verify(transactionTemplate, never()).execute(any());
    }

    @Test
    void deveManterSessaoEncerradaNaoPublicadaQuandoKafkaFalhar() {
        PautaEntity pauta = pauta(1L);
        SessaoVotacaoEntity sessao = sessao(10L, pauta, StatusSessaoVotacao.DISPONIVEL, LocalDateTime.now().minusSeconds(10));
        when(sessaoRepository.findByStatusAndFechaEmLessThanEqualOrderByFechaEmAsc(eq(StatusSessaoVotacao.DISPONIVEL), any(), any()))
                .thenReturn(List.of(sessao));
        when(sessaoRepository.findByStatusAndResultadoPublicadoFalseOrderByFechaEmAsc(eq(StatusSessaoVotacao.ENCERRADA), any()))
                .thenReturn(List.of());
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(sessaoRepository.findById(10L)).thenReturn(Optional.of(sessao));
        when(resultadoService.calcular(1L, sessao)).thenReturn(resultado());
        doThrow(new RuntimeException("Kafka indisponível")).when(publisher).publicar(any(ResultadoVotacaoEvento.class));

        int processadas = finalizacaoService.finalizarPendentes();

        assertThat(processadas).isEqualTo(1);
        assertThat(sessao.getStatus()).isEqualTo(StatusSessaoVotacao.ENCERRADA);
        assertThat(sessao.isResultadoPublicado()).isFalse();
    }

    @Test
    void deveEncerrarForcadoSessaoDisponivelComSucesso() {
        PautaEntity pauta = pauta(1L);
        SessaoVotacaoEntity sessao = sessao(10L, pauta, StatusSessaoVotacao.DISPONIVEL, LocalDateTime.now().plusMinutes(5));
        when(sessaoRepository.findById(10L)).thenReturn(Optional.of(sessao));
        when(resultadoService.calcular(1L, sessao)).thenReturn(resultado());

        SessaoVotacaoResponse response = finalizacaoService.encerrarForcado(10L);

        assertThat(response.status()).isEqualTo(StatusSessaoVotacao.PUBLICADA);
        assertThat(sessao.isResultadoPublicado()).isTrue();
        ArgumentCaptor<ResultadoVotacaoEvento> captor = ArgumentCaptor.forClass(ResultadoVotacaoEvento.class);
        verify(publisher).publicar(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(1L);
    }

    @Test
    void deveBloquearEncerramentoForcadoQuandoSessaoEstiverCriada() {
        PautaEntity pauta = pauta(1L);
        when(sessaoRepository.findById(10L)).thenReturn(Optional.of(sessao(10L, pauta, StatusSessaoVotacao.CRIADA, LocalDateTime.now().plusMinutes(5))));

        assertThatThrownBy(() -> finalizacaoService.encerrarForcado(10L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("disponibilizada");
    }

    @Test
    void deveLancarErroQuandoSessaoForcadaNaoExistir() {
        when(sessaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> finalizacaoService.encerrarForcado(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    private ResultadoVotacaoResponse resultado() {
        return new ResultadoVotacaoResponse(1L, 2L, 1L, 3L, StatusResultado.SESSAO_ENCERRADA, VencedorVotacao.SIM);
    }

    private PautaEntity pauta(Long id) {
        return PautaEntity.builder()
                .id(id)
                .titulo("Pauta " + id)
                .descricao("Descrição")
                .criadaEm(LocalDateTime.now())
                .build();
    }

    private SessaoVotacaoEntity sessao(Long id, PautaEntity pauta, StatusSessaoVotacao status, LocalDateTime fechaEm) {
        return SessaoVotacaoEntity.builder()
                .id(id)
                .pautaEntity(pauta)
                .abertaEm(LocalDateTime.now().minusMinutes(1))
                .fechaEm(fechaEm)
                .status(status)
                .resultadoPublicado(false)
                .build();
    }
}
