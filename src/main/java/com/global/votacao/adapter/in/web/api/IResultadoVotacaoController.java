package com.global.votacao.adapter.in.web.api;

import com.global.votacao.application.dto.ResultadoVotacaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/resultado")
@Tag(name = "Resultados", description = "Consulta da apuração parcial ou final de uma pauta.")
public interface IResultadoVotacaoController {

    @Operation(
            summary = "Consultar resultado da votação",
            description = "Retorna votos SIM, votos NAO, total, status calculado e vencedor. Sessões CRIADA ainda não recebem votos; sessões DISPONIVEL podem retornar resultado parcial; sessões ENCERRADA/PUBLICADA retornam resultado final."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Resultado encontrado",
                    content = @Content(schema = @Schema(implementation = ResultadoVotacaoResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Pauta ou sessão não encontrada", content = @Content)
    })
    @GetMapping
    ResponseEntity<ResultadoVotacaoResponse> consultar(
            @Parameter(description = "Identificador único da pauta.", example = "1", required = true)
            @PathVariable Long pautaId
    );
}
