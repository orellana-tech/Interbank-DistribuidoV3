package com.interbank.pagos.controller;

import com.interbank.pagos.kafka.PagosProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagos")
public class TestKafkaController {

    @Autowired
    private PagosProducer productor;

    @PostMapping("/procesar")
    public ResponseEntity<String> procesarPago(@RequestAttribute("username") String username) {
        // Simulamos que procesamos un cobro en la BD...
        String mensaje = "✅ El usuario [" + username + "] acaba de realizar un pago de 100 PEN exitosamente.";
        
        // Disparamos el evento a Kafka de forma asíncrona
        productor.notificarPago(mensaje);
        
        return ResponseEntity.ok("Pago procesado. El sistema de mensajería ha sido notificado.");
    }
}
