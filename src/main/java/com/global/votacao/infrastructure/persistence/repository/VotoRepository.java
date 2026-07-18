package com.global.votacao.infrastructure.persistence.repository;

import com.global.votacao.domain.model.TipoVoto;
import com.global.votacao.domain.entity.Voto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    boolean existsByPautaEntityIdAndDocumento(Long pautaId, String documento);

    boolean existsByPautaEntityId(Long pautaId);

    long countByPautaEntityIdAndTipo(Long pautaId, TipoVoto tipo);
}


