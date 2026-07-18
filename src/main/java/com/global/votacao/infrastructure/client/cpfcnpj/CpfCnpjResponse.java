package com.global.votacao.infrastructure.client.cpfcnpj;

import com.global.votacao.domain.model.TipoDocumento;

public record CpfCnpjResponse(
        int status,
        String cpf,
        String nome,
        Integer pacoteUsado,
        Integer saldo,
        String consultaID,
        Double delay,
        String erro,
        Integer erroCodigo
) {
    String mensagemErro(TipoDocumento tipoDocumento) {
        if (erro != null && !erro.isBlank()) {
            return erro;
        }
        return tipoDocumento + " invÃ¡lido";
    }
}


