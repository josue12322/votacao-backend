package com.global.votacao.infrastructure.persistence.repository;

import com.global.votacao.domain.entity.PautaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PautaRepository extends JpaRepository<PautaEntity, Long> {
}


