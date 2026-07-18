package com.global.votacao.application.mapper;

import com.global.votacao.application.dto.CriarPautaRequest;
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

}


