package com.global.votacao.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.global.votacao.application.dto.AtualizarSessaoRequest;
import com.global.votacao.application.dto.CriarSessaoRequest;
import com.global.votacao.application.dto.SessaoVotacaoResponse;
import com.global.votacao.application.mapper.SessaoMapper;
import com.global.votacao.domain.entity.PautaEntity;
import com.global.votacao.domain.entity.SessaoVotacaoEntity;
import com.global.votacao.domain.model.StatusSessaoVotacao;
import com.global.votacao.infrastructure.persistence.repository.SessaoVotacaoRepository;
import com.global.votacao.infrastructure.persistence.repository.VotoRepository;
import com.global.votacao.shared.exception.ConflitoException;
import com.global.votacao.shared.exception.RecursoNaoEncontradoException;
import com.global.votacao.shared.exception.RegraNegocioException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SessaoVotacaoServiceTest {

    @Mock
    private PautaService pautaService;

    @Mock
    private SessaoVotacaoRepository sessaoRepository;

    @Mock
    private VotoRepository votoRepository;

    private SessaoVotacaoService sessaoService;

    @BeforeEach
    void setUp() {
        sessaoService = new SessaoVotacaoService(pautaService, sessaoRepository, votoRepository, new SessaoMapper());
        ReflectionTestUtils.setField(sessaoService, "defaultDuracao", 90);
    }

    @Test
    void deveCriarSessaoComDuracaoInformadaComSucesso() {
        PautaEntity pauta = pauta(1L);
        when(pautaService.buscarEntidade(1L)).thenReturn(pauta);
        when(sessaoRepository.existsByPautaEntityId(1L)).thenReturn(false);
        when(sessaoRepository.save(any(SessaoVotacaoEntity.class))).thenAnswer(invocation -> {
            SessaoVotacaoEntity sessao = invocation.getArgument(0);
            sessao.setId(10L);
            return sessao;
        });

        SessaoVotacaoResponse response = sessaoService.criar(1L, new CriarSessaoRequest(120));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.pautaId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(StatusSessaoVotacao.CRIADA);
        assertThat(response.fechaEm()).isEqualTo(response.abertaEm().plusSeconds(120));
    }

    @Test
    void deveCriarSessaoComDuracaoDefaultDoApplicationYamlQuandoBodyNaoInformado() {
        PautaEntity pauta = pauta(1L);
        when(pautaService.buscarEntidade(1L)).thenReturn(pauta);
        when(sessaoRepository.existsByPautaEntityId(1L)).thenReturn(false);
        when(sessaoRepository.save(any(SessaoVotacaoEntity.class))).thenAnswer(invocation -> {
            SessaoVotacaoEntity sessao = invocation.getArgument(0);
            sessao.setId(10L);
            return sessao;
        });

        SessaoVotacaoResponse response = sessaoService.criar(1L, (CriarSessaoRequest) null);

        assertThat(response.fechaEm()).isEqualTo(response.abertaEm().plusSeconds(90));
    }

    @Test
    void deveBloquearCriacaoQuandoDuracaoForInvalida() {
        assertThatThrownBy(() -> sessaoService.criar(1L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maior que zero");
        verify(sessaoRepository, never()).save(any());
    }

    @Test
    void deveBloquearCriacaoQuandoPautaJaPossuirSessao() {
        when(pautaService.buscarEntidade(1L)).thenReturn(pauta(1L));
        when(sessaoRepository.existsByPautaEntityId(1L)).thenReturn(true);

        assertThatThrownBy(() -> sessaoService.criar(1L, new CriarSessaoRequest(120)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("1 sessão para 1 pauta");
        verify(sessaoRepository, never()).save(any());
    }

    @Test
    void deveListarSessoesComSucesso() {
        PautaEntity pauta = pauta(1L);
        when(sessaoRepository.findAll()).thenReturn(List.of(sessao(10L, pauta, StatusSessaoVotacao.CRIADA)));

        List<SessaoVotacaoResponse> responses = sessaoService.listar();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(10L);
    }

    @Test
    void deveBuscarSessaoPorPautaIdComSucesso() {
        PautaEntity pauta = pauta(1L);
        when(pautaService.buscarEntidade(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaEntityId(1L)).thenReturn(Optional.of(sessao(10L, pauta, StatusSessaoVotacao.CRIADA)));

        SessaoVotacaoResponse response = sessaoService.buscarPorPautaId(1L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.pautaId()).isEqualTo(1L);
    }

    @Test
    void deveLancarErroQuandoSessaoNaoExistirParaPauta() {
        when(pautaService.buscarEntidade(1L)).thenReturn(pauta(1L));
        when(sessaoRepository.findByPautaEntityId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessaoService.buscarPorPautaId(1L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("1");
    }

    @Test
    void deveAtualizarSessaoCriadaSemVotoComSucesso() {
        PautaEntity pauta = pauta(1L);
        SessaoVotacaoEntity sessao = sessao(10L, pauta, StatusSessaoVotacao.CRIADA);
        when(sessaoRepository.findById(10L)).thenReturn(Optional.of(sessao));
        when(votoRepository.existsByPautaEntityId(1L)).thenReturn(false);
        when(sessaoRepository.save(sessao)).thenReturn(sessao);

        SessaoVotacaoResponse response = sessaoService.atualizar(10L, new AtualizarSessaoRequest(300));

        assertThat(response.fechaEm()).isEqualTo(response.abertaEm().plusSeconds(300));
        verify(sessaoRepository).save(sessao);
    }

    @Test
    void deveBloquearAtualizacaoQuandoSessaoJaFoiDisponibilizada() {
        PautaEntity pauta = pauta(1L);
        when(sessaoRepository.findById(10L)).thenReturn(Optional.of(sessao(10L, pauta, StatusSessaoVotacao.DISPONIVEL)));

        assertThatThrownBy(() -> sessaoService.atualizar(10L, new AtualizarSessaoRequest(300)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("disponibilizada");
    }

    @Test
    void deveDisponibilizarSessaoCriadaComSucesso() {
        PautaEntity pauta = pauta(1L);
        SessaoVotacaoEntity sessao = sessao(10L, pauta, StatusSessaoVotacao.CRIADA);
        when(sessaoRepository.findById(10L)).thenReturn(Optional.of(sessao));
        when(sessaoRepository.save(sessao)).thenReturn(sessao);

        SessaoVotacaoResponse response = sessaoService.disponibilizar(10L);

        assertThat(response.status()).isEqualTo(StatusSessaoVotacao.DISPONIVEL);
        verify(sessaoRepository).save(sessao);
    }

    @Test
    void deveDisponibilizarSessaoCriadaNoPassadoMantendoDuracaoOriginal() {
        PautaEntity pauta = pauta(1L);
        LocalDateTime criadaEm = LocalDateTime.now().minusMinutes(10);
        SessaoVotacaoEntity sessao = SessaoVotacaoEntity.builder()
                .id(10L)
                .pautaEntity(pauta)
                .abertaEm(criadaEm)
                .fechaEm(criadaEm.plusSeconds(60))
                .status(StatusSessaoVotacao.CRIADA)
                .resultadoPublicado(false)
                .build();
        when(sessaoRepository.findById(10L)).thenReturn(Optional.of(sessao));
        when(sessaoRepository.save(sessao)).thenReturn(sessao);

        SessaoVotacaoResponse response = sessaoService.disponibilizar(10L);

        assertThat(response.status()).isEqualTo(StatusSessaoVotacao.DISPONIVEL);
        assertThat(response.fechaEm()).isAfter(response.abertaEm());
        assertThat(response.fechaEm()).isEqualTo(response.abertaEm().plusSeconds(60));
    }

    @Test
    void deveBloquearDisponibilizacaoQuandoSessaoNaoEstiverCriada() {
        PautaEntity pauta = pauta(1L);
        when(sessaoRepository.findById(10L)).thenReturn(Optional.of(sessao(10L, pauta, StatusSessaoVotacao.ENCERRADA)));

        assertThatThrownBy(() -> sessaoService.disponibilizar(10L))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("criada");
    }

    @Test
    void deveEncerrarSessaoDisponivelComSucesso() {
        PautaEntity pauta = pauta(1L);
        SessaoVotacaoEntity sessao = sessao(10L, pauta, StatusSessaoVotacao.DISPONIVEL);
        when(sessaoRepository.findById(10L)).thenReturn(Optional.of(sessao));
        when(sessaoRepository.save(sessao)).thenReturn(sessao);

        SessaoVotacaoResponse response = sessaoService.encerrar(10L);

        assertThat(response.status()).isEqualTo(StatusSessaoVotacao.ENCERRADA);
        assertThat(response.encerradaEm()).isNotNull();
    }

    @Test
    void deveBloquearEncerramentoQuandoSessaoAindaEstiverCriada() {
        PautaEntity pauta = pauta(1L);
        when(sessaoRepository.findById(10L)).thenReturn(Optional.of(sessao(10L, pauta, StatusSessaoVotacao.CRIADA)));

        assertThatThrownBy(() -> sessaoService.encerrar(10L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não disponibilizada");
    }

    private PautaEntity pauta(Long id) {
        return PautaEntity.builder()
                .id(id)
                .titulo("Pauta " + id)
                .descricao("Descrição")
                .criadaEm(LocalDateTime.now())
                .build();
    }

    private SessaoVotacaoEntity sessao(Long id, PautaEntity pauta, StatusSessaoVotacao status) {
        LocalDateTime abertaEm = LocalDateTime.now().minusMinutes(1);
        return SessaoVotacaoEntity.builder()
                .id(id)
                .pautaEntity(pauta)
                .abertaEm(abertaEm)
                .fechaEm(abertaEm.plusMinutes(5))
                .status(status)
                .resultadoPublicado(false)
                .build();
    }
}
