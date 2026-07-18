package com.global.votacao.adapter.in.web.api;

import com.global.votacao.application.dto.AtualizarSessaoRequest;
import com.global.votacao.application.dto.CriarSessaoRequest;
import com.global.votacao.application.dto.FinalizacaoSessoesResponse;
import com.global.votacao.application.dto.SessaoVotacaoResponse;
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
@RequestMapping("/api/v1")
@Tag(
        name = "Sessões de votação",
        description = "CRUD e transições de sessão. Sessão CRIADA é configurável; sessão DISPONIVEL recebe votos e só pode ser encerrada."
)
public interface ISessaoVotacaoController {

    @Operation(
            summary = "Criar sessão ainda não disponível",
            description = "Cria uma sessão para uma pauta existente no status CRIADA. Nesse estado ela ainda não recebe votos e pode ser atualizada/removida."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sessão criada com sucesso", content = @Content(schema = @Schema(implementation = SessaoVotacaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pauta não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Pauta já possui sessão", content = @Content)
    })
    @PostMapping("/pautas/{pautaId}/sessoes")
    ResponseEntity<SessaoVotacaoResponse> criar(
            @Parameter(description = "Identificador único da pauta.", example = "1", required = true)
            @PathVariable Long pautaId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Configuração opcional da sessão. Sem body, usa duração padrão de 60 segundos.",
                    content = @Content(
                            schema = @Schema(implementation = CriarSessaoRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "duracaoEmSegundos": 120
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody(required = false) CriarSessaoRequest request
    );

    @Operation(
            summary = "Criar sessão com duração pelo path",
            description = "Cria uma sessão no status CRIADA usando a duração informada diretamente no path."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sessão criada com sucesso", content = @Content(schema = @Schema(implementation = SessaoVotacaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Duração inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pauta não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Pauta já possui sessão", content = @Content)
    })
    @PostMapping("/pautas/{pautaId}/sessoes/{duracaoEmSegundos}")
    ResponseEntity<SessaoVotacaoResponse> criarComDuracao(
            @Parameter(description = "Identificador único da pauta.", example = "1", required = true)
            @PathVariable Long pautaId,
            @Parameter(description = "Duração da sessão em segundos.", example = "120", required = true)
            @PathVariable Integer duracaoEmSegundos
    );

    @Operation(summary = "Listar sessões", description = "Retorna todas as sessões cadastradas.")
    @ApiResponse(
            responseCode = "200",
            description = "Sessões retornadas com sucesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SessaoVotacaoResponse.class)))
    )
    @GetMapping("/sessoes")
    ResponseEntity<List<SessaoVotacaoResponse>> listar();

    @Operation(summary = "Buscar sessão por id", description = "Consulta uma sessão pelo identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão encontrada", content = @Content(schema = @Schema(implementation = SessaoVotacaoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sessão não encontrada", content = @Content)
    })
    @GetMapping("/sessoes/{sessaoId}")
    ResponseEntity<SessaoVotacaoResponse> buscarPorId(
            @Parameter(description = "Identificador único da sessão.", example = "10", required = true)
            @PathVariable Long sessaoId
    );

    @Operation(
            summary = "Atualizar sessão criada",
            description = "Atualiza a duração de uma sessão. Só é permitido enquanto a sessão estiver CRIADA e sem votos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão atualizada com sucesso", content = @Content(schema = @Schema(implementation = SessaoVotacaoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sessão não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Sessão não pode mais ser alterada", content = @Content)
    })
    @PutMapping("/sessoes/{sessaoId}")
    ResponseEntity<SessaoVotacaoResponse> atualizar(
            @Parameter(description = "Identificador único da sessão.", example = "10", required = true)
            @PathVariable Long sessaoId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Nova duração da sessão.",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = AtualizarSessaoRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "duracaoEmSegundos": 180
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody AtualizarSessaoRequest request
    );

    @Operation(
            summary = "Deletar sessão criada",
            description = "Remove uma sessão. Só é permitido enquanto a sessão estiver CRIADA e sem votos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sessão removida com sucesso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Sessão não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Sessão não pode mais ser removida", content = @Content)
    })
    @DeleteMapping("/sessoes/{sessaoId}")
    ResponseEntity<Void> deletar(
            @Parameter(description = "Identificador único da sessão.", example = "10", required = true)
            @PathVariable Long sessaoId
    );

    @Operation(
            summary = "Disponibilizar sessão para votação",
            description = "Muda a sessão de CRIADA para DISPONIVEL. Depois dessa transição ela passa a receber votos e não pode mais ser alterada/removida."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão disponibilizada com sucesso", content = @Content(schema = @Schema(implementation = SessaoVotacaoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sessão não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Sessão não está em estado CRIADA", content = @Content)
    })
    @PostMapping("/sessoes/{sessaoId}/disponibilizar")
    ResponseEntity<SessaoVotacaoResponse> disponibilizar(
            @Parameter(description = "Identificador único da sessão.", example = "10", required = true)
            @PathVariable Long sessaoId
    );

    @Operation(
            summary = "Forçar encerramento de sessão",
            description = "Encerra manualmente uma sessão DISPONIVEL e publica o resultado no Kafka quando a mensageria estiver habilitada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão encerrada com sucesso", content = @Content(schema = @Schema(implementation = SessaoVotacaoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sessão não encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "Sessão CRIADA ainda não pode ser encerrada como votação", content = @Content)
    })
    @PostMapping("/sessoes/{sessaoId}/encerrar")
    ResponseEntity<SessaoVotacaoResponse> encerrar(
            @Parameter(description = "Identificador único da sessão.", example = "10", required = true)
            @PathVariable Long sessaoId
    );

    @Operation(
            summary = "Finalizar sessões vencidas",
            description = "Executa manualmente o mesmo fluxo do cron: busca até 10k sessões DISPONIVEL vencidas e até 10k sessões ENCERRADA ainda não publicadas, processando com pool-size configurável, contabilizando e publicando o resultado quando Kafka estiver habilitado."
    )
    @ApiResponse(responseCode = "200", description = "Finalização executada com sucesso", content = @Content(schema = @Schema(implementation = FinalizacaoSessoesResponse.class)))
    @PostMapping("/sessoes/finalizar")
    ResponseEntity<FinalizacaoSessoesResponse> finalizar();
}
