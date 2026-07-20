package com.global.votacao.infrastructure.persistence.repository;

import com.global.votacao.domain.entity.SessaoVotacaoEntity;
import com.global.votacao.domain.model.StatusSessaoVotacao;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoVotacaoRepository extends JpaRepository<SessaoVotacaoEntity, Long> {

    Optional<SessaoVotacaoEntity> findByPautaEntityId(Long pautaId);


    boolean existsByPautaEntityId(Long pautaId);

    boolean existsByPautaEntityIdAndStatusIn(Long pautaId, List<StatusSessaoVotacao> status);

    List<SessaoVotacaoEntity> findByStatusAndFechaEmLessThanEqualOrderByFechaEmAsc(
            StatusSessaoVotacao status,
            LocalDateTime fechaEm,
            Pageable pageable
    );

    List<SessaoVotacaoEntity> findByStatusAndResultadoPublicadoFalseOrderByFechaEmAsc(
            StatusSessaoVotacao status,
            Pageable pageable
    );
}


