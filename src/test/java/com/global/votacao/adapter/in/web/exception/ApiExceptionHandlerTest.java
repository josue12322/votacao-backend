package com.global.votacao.adapter.in.web.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.global.votacao.application.dto.ErroResponse;
import com.global.votacao.shared.exception.ConflitoException;
import com.global.votacao.shared.exception.DependenciaExternaException;
import com.global.votacao.shared.exception.RecursoNaoEncontradoException;
import com.global.votacao.shared.exception.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private ApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApiExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/pautas");
    }

    @Test
    void deveRetornarNotFoundParaRecursoNaoEncontrado() {
        ResponseEntity<ErroResponse> response = handler.handleNotFound(new RecursoNaoEncontradoException("Pauta não encontrada"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Pauta não encontrada");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/pautas");
    }

    @Test
    void deveRetornarConflictParaConflitoDeRegra() {
        ResponseEntity<ErroResponse> response = handler.handleConflict(new ConflitoException("Voto duplicado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Voto duplicado");
    }

    @Test
    void deveRetornarConflictParaViolacaoDeIntegridade() {
        ResponseEntity<ErroResponse> response = handler.handleIntegrity(new DataIntegrityViolationException("constraint"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("integridade");
    }

    @Test
    void deveRetornarUnprocessableEntityParaRegraNegocio() {
        ResponseEntity<ErroResponse> response = handler.handleBusiness(new RegraNegocioException("Sessão encerrada"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Sessão encerrada");
    }

    @Test
    void deveRetornarServiceUnavailableParaDependenciaExterna() {
        ResponseEntity<ErroResponse> response = handler.handleExternal(new DependenciaExternaException("CPF/CNPJ fora", new RuntimeException()), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("CPF/CNPJ fora");
    }

    @Test
    void deveRetornarBadRequestParaArgumentoInvalido() {
        ResponseEntity<ErroResponse> response = handler.handleIllegalArgument(new IllegalArgumentException("Duração inválida"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Duração inválida");
    }

    @Test
    void deveRetornarInternalServerErrorParaErroInesperado() {
        ResponseEntity<ErroResponse> response = handler.handleUnexpected(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Erro inesperado");
    }
}
