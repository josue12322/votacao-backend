package com.global.votacao.application.mapper;

import com.global.votacao.application.dto.CriarPautaRequest;
import com.global.votacao.application.dto.PautaResponse;
import com.global.votacao.domain.entity.PautaEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PautaMapper {

    public PautaEntity toMapperPautaEntity(CriarPautaRequest request){
        return PautaEntity.builder()
                .titulo(request.titulo())
                .descricao(request.descricao())
                .criadaEm(LocalDateTime.now())
                .build();
    }


    public PautaResponse toResponse(PautaEntity pautaEntity) {
        PautaResponse pautaResponse = new PautaResponse();
        pautaResponse.setId(pautaEntity.getId());
        pautaResponse.setTitulo(pautaEntity.getTitulo());
        pautaResponse.setDescricao(pautaEntity.getDescricao());
        pautaResponse.setCriadaEm(pautaEntity.getCriadaEm());
        return pautaResponse;
    }


}


