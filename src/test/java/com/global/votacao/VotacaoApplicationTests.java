package com.global.votacao;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VotacaoApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void deveExecutarFluxoPrincipalDeVotacao() throws Exception {
        String pautaResponse = mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "AprovaÃ§Ã£o de nova polÃ­tica de crÃ©dito",
                                  "descricao": "VotaÃ§Ã£o sobre a polÃ­tica proposta"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long pautaId = extrairId(pautaResponse);

        String sessaoResponse = mockMvc.perform(post("/api/v1/pautas/{pautaId}/sessoes/120", pautaId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pautaId", is(pautaId.intValue())))
                .andExpect(jsonPath("$.status", is("CRIADA")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long sessaoId = extrairId(sessaoResponse);

        mockMvc.perform(post("/api/v1/sessoes/{sessaoId}/disponibilizar", sessaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DISPONIVEL")));

        mockMvc.perform(post("/api/v1/pautas/{pautaId}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoDocumento": "CPF",
                                  "documento": "61500421381",
                                  "voto": "SIM"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.voto", is("SIM")));

        mockMvc.perform(post("/api/v1/pautas/{pautaId}/votos", pautaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoDocumento": "CPF",
                                  "documento": "61500421381",
                                  "voto": "NAO"
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/pautas/{pautaId}/resultado", pautaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votosSim", is(1)))
                .andExpect(jsonPath("$.votosNao", is(0)))
                .andExpect(jsonPath("$.vencedor", is("SIM")));
    }

    private Long extrairId(String json) {
        String id = json.replaceAll(".*\"id\":(\\d+).*", "$1");
        return Long.valueOf(id);
    }
}


