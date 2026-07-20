package com.global.votacao.infrastructure.client.cpfcnpj;

import com.global.votacao.application.port.out.AssociadoElegibilidadeClient;
import com.global.votacao.domain.model.TipoDocumento;
import com.global.votacao.shared.exception.DependenciaExternaException;
import com.global.votacao.shared.exception.RegraNegocioException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CpfCnpjAssociadoElegibilidadeClient implements AssociadoElegibilidadeClient {

    private static final Logger log = LoggerFactory.getLogger(CpfCnpjAssociadoElegibilidadeClient.class);

    private final AssociadoElegibilidadeProperties properties;
    private final CpfCnpjFeignClient cpfCnpjFeignClient;
    private final CpfCnpjErrorHandler errorHandler;

    @Override
    public void validarElegibilidade(String documento, TipoDocumento tipoDocumento) {
        if (!properties.habilitada()) {
            return;
        }

        validarTipoDocumento(tipoDocumento);
        String documentoNormalizado = normalizarDocumento(documento);
        validarFormatoDocumento(documentoNormalizado, tipoDocumento);

        try {
            CpfCnpjResponse response = cpfCnpjFeignClient.consultarDocumento(
                    properties.token(),
                    codigoConsulta(tipoDocumento),
                    documentoNormalizado
            );
            validarResponse(response, tipoDocumento);
        } catch (FeignException.BadRequest exception) {
            throw errorHandler.documentoInvalido(exception, tipoDocumento, documentoNormalizado);
        } catch (FeignException.NotFound exception) {
            throw new RegraNegocioException(tipoDocumento + " inválido");
        } catch (FeignException exception) {
            throw erroDependenciaExterna(tipoDocumento, documentoNormalizado, exception);
        } catch (RegraNegocioException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw erroDependenciaExterna(tipoDocumento, documentoNormalizado, exception);
        }
    }

    private void validarTipoDocumento(TipoDocumento tipoDocumento) {
        if (tipoDocumento == null) {
            throw new RegraNegocioException("Tipo do documento é obrigatório para validação de elegibilidade");
        }
    }

    private void validarResponse(CpfCnpjResponse response, TipoDocumento tipoDocumento) {
        if (response == null || response.getStatus() == null || response.getStatus() != 1) {
            String mensagem = response == null ? tipoDocumento + " inválido" : response.mensagemErro(tipoDocumento);
            throw new RegraNegocioException(mensagem);
        }
    }

    private DependenciaExternaException erroDependenciaExterna(
            TipoDocumento tipoDocumento,
            String documentoNormalizado,
            RuntimeException exception
    ) {
        log.warn("Falha ao validar {} mascarado {}", tipoDocumento, mascararDocumento(documentoNormalizado), exception);
        return new DependenciaExternaException("Serviço externo de validação de documento indisponível", exception);
    }

    private void validarFormatoDocumento(String documento, TipoDocumento tipoDocumento) {
        if (documento.isBlank()) {
            throw new RegraNegocioException(tipoDocumento + " é obrigatório para validação de elegibilidade");
        }
        if (tipoDocumento == TipoDocumento.CPF && documento.length() != 11) {
            throw new RegraNegocioException("CPF deve conter 11 dígitos");
        }
        if (tipoDocumento == TipoDocumento.CNPJ && documento.length() != 14) {
            throw new RegraNegocioException("CNPJ deve conter 14 dígitos");
        }
    }

    private Integer codigoConsulta(TipoDocumento tipoDocumento) {
        return tipoDocumento == TipoDocumento.CPF ? properties.codigoCpf() : properties.codigoCnpj();
    }

    private String normalizarDocumento(String documento) {
        if (documento == null) {
            return "";
        }
        return documento.replaceAll("\\D", "");
    }

    private String mascararDocumento(String documento) {
        if (documento == null || documento.length() < 4) {
            return "***";
        }
        return "***" + documento.substring(documento.length() - 4);
    }
}
