package com.global.votacao.adapter.in.web.api;

import com.global.votacao.application.dto.AtualizarPautaRequest;
import com.global.votacao.application.dto.CriarPautaRequest;
import com.global.votacao.application.dto.PautaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas")
@Tag(
        name = "Pautas",
        description = "CRUD de pautas. Pautas podem ser alteradas antes de existir voto e antes da sessão ser disponibilizada."
)
public interface IPautaController {

    @Operation(
            summary = "Cadastrar nova pauta",
            description = "Cria uma pauta ainda sem sessão de votação. Depois, uma sessão pode ser criada para essa pauta."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pauta cadastrada com sucesso", content = @Content(schema = @Schema(implementation = PautaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido", content = @Content)
    })
    @PostMapping
    ResponseEntity<PautaResponse> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Dados da pauta a ser cadastrada.",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CriarPautaRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "titulo": "Aprovação de nova política de crédito",
                                      "descricao": "Votação sobre a política proposta para o próximo ciclo."
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody CriarPautaRequest request
    );

    @Operation(
            summary = "Listar pautas",
            description = "Retorna todas as pautas cadastradas. Não retorna votos nem resultado."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Pautas retornadas com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PautaResponse.class)))
    )
    @GetMapping
    ResponseEntity<List<PautaResponse>> listar();

    @Operation(
            summary = "Buscar pauta por id",
            description = "Consulta os dados cadastrais de uma pauta específica."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pauta encontrada", content = @Content(schema = @Schema(implementation = PautaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Pauta não encontrada", content = @Content)
    })
    @GetMapping("/{pautaId}")
    ResponseEntity<PautaResponse> buscarPorId(
            @Parameter(description = "Identificador único da pauta.", example = "1", required = true)
            @PathVariable Long pautaId
    );

    @Operation(
            summary = "Atualizar pauta",
            description = "Atualiza título e descrição da pauta. Só é permitido antes de existir voto e antes da sessão vinculada ser disponibilizada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pauta atualizada com sucesso", content = @Content(schema = @Schema(implementation = PautaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pauta não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Pauta não pode ser alterada por já possuir voto ou sessão disponibilizada", content = @Content)
    })
    @PutMapping("/{pautaId}")
    ResponseEntity<PautaResponse> atualizar(
            @Parameter(description = "Identificador único da pauta.", example = "1", required = true)
            @PathVariable Long pautaId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Novos dados da pauta.",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = AtualizarPautaRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "titulo": "Aprovação revisada de política de crédito",
                                      "descricao": "Descrição ajustada antes da votação."
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody AtualizarPautaRequest request
    );

    @Operation(
            summary = "Deletar pauta",
            description = "Remove uma pauta. Só é permitido quando não houver votos nem sessão vinculada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pauta removida com sucesso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pauta não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Pauta não pode ser removida por possuir votos ou sessão vinculada", content = @Content)
    })
    @DeleteMapping("/{pautaId}")
    ResponseEntity<Void> deletar(
            @Parameter(description = "Identificador único da pauta.", example = "1", required = true)
            @PathVariable Long pautaId
    );
}
