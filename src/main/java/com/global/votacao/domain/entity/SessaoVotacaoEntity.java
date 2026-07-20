package com.global.votacao.domain.entity;

import com.global.votacao.domain.model.StatusSessaoVotacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "sessoes_votacao",
        uniqueConstraints = @UniqueConstraint(name = "uk_sessoes_votacao_pauta", columnNames = "pauta_id"),
        indexes = @Index(name = "idx_sessoes_status_fecha_em", columnList = "status, fecha_em")
)
public class SessaoVotacaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id", nullable = false)
    private PautaEntity pautaEntity;

    @Column(name = "aberta_em", nullable = false)
    private LocalDateTime abertaEm;

    @Column(name = "fecha_em", nullable = false)
    private LocalDateTime fechaEm;

    @Column(name = "encerrada_em")
    private LocalDateTime encerradaEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSessaoVotacao status;

    @Column(name = "resultado_publicado", nullable = false)
    private boolean resultadoPublicado;

    public boolean aceitaVoto(LocalDateTime agora) {
        return status == StatusSessaoVotacao.DISPONIVEL && agora.isBefore(fechaEm);
    }

    public boolean estaVencida(LocalDateTime agora) {
        return status == StatusSessaoVotacao.DISPONIVEL && !fechaEm.isAfter(agora);
    }

    public boolean podeSerAlterada() {
        return status == StatusSessaoVotacao.CRIADA;
    }

    public void disponibilizar(LocalDateTime abertaEm) {
        int duracaoEmSegundos = duracaoEmSegundos();
        this.status = StatusSessaoVotacao.DISPONIVEL;
        this.abertaEm = abertaEm;
        this.fechaEm = abertaEm.plusSeconds(duracaoEmSegundos);
    }

    public void atualizarDuracao(Integer duracaoEmSegundos) {
        this.fechaEm = abertaEm.plusSeconds(duracaoEmSegundos);
    }

    public void encerrar(LocalDateTime encerradaEm) {
        this.status = StatusSessaoVotacao.ENCERRADA;
        this.encerradaEm = encerradaEm;
    }

    public void marcarPublicada() {
        this.status = StatusSessaoVotacao.PUBLICADA;
        this.resultadoPublicado = true;
    }

    public int duracaoEmSegundos() {
        return (int) java.time.Duration.between(abertaEm, fechaEm).getSeconds();
    }
}


