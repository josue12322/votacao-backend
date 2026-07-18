package com.global.votacao.adapter.in.web.exception;

import com.global.votacao.application.dto.ErroResponse;
import com.global.votacao.shared.exception.ConflitoException;
import com.global.votacao.shared.exception.DependenciaExternaException;
import com.global.votacao.shared.exception.RecursoNaoEncontradoException;
import com.global.votacao.shared.exception.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroResponse> handleNotFound(RecursoNaoEncontradoException exception, HttpServletRequest request) {
        return erro(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<ErroResponse> handleConflict(ConflitoException exception, HttpServletRequest request) {
        return erro(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroResponse> handleIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        return erro(HttpStatus.CONFLICT, "OperaÃ§Ã£o viola uma restriÃ§Ã£o de integridade", request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ErroResponse> handleBusiness(RegraNegocioException exception, HttpServletRequest request) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    @ExceptionHandler(DependenciaExternaException.class)
    ResponseEntity<ErroResponse> handleExternal(DependenciaExternaException exception, HttpServletRequest request) {
        return erro(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return erro(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErroResponse> handleMethodValidation(HandlerMethodValidationException exception, HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, "ParÃ¢metro invÃ¡lido", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErroResponse> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return erro(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado", request);
    }

    private ResponseEntity<ErroResponse> erro(HttpStatus status, String message, HttpServletRequest request) {
        ErroResponse response = new ErroResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }
}



