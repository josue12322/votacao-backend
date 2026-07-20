package com.global.votacao.application.mapper;

import com.global.votacao.application.dto.SessaoVotacaoResponse;
import com.global.votacao.domain.model.StatusSessaoVotacao;
import com.global.votacao.domain.entity.PautaEntity;
import com.global.votacao.domain.entity.SessaoVotacaoEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SessaoMapper {

    public SessaoVotacaoEntity toMapperSessaoEntity(PautaEntity pautaEntity, Integer duracaoEmSegundos){
       LocalDateTime dataAbertura = LocalDateTime.now();
        return SessaoVotacaoEntity.builder()
                .pautaEntity(pautaEntity)
                .abertaEm(dataAbertura)
                .fechaEm(dataAbertura.plusSeconds(duracaoEmSegundos))
                .status(StatusSessaoVotacao.CRIADA)
                .resultadoPublicado(false)
                .build();
    }

    public SessaoVotacaoEntity toMapperSessaoEntityPublish(PautaEntity pautaEntity){
        return  SessaoVotacaoEntity.builder()
                .status(StatusSessaoVotacao.PUBLICADA)
                .build();
    }

    public SessaoVotacaoResponse toSessaoResponse(SessaoVotacaoEntity sessao) {
        return new SessaoVotacaoResponse(
                sessao.getId(),
                sessao.getPautaEntity().getId(),
                sessao.getAbertaEm(),
                sessao.getFechaEm(),
                sessao.getEncerradaEm(),
                sessao.getStatus()
        );
    }



//    new SessaoVotacaoEntity(pautaEntity, abertaEm, abertaEm.plusSeconds(duracaoEmSegundos)
}


