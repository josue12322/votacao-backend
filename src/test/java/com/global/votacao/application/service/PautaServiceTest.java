package com.global.votacao.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.global.votacao.application.dto.AtualizarPautaRequest;
import com.global.votacao.application.dto.CriarPautaRequest;
import com.global.votacao.application.dto.PautaResponse;
import com.global.votacao.application.dto.PautaSessaoAbertaResponse;
import com.global.votacao.application.mapper.PautaMapper;
import com.global.votacao.application.mapper.SessaoMapper;
import com.global.votacao.domain.entity.PautaEntity;
import com.global.votacao.domain.entity.SessaoVotacaoEntity;
import com.global.votacao.domain.model.StatusSessaoVotacao;
import com.global.votacao.infrastructure.persistence.repository.PautaRepository;
import com.global.votacao.infrastructure.persistence.repository.SessaoVotacaoRepository;
import com.global.votacao.infrastructure.persistence.repository.VotoRepository;
import com.global.votacao.shared.exception.ConflitoException;
import com.global.votacao.shared.exception.RecursoNaoEncontradoException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PautaServiceTest {

    @Mock
    private PautaRepository pautaRepository;

    @Mock
    private SessaoVotacaoRepository sessaoRepository;

    @Mock
    private VotoRepository votoRepository;

    private PautaService pautaService;

    @BeforeEach
    void setUp() {
        pautaService = new PautaService(
                pautaRepository,
                sessaoRepository,
                votoRepository,
                new PautaMapper(),
                new SessaoMapper()
        );
    }

    @Test
    void deveCriarPautaComSucesso() {
        CriarPautaRequest request = new CriarPautaRequest("Nova política", "Descrição da pauta");
        when(pautaRepository.save(any(PautaEntity.class))).thenAnswer(invocation -> {
            PautaEntity pauta = invocation.getArgument(0);
            pauta.setId(1L);
            return pauta;
        });

        PautaResponse response = pautaService.criar(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitulo()).isEqualTo("Nova política");
        assertThat(response.getDescricao()).isEqualTo("Descrição da pauta");
        verify(pautaRepository).save(any(PautaEntity.class));
    }

    @Test
    void deveListarPautasComSucesso() {
        when(pautaRepository.findAll()).thenReturn(List.of(pauta(1L), pauta(2L)));

        List<PautaResponse> responses = pautaService.listar();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(PautaResponse::getId).containsExactly(1L, 2L);
    }

    @Test
    void deveBuscarPautaPorIdComSucesso() {
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta(1L)));

        PautaResponse response = pautaService.buscarPorId(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitulo()).isEqualTo("Pauta 1");
    }

    @Test
    void deveLancarErroQuandoPautaNaoExistir() {
        when(pautaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pautaService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deveBuscarPautaComSessaoAbertaComSucesso() {
        PautaEntity pauta = pauta(1L);
        SessaoVotacaoEntity sessao = sessao(10L, pauta, StatusSessaoVotacao.DISPONIVEL);
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.findByPautaEntityId(1L)).thenReturn(Optional.of(sessao));

        PautaSessaoAbertaResponse response = pautaService.buscarComSessaoAberta(1L);

        assertThat(response.pauta().getId()).isEqualTo(1L);
        assertThat(response.sessao().id()).isEqualTo(10L);
    }

    @Test
    void deveAtualizarPautaQuandoNaoPossuirVotosNemSessaoBloqueante() {
        PautaEntity pauta = pauta(1L);
        when(votoRepository.existsByPautaEntityId(1L)).thenReturn(false);
        when(sessaoRepository.existsByPautaEntityIdAndStatusIn(any(), any())).thenReturn(false);
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));
        when(pautaRepository.save(pauta)).thenReturn(pauta);

        PautaResponse response = pautaService.atualizar(1L, new AtualizarPautaRequest("Título atualizado", "Nova descrição"));

        assertThat(response.getTitulo()).isEqualTo("Título atualizado");
        assertThat(response.getDescricao()).isEqualTo("Nova descrição");
        verify(pautaRepository).save(pauta);
    }

    @Test
    void deveBloquearAtualizacaoQuandoPautaPossuirVoto() {
        when(votoRepository.existsByPautaEntityId(1L)).thenReturn(true);

        assertThatThrownBy(() -> pautaService.atualizar(1L, new AtualizarPautaRequest("Título", "Descrição")))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("voto");
        verify(pautaRepository, never()).save(any());
    }

    @Test
    void deveDeletarPautaComSucesso() {
        PautaEntity pauta = pauta(1L);
        when(votoRepository.existsByPautaEntityId(1L)).thenReturn(false);
        when(sessaoRepository.existsByPautaEntityIdAndStatusIn(any(), any())).thenReturn(false);
        when(sessaoRepository.existsByPautaEntityId(1L)).thenReturn(false);
        when(pautaRepository.findById(1L)).thenReturn(Optional.of(pauta));

        pautaService.deletar(1L);

        verify(pautaRepository).delete(pauta);
    }

    @Test
    void deveBloquearDeleteQuandoPautaPossuirSessaoVinculada() {
        when(votoRepository.existsByPautaEntityId(1L)).thenReturn(false);
        when(sessaoRepository.existsByPautaEntityIdAndStatusIn(any(), any())).thenReturn(false);
        when(sessaoRepository.existsByPautaEntityId(1L)).thenReturn(true);

        assertThatThrownBy(() -> pautaService.deletar(1L))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("sess");
        verify(pautaRepository, never()).delete(any());
    }

    private PautaEntity pauta(Long id) {
        return PautaEntity.builder()
                .id(id)
                .titulo("Pauta " + id)
                .descricao("Descrição " + id)
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
