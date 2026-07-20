package com.global.votacao.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.global.votacao.application.dto.RegistrarVotoRequest;
import com.global.votacao.application.dto.VotoResponse;
import com.global.votacao.application.mapper.VotoMapper;
import com.global.votacao.application.port.out.AssociadoElegibilidadeClient;
import com.global.votacao.domain.entity.PautaEntity;
import com.global.votacao.domain.entity.SessaoVotacaoEntity;
import com.global.votacao.domain.entity.Voto;
import com.global.votacao.domain.model.StatusSessaoVotacao;
import com.global.votacao.domain.model.TipoDocumento;
import com.global.votacao.domain.model.TipoVoto;
import com.global.votacao.infrastructure.persistence.repository.SessaoVotacaoRepository;
import com.global.votacao.infrastructure.persistence.repository.VotoRepository;
import com.global.votacao.shared.exception.ConflitoException;
import com.global.votacao.shared.exception.DocumentoInvalidoException;
import com.global.votacao.shared.exception.RecursoNaoEncontradoException;
import com.global.votacao.shared.exception.RegraNegocioException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {

    @Mock
    private PautaService pautaService;

    @Mock
    private SessaoVotacaoRepository sessaoRepository;

    @Mock
    private VotoRepository votoRepository;

    @Mock
    private AssociadoElegibilidadeClient elegibilidadeClient;

    private VotoService votoService;

    @BeforeEach
    void setUp() {
        votoService = new VotoService(pautaService, sessaoRepository, votoRepository, elegibilidadeClient, new VotoMapper());
    }

    @Test
    void deveRegistrarVotoComSucesso() {
        PautaEntity pauta = pauta(1L);
        RegistrarVotoRequest request = new RegistrarVotoRequest(TipoDocumento.CPF, "615.004.213-81", TipoVoto.SIM);
        when(pautaService.buscarEntidade(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaEntityId(1L)).thenReturn(Optional.of(sessaoDisponivel(pauta)));
        when(votoRepository.existsByPautaEntityIdAndDocumento(1L, "61500421381")).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenAnswer(invocation -> {
            Voto voto = invocation.getArgument(0);
            voto.setId(100L);
            return voto;
        });

        VotoResponse response = votoService.registrar(1L, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getPautaId()).isEqualTo(1L);
        assertThat(response.getDocumento()).isEqualTo("61500421381");
        assertThat(response.getVoto()).isEqualTo(TipoVoto.SIM);
        verify(elegibilidadeClient).validarElegibilidade("61500421381", TipoDocumento.CPF);
    }

    @Test
    void deveLancarErroQuandoSessaoNaoExistir() {
        RegistrarVotoRequest request = new RegistrarVotoRequest(TipoDocumento.CPF, "61500421381", TipoVoto.SIM);
        when(pautaService.buscarEntidade(1L)).thenReturn(pauta(1L));
        when(sessaoRepository.findByPautaEntityId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> votoService.registrar(1L, request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("1");
    }

    @Test
    void deveBloquearVotoQuandoSessaoNaoAceitarVoto() {
        PautaEntity pauta = pauta(1L);
        RegistrarVotoRequest request = new RegistrarVotoRequest(TipoDocumento.CPF, "61500421381", TipoVoto.SIM);
        when(pautaService.buscarEntidade(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaEntityId(1L)).thenReturn(Optional.of(sessaoEncerrada(pauta)));

        assertThatThrownBy(() -> votoService.registrar(1L, request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("votos");
        verify(elegibilidadeClient, never()).validarElegibilidade(any(), any());
        verify(votoRepository, never()).save(any());
    }

    @Test
    void deveBloquearVotoQuandoDocumentoForInvalidoNaIntegracao() {
        PautaEntity pauta = pauta(1L);
        RegistrarVotoRequest request = new RegistrarVotoRequest(TipoDocumento.CPF, "61500421379", TipoVoto.SIM);
        when(pautaService.buscarEntidade(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaEntityId(1L)).thenReturn(Optional.of(sessaoDisponivel(pauta)));
        doThrow(new DocumentoInvalidoException("CPF inválido!"))
                .when(elegibilidadeClient).validarElegibilidade("61500421379", TipoDocumento.CPF);

        assertThatThrownBy(() -> votoService.registrar(1L, request))
                .isInstanceOf(DocumentoInvalidoException.class)
                .hasMessageContaining("CPF inválido");
        verify(votoRepository, never()).save(any());
    }

    @Test
    void deveBloquearVotoDuplicadoAntesDeSalvar() {
        PautaEntity pauta = pauta(1L);
        RegistrarVotoRequest request = new RegistrarVotoRequest(TipoDocumento.CPF, "61500421381", TipoVoto.NAO);
        when(pautaService.buscarEntidade(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaEntityId(1L)).thenReturn(Optional.of(sessaoDisponivel(pauta)));
        when(votoRepository.existsByPautaEntityIdAndDocumento(1L, "61500421381")).thenReturn(true);

        assertThatThrownBy(() -> votoService.registrar(1L, request))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("votou");
        verify(votoRepository, never()).save(any());
    }

    @Test
    void deveTraduzirViolacaoDeIntegridadeComoConflito() {
        PautaEntity pauta = pauta(1L);
        RegistrarVotoRequest request = new RegistrarVotoRequest(TipoDocumento.CPF, "61500421381", TipoVoto.SIM);
        when(pautaService.buscarEntidade(1L)).thenReturn(pauta);
        when(sessaoRepository.findByPautaEntityId(1L)).thenReturn(Optional.of(sessaoDisponivel(pauta)));
        when(votoRepository.existsByPautaEntityIdAndDocumento(1L, "61500421381")).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThatThrownBy(() -> votoService.registrar(1L, request))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("votou");
    }

    private PautaEntity pauta(Long id) {
        return PautaEntity.builder()
                .id(id)
                .titulo("Pauta " + id)
                .descricao("Descrição")
                .criadaEm(LocalDateTime.now())
                .build();
    }

    private SessaoVotacaoEntity sessaoDisponivel(PautaEntity pauta) {
        LocalDateTime abertaEm = LocalDateTime.now().minusMinutes(1);
        return SessaoVotacaoEntity.builder()
                .id(10L)
                .pautaEntity(pauta)
                .abertaEm(abertaEm)
                .fechaEm(LocalDateTime.now().plusMinutes(5))
                .status(StatusSessaoVotacao.DISPONIVEL)
                .resultadoPublicado(false)
                .build();
    }

    private SessaoVotacaoEntity sessaoEncerrada(PautaEntity pauta) {
        LocalDateTime abertaEm = LocalDateTime.now().minusMinutes(10);
        return SessaoVotacaoEntity.builder()
                .id(10L)
                .pautaEntity(pauta)
                .abertaEm(abertaEm)
                .fechaEm(abertaEm.plusMinutes(1))
                .status(StatusSessaoVotacao.ENCERRADA)
                .resultadoPublicado(false)
                .build();
    }
}
