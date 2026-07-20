package com.global.votacao.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.global.votacao.application.dto.ResultadoVotacaoResponse;
import com.global.votacao.domain.entity.PautaEntity;
import com.global.votacao.domain.entity.SessaoVotacaoEntity;
import com.global.votacao.domain.model.StatusResultado;
import com.global.votacao.domain.model.StatusSessaoVotacao;
import com.global.votacao.domain.model.TipoVoto;
import com.global.votacao.domain.model.VencedorVotacao;
import com.global.votacao.infrastructure.persistence.repository.SessaoVotacaoRepository;
import com.global.votacao.infrastructure.persistence.repository.VotoRepository;
import com.global.votacao.shared.exception.RecursoNaoEncontradoException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResultadoVotacaoServiceTest {

    @Mock
    private PautaService pautaService;

    @Mock
    private SessaoVotacaoRepository sessaoRepository;

    @Mock
    private VotoRepository votoRepository;

    private ResultadoVotacaoService resultadoService;

    @BeforeEach
    void setUp() {
        resultadoService = new ResultadoVotacaoService(pautaService, sessaoRepository, votoRepository);
    }

    @Test
    void deveConsultarResultadoComVencedorSimComSucesso() {
        PautaEntity pauta = pauta(1L);
        SessaoVotacaoEntity sessao = sessao(pauta, StatusSessaoVotacao.ENCERRADA);
        when(pautaService.buscarEntidade(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaEntityId(1L)).thenReturn(Optional.of(sessao));
        when(votoRepository.countByPautaEntityIdAndTipo(1L, TipoVoto.SIM)).thenReturn(10L);
        when(votoRepository.countByPautaEntityIdAndTipo(1L, TipoVoto.NAO)).thenReturn(4L);

        ResultadoVotacaoResponse response = resultadoService.consultarPorPauta(1L);

        assertThat(response.votosSim()).isEqualTo(10L);
        assertThat(response.votosNao()).isEqualTo(4L);
        assertThat(response.totalVotos()).isEqualTo(14L);
        assertThat(response.vencedor()).isEqualTo(VencedorVotacao.SIM);
        assertThat(response.status()).isEqualTo(StatusResultado.SESSAO_ENCERRADA);
    }

    @Test
    void deveCalcularResultadoComVencedorNao() {
        PautaEntity pauta = pauta(1L);
        SessaoVotacaoEntity sessao = sessao(pauta, StatusSessaoVotacao.ENCERRADA);
        when(votoRepository.countByPautaEntityIdAndTipo(1L, TipoVoto.SIM)).thenReturn(3L);
        when(votoRepository.countByPautaEntityIdAndTipo(1L, TipoVoto.NAO)).thenReturn(9L);

        ResultadoVotacaoResponse response = resultadoService.calcular(1L, sessao);

        assertThat(response.vencedor()).isEqualTo(VencedorVotacao.NAO);
        assertThat(response.totalVotos()).isEqualTo(12L);
    }

    @Test
    void deveCalcularResultadoEmpatado() {
        PautaEntity pauta = pauta(1L);
        SessaoVotacaoEntity sessao = sessao(pauta, StatusSessaoVotacao.ENCERRADA);
        when(votoRepository.countByPautaEntityIdAndTipo(1L, TipoVoto.SIM)).thenReturn(5L);
        when(votoRepository.countByPautaEntityIdAndTipo(1L, TipoVoto.NAO)).thenReturn(5L);

        ResultadoVotacaoResponse response = resultadoService.calcular(1L, sessao);

        assertThat(response.vencedor()).isEqualTo(VencedorVotacao.EMPATE);
        assertThat(response.totalVotos()).isEqualTo(10L);
    }

    @Test
    void deveRetornarStatusSessaoAbertaQuandoSessaoDisponivelAindaNaoVenceu() {
        PautaEntity pauta = pauta(1L);
        SessaoVotacaoEntity sessao = sessao(pauta, StatusSessaoVotacao.DISPONIVEL);

        ResultadoVotacaoResponse response = resultadoService.calcular(1L, sessao);

        assertThat(response.status()).isEqualTo(StatusResultado.SESSAO_ABERTA);
    }

    @Test
    void deveRetornarStatusSemSessaoQuandoSessaoAindaEstiverCriada() {
        PautaEntity pauta = pauta(1L);
        SessaoVotacaoEntity sessao = sessao(pauta, StatusSessaoVotacao.CRIADA);

        ResultadoVotacaoResponse response = resultadoService.calcular(1L, sessao);

        assertThat(response.status()).isEqualTo(StatusResultado.SEM_SESSAO);
    }

    @Test
    void deveLancarErroQuandoSessaoNaoExistirParaResultado() {
        PautaEntity pauta = pauta(1L);
        when(pautaService.buscarEntidade(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaEntityId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultadoService.consultarPorPauta(1L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("1");
    }

    private PautaEntity pauta(Long id) {
        return PautaEntity.builder()
                .id(id)
                .titulo("Pauta " + id)
                .descricao("Descrição")
                .criadaEm(LocalDateTime.now())
                .build();
    }

    private SessaoVotacaoEntity sessao(PautaEntity pauta, StatusSessaoVotacao status) {
        LocalDateTime abertaEm = LocalDateTime.now().minusMinutes(1);
        return SessaoVotacaoEntity.builder()
                .id(10L)
                .pautaEntity(pauta)
                .abertaEm(abertaEm)
                .fechaEm(LocalDateTime.now().plusMinutes(5))
                .status(status)
                .resultadoPublicado(false)
                .build();
    }
}
