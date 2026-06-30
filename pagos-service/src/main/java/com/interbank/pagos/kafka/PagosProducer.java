package com.interbank.pagos.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PagosProducer {
    
    private final KafkaTemplate<String, String> kafkaTemplate;

    public PagosProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void notificarPago(String mensaje) {
        kafkaTemplate.send("transacciones-topic", mensaje);
        System.out.println("🚀 [KAFKA] Evento enviado al broker: " + mensaje);
    }
}
