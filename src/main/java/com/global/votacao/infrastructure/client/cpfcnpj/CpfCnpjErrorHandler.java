package com.global.votacao.infrastructure.client.cpfcnpj;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.global.votacao.domain.model.TipoDocumento;
import com.global.votacao.shared.exception.DocumentoInvalidoException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CpfCnpjErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(CpfCnpjErrorHandler.class);

    private final ObjectMapper objectMapper;

    public DocumentoInvalidoException documentoInvalido(
            FeignException exception,
            TipoDocumento tipoDocumento,
            String documentoNormalizado
    ) {
        CpfCnpjResponse responseErro = extrairResponseErro(exception, tipoDocumento);
        String mensagem = responseErro.mensagemErro(tipoDocumento);

        log.warn(
                "Documento recusado pela API CPF/CNPJ tipoDocumento={} documentoMascarado={} erroCodigo={} erro={}",
                tipoDocumento,
                mascararDocumento(documentoNormalizado),
                responseErro.getErroCodigo(),
                mensagem
        );

        return new DocumentoInvalidoException(mensagem);
    }

    private CpfCnpjResponse extrairResponseErro(FeignException exception, TipoDocumento tipoDocumento) {
        String body = exception.contentUTF8();
        if (body == null || body.isBlank()) {
            return CpfCnpjResponse.builder().erro(tipoDocumento + " inválido").build();
        }
        try {
            return objectMapper.readValue(body, CpfCnpjResponse.class);
        } catch (JsonProcessingException jsonException) {
            log.warn("Falha ao converter erro da API CPF/CNPJ body={}", body, jsonException);
            return CpfCnpjResponse.builder().erro(tipoDocumento + " inválido").build();
        }
    }

    private String mascararDocumento(String documento) {
        if (documento == null || documento.length() < 4) {
            return "***";
        }
        return "***" + documento.substring(documento.length() - 4);
    }
}
