package com.global.votacao.infrastructure.client.cpfcnpj;

import com.global.votacao.application.port.out.AssociadoElegibilidadeClient;
import com.global.votacao.domain.model.TipoDocumento;
import com.global.votacao.shared.exception.DependenciaExternaException;
import com.global.votacao.shared.exception.RegraNegocioException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CpfCnpjAssociadoElegibilidadeClient implements AssociadoElegibilidadeClient {

    private static final Logger log = LoggerFactory.getLogger(CpfCnpjAssociadoElegibilidadeClient.class);

    private final AssociadoElegibilidadeProperties properties;
    private final CpfCnpjFeignClient cpfCnpjFeignClient;

    public CpfCnpjAssociadoElegibilidadeClient(
            AssociadoElegibilidadeProperties properties,
            CpfCnpjFeignClient cpfCnpjFeignClient
    ) {
        this.properties = properties;
        this.cpfCnpjFeignClient = cpfCnpjFeignClient;
    }

    @Override
    public void validarElegibilidade(String documento, TipoDocumento tipoDocumento) {
        if (!properties.habilitada()) {
            return;
        }
        if (tipoDocumento == null) {
            throw new RegraNegocioException("Tipo do documento Ã© obrigatÃ³rio para validaÃ§Ã£o de elegibilidade");
        }

        String documentoNormalizado = normalizarDocumento(documento);
        validarFormatoDocumento(documentoNormalizado, tipoDocumento);
        Integer codigoConsulta = codigoConsulta(tipoDocumento);

        try {
            CpfCnpjResponse response = cpfCnpjFeignClient.consultarDocumento(
                    properties.token(),
                    codigoConsulta,
                    documentoNormalizado
            );
            if (response == null || response.status() != 1) {
                String mensagem = response == null ? tipoDocumento + " invÃ¡lido" : response.mensagemErro(tipoDocumento);
                throw new RegraNegocioException(mensagem);
            }
        } catch (FeignException exception) {
            log.warn("Falha HTTP ao validar {} mascarado {}", tipoDocumento, mascararDocumento(documentoNormalizado), exception);
            throw new DependenciaExternaException("ServiÃ§o externo de validaÃ§Ã£o de documento indisponÃ­vel", exception);
        } catch (RegraNegocioException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Falha ao validar {} mascarado {}", tipoDocumento, mascararDocumento(documentoNormalizado), exception);
            throw new DependenciaExternaException("ServiÃ§o externo de validaÃ§Ã£o de documento indisponÃ­vel", exception);
        }
    }

    private void validarFormatoDocumento(String documento, TipoDocumento tipoDocumento) {
        if (documento.isBlank()) {
            throw new RegraNegocioException(tipoDocumento + " Ã© obrigatÃ³rio para validaÃ§Ã£o de elegibilidade");
        }
        if (tipoDocumento == TipoDocumento.CPF && documento.length() != 11) {
            throw new RegraNegocioException("CPF deve conter 11 dÃ­gitos");
        }
        if (tipoDocumento == TipoDocumento.CNPJ && documento.length() != 14) {
            throw new RegraNegocioException("CNPJ deve conter 14 dÃ­gitos");
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



