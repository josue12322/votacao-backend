package com.global.votacao.infrastructure.messaging.kafka;

import com.global.votacao.application.event.ResultadoVotacaoEvento;
import com.global.votacao.application.port.out.ResultadoVotacaoPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaResultadoVotacaoPublisher implements ResultadoVotacaoPublisher {

    private final ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider;
    private final String topic;
    private final boolean habilitado;

    public KafkaResultadoVotacaoPublisher(
            ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider,
            @Value("${votacao.kafka.topico-resultado:votacao.resultado.encerrado}") String topic,
            @Value("${votacao.kafka.habilitado:false}") boolean habilitado
    ) {
        this.kafkaTemplateProvider = kafkaTemplateProvider;
        this.topic = topic;
        this.habilitado = habilitado;
    }

    @Override
    public void publicar(ResultadoVotacaoEvento evento) {

        if (!habilitado) {
            return;
        }
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            return;
        }
        try {
            kafkaTemplate.send(topic, String.valueOf(evento.id()), evento).join();
        } catch (RuntimeException e) {
            log.error("Erro ao processa mensagem do eventoid={} para a msg= {}", evento.id(), evento);
            throw new RuntimeException(e);
        }
    }
}


