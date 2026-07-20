package com.global.votacao.application.service;

import com.global.votacao.application.port.out.AssociadoElegibilidadeClient;
import com.global.votacao.application.dto.RegistrarVotoRequest;
import com.global.votacao.application.dto.VotoResponse;
import com.global.votacao.domain.entity.PautaEntity;
import com.global.votacao.domain.entity.SessaoVotacaoEntity;
import com.global.votacao.domain.entity.Voto;
import com.global.votacao.infrastructure.persistence.repository.SessaoVotacaoRepository;
import com.global.votacao.infrastructure.persistence.repository.VotoRepository;
import com.global.votacao.application.mapper.VotoMapper;
import com.global.votacao.shared.exception.ConflitoException;
import com.global.votacao.shared.exception.RecursoNaoEncontradoException;
import com.global.votacao.shared.exception.RegraNegocioException;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class VotoService {

    private static final Logger log = LoggerFactory.getLogger(VotoService.class);

    private final PautaService pautaService;
    private final SessaoVotacaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final AssociadoElegibilidadeClient elegibilidadeClient;
    private final VotoMapper mapper;

    @Transactional
    public VotoResponse registrar(Long pautaId, RegistrarVotoRequest request) {
        PautaEntity pautaEntity = pautaService.buscarEntidade(pautaId);
        SessaoVotacaoEntity sessao = sessaoRepository.findByPautaEntityId(pautaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão não encontrada para a pauta " + pautaId));
        if (!sessao.aceitaVoto(LocalDateTime.now())) {
            log.info("Voto recusado porque sessão não está Disponivél pautaId={} documento={}", pautaId, request.documento());
            throw new RegraNegocioException("Sessão de votação não está disponivél para receber votos");
        }

        String documento = normalizarDocumento(request.documento());
        elegibilidadeClient.validarElegibilidade(documento, request.tipoDocumento());

        if (votoRepository.existsByPautaEntityIdAndDocumento(pautaId, documento)) {
            log.info("Voto duplicado recusado pautaId={} documento={}", pautaId, documento);
            throw new ConflitoException("usuario já votou nesta pauta");
        }

        try {
            Voto voto = votoRepository.save(mapper.toMapperVoto(pautaEntity, request.tipoDocumento(), documento, request.voto()));
            log.info("Voto registrado pautaId={} documento={}", pautaId, documento);
            return mapper.toMapperVotoResponse(voto);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflitoException("usuario já votou nesta pauta");
        }
    }

    private String normalizarDocumento(String documento) {
        if (documento == null) {
            return null;
        }
        return documento.replaceAll("\\D", "");
    }
}


