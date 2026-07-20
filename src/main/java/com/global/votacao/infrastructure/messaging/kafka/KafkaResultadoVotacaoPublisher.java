package com.global.votacao.infrastructure.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.global.votacao.application.event.ResultadoVotacaoEvento;
import com.global.votacao.application.port.out.ResultadoVotacaoPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaResultadoVotacaoPublisher implements ResultadoVotacaoPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final boolean habilitado;

    public KafkaResultadoVotacaoPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${votacao.kafka.topico-resultado:votacao.resultado.encerrado}") String topic,
            @Value("${votacao.kafka.habilitado:true}") boolean habilitado
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.habilitado = habilitado;
    }

    @Override
    public void publicar(ResultadoVotacaoEvento evento) {

        if (!habilitado) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(evento);
            kafkaTemplate.send(topic, String.valueOf(evento.id()), payload).join();
            log.info("Resultado de votação enviado ao Kafka topic={} eventoId={}", topic, evento.id());
        } catch (JsonProcessingException exception) {
            log.error("Erro ao serializar evento de resultado eventoId={} evento={}", evento.id(), evento, exception);
            throw new RuntimeException("Erro ao serializar evento de resultado da votação", exception);
        } catch (RuntimeException exception) {
            log.error("Erro ao enviar evento de resultado ao Kafka topic={} eventoId={} evento={}", topic, evento.id(), evento, exception);
            throw exception;
        }
    }
}


