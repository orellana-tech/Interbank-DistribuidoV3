package com.interbank.transferencias.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransferenciaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public TransferenciaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void notificarTransferencia(String mensaje) {
        kafkaTemplate.send("transferencias-topic", mensaje);
        System.out.println("🚀 [KAFKA] Evento de transferencia enviado al broker: " + mensaje);
    }
}
