package com.global.votacao.adapter.in.web.api;

import com.global.votacao.application.dto.RegistrarVotoRequest;
import com.global.votacao.application.dto.VotoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/votos")
@Tag(name = "Votos", description = "Registro de votos por documento em sessões disponibilizadas para votação.")
public interface IVotoController {

    @Operation(
            summary = "Registrar voto do documento",
            description = "Registra um voto SIM ou NAO para uma pauta com sessão DISPONIVEL. Cada documento pode votar apenas uma vez por pauta. Votos não podem ser alterados, removidos ou refeitos."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Voto registrado com sucesso",
                    content = @Content(schema = @Schema(implementation = VotoResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Payload inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pauta ou sessão não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Documento já votou nesta pauta", content = @Content),
            @ApiResponse(responseCode = "422", description = "Sessão não disponível, encerrada, CPF/CNPJ inválido ou documento inapto", content = @Content),
            @ApiResponse(responseCode = "503", description = "Serviço externo de elegibilidade indisponível", content = @Content)
    })
    @PostMapping
    ResponseEntity<VotoResponse> registrar(
            @Parameter(description = "Identificador único da pauta.", example = "1", required = true)
            @PathVariable Long pautaId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Documento e voto desejado.",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RegistrarVotoRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "tipoDocumento": "CPF",
                                      "documento": "61500421381",
                                      "voto": "SIM"
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody RegistrarVotoRequest request
    );
}
