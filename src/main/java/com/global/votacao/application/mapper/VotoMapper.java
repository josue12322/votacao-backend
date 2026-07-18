package com.global.votacao.application.mapper;

import com.global.votacao.domain.model.TipoDocumento;
import com.global.votacao.domain.model.TipoVoto;
import com.global.votacao.application.dto.VotoResponse;
import com.global.votacao.domain.entity.PautaEntity;
import com.global.votacao.domain.entity.Voto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class VotoMapper {
    public Voto toMapperVoto(PautaEntity pautaEntity, TipoDocumento tipoDocumento, String document, TipoVoto tipoVoto){
        return Voto.builder()
                .tipoDocumento(tipoDocumento)
                .pautaEntity(pautaEntity)
                .documento(normalizarDocumento(document))
                .tipo(tipoVoto)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    public VotoResponse toMapperVotoResponse( Voto voto) {
        return VotoResponse.builder()
                .id(voto.getId())
                .pautaId(voto.getPautaEntity().getId())
                .tipoDocumento(voto.getTipoDocumento())
                .documento(voto.getDocumento())
                .voto(voto.getTipo())
                .criadoEm(voto.getCriadoEm())
                .build();
    }

    private String normalizarDocumento(String documento) {
        if (documento == null) {
            return null;
        }
        return documento.replaceAll("\\D", "");
    }
}


