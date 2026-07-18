package com.global.votacao.application.port.out;

import com.global.votacao.domain.model.TipoDocumento;

public interface AssociadoElegibilidadeClient {

    void validarElegibilidade(String documento, TipoDocumento tipoDocumento);
}


