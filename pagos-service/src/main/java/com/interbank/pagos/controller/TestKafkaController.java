package com.interbank.pagos.controller;

import com.interbank.pagos.entity.EstadoPago;
import com.interbank.pagos.entity.Pago;
import com.interbank.pagos.kafka.PagosProducer;
import com.interbank.pagos.repository.PagoRepository;
import com.interbank.pagos.service.IdempotencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/pagos")
public class TestKafkaController {

    @Autowired
    private PagosProducer productor;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private PagoRepository pagoRepository;

    @PostMapping("/procesar")
    public ResponseEntity<String> procesarPago(
            @RequestAttribute("username") String username,
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) Map<String, Object> body) {

        // ── IDEMPOTENCIA: si viene una clave, verificar si ya se procesó ──
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String resultadoPrevio = idempotencyService.obtenerResultadoPrevio(idempotencyKey);
            if (resultadoPrevio != null) {
                System.out.println("⚠️ [IDEMPOTENCIA] Petición duplicada detectada. Key: " + idempotencyKey);
                return ResponseEntity.status(HttpStatus.OK)
                        .header("X-Idempotency-Replayed", "true")
                        .body("[DUPLICADO - respuesta desde cache Redis] " + resultadoPrevio);
            }
        }

        // ── Leer el monto del body (por defecto 100 si no viene) ──
        double amount = 100.0;
        if (body != null && body.get("amount") != null) {
            amount = Double.parseDouble(body.get("amount").toString());
        }

        // ── NUEVO: Guardar el pago en la BD ──
        Pago pago = new Pago();
        pago.setUserId(userId != null ? userId : username);
        pago.setMonto(amount);
        pago.setEstado(EstadoPago.APROBADO);
        pagoRepository.save(pago);
        System.out.println("💾 [BD] Pago guardado con ID interno: " + pago.getId());

        // ── Enviar evento a Kafka ──
        String mensaje = "✅ El usuario [" + username + "] realizó un pago de " + amount + " PEN. ID: " + pago.getId();
        productor.notificarPago(mensaje);

        String respuesta = "Pago procesado con ID: " + pago.getId();

        // ── Guardar el resultado en Redis bajo la clave de idempotencia ──
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.guardarResultado(idempotencyKey, respuesta);
            System.out.println("💾 [IDEMPOTENCIA] Resultado guardado en Redis. Key: " + idempotencyKey);
        }

        return ResponseEntity.ok(respuesta);
    }
}
