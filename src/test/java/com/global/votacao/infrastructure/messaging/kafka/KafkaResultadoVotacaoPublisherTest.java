package com.global.votacao.infrastructure.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.global.votacao.application.dto.ResultadoVotacaoResponse;
import com.global.votacao.application.event.ResultadoVotacaoEvento;
import com.global.votacao.domain.model.StatusResultado;
import com.global.votacao.domain.model.VencedorVotacao;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class KafkaResultadoVotacaoPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void devePublicarEventoSerializadoEmJsonComSucesso() {
        KafkaResultadoVotacaoPublisher publisher = new KafkaResultadoVotacaoPublisher(
                kafkaTemplate,
                new ObjectMapper(),
                "votacao.resultado.encerrado",
                true
        );
        ResultadoVotacaoEvento evento = evento();
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq("votacao.resultado.encerrado"), eq("21"), anyString()))
                .thenReturn(future);

        publisher.publicar(evento);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(
                eq("votacao.resultado.encerrado"),
                eq("21"),
                payload.capture()
        );
        assertThat(payload.getValue()).contains("\"id\":21");
        assertThat(payload.getValue()).contains("\"titulo\":\"Aprovação de nova política de crédito\"");
        assertThat(payload.getValue()).contains("\"votosSim\":1");
        assertThat(payload.getValue()).contains("\"votosNao\":1");
        assertThat(payload.getValue()).contains("\"vencedor\":\"EMPATE\"");
    }

    @Test
    void naoDevePublicarQuandoKafkaEstiverDesabilitado() {
        KafkaResultadoVotacaoPublisher publisher = new KafkaResultadoVotacaoPublisher(
                kafkaTemplate,
                new ObjectMapper(),
                "votacao.resultado.encerrado",
                false
        );

        publisher.publicar(evento());

        verify(kafkaTemplate, never()).send(
                anyString(),
                anyString(),
                anyString()
        );
    }

    private ResultadoVotacaoEvento evento() {
        ResultadoVotacaoResponse resultado = new ResultadoVotacaoResponse(
                21L,
                1L,
                1L,
                2L,
                StatusResultado.SESSAO_ENCERRADA,
                VencedorVotacao.EMPATE
        );
        return new ResultadoVotacaoEvento(
                21L,
                "Aprovação de nova política de crédito",
                "Votação sobre a política proposta para o próximo ciclo.",
                resultado
        );
    }
}
