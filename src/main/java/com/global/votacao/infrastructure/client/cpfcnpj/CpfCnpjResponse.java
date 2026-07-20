package com.global.votacao.infrastructure.client.cpfcnpj;

import com.global.votacao.domain.model.TipoDocumento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CpfCnpjResponse {

    private Integer status;
    private String cpf;
    private String cnpj;
    private String nome;
    private Integer pacoteUsado;
    private Integer saldo;
    private String consultaID;
    private Double delay;
    private String erro;
    private Integer erroCodigo;

    String mensagemErro(TipoDocumento tipoDocumento) {
        if (erro != null && !erro.isBlank()) {
            return erro;
        }
        return tipoDocumento + " inválido";
    }
}
