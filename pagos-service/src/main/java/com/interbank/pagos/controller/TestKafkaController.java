package com.interbank.pagos.controller;

import com.interbank.pagos.kafka.PagosProducer;
import com.interbank.pagos.service.IdempotencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagos")
public class TestKafkaController {

    @Autowired
    private PagosProducer productor;

    @Autowired
    private IdempotencyService idempotencyService;

    @PostMapping("/procesar")
    public ResponseEntity<String> procesarPago(
            @RequestAttribute("username") String username,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // ── IDEMPOTENCIA: si viene una clave, verificar si ya se procesó ──
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {

            String resultadoPrevio = idempotencyService.obtenerResultadoPrevio(idempotencyKey);

            if (resultadoPrevio != null) {
                // Ya existe: es un duplicado. Devolvemos el resultado guardado SIN tocar la BD.
                System.out.println("⚠️ [IDEMPOTENCIA] Petición duplicada detectada. Key: " + idempotencyKey);
                return ResponseEntity.status(HttpStatus.OK)
                        .header("X-Idempotency-Replayed", "true")
                        .body("[DUPLICADO - respuesta desde cache Redis] " + resultadoPrevio);
            }
        }

        // ── Procesamiento normal (primera vez) ──
        String mensaje = "✅ El usuario [" + username + "] acaba de realizar un pago de 100 PEN exitosamente.";

        // Disparamos el evento a Kafka
        productor.notificarPago(mensaje);

        String respuesta = "Pago procesado. El sistema de mensajería ha sido notificado.";

        // ── Guardar el resultado en Redis bajo la clave de idempotencia ──
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.guardarResultado(idempotencyKey, respuesta);
            System.out.println("💾 [IDEMPOTENCIA] Resultado guardado en Redis. Key: " + idempotencyKey);
        }

        return ResponseEntity.ok(respuesta);
    }
}
