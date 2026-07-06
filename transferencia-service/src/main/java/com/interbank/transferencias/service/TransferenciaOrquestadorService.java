package com.interbank.transferencias.service;

import com.interbank.transferencias.util.JwtUtil;
import com.interbank.transferencias.kafka.TransferenciaProducer;
import com.interbank.transferencias.entity.Transferencia;
import com.interbank.transferencias.repository.TransferenciaRepository;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.interbank.grpc.PagosServiceGrpc;
import com.interbank.grpc.PaymentRequest;
import com.interbank.grpc.PaymentResponse;

@Service
public class TransferenciaOrquestadorService {

    @GrpcClient("pagos-service")
    private PagosServiceGrpc.PagosServiceBlockingStub pagosClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TransferenciaProducer transferenciaProducer;

    // Inyectamos el repositorio para guardar en BD
    @Autowired
    private TransferenciaRepository transferenciaRepository;

    public String ejecutarTransferencia(String token, double monto, String cuentaDestino) {

        // Extraemos el ID del usuario dinámicamente del token
        String userId = jwtUtil.extractUserId(token);

        if (userId == null) {
            throw new SecurityException("No se pudo extraer el identificador del usuario del token.");
        }

        // Procesar pago con el ID dinámico vía gRPC
        PaymentRequest pagosReq = PaymentRequest.newBuilder()
                .setAmount(monto)
                .setUserId(userId)
                .setToken(token)
                .build();

        PaymentResponse pagosRes = pagosClient.processPayment(pagosReq);

        if (!pagosRes.getSuccess()) {
            throw new RuntimeException("El pago fue rechazado por el servicio de pagos.");
        }

        String transactionId = pagosRes.getTransactionId();

        // ── NUEVO: Guardar transferencia en la BD ──
        Transferencia transferencia = new Transferencia();
        transferencia.setUserId(userId);
        transferencia.setMonto(monto);
        transferencia.setCuentaDestino(cuentaDestino);
        transferencia.setTransactionId(transactionId);
        transferencia.setEstado("APROBADO");
        transferenciaRepository.save(transferencia);
        System.out.println("💾 [BD] Transferencia guardada con ID interno: " + transferencia.getId());

        // Enviar evento a Kafka tras transferencia exitosa
        String evento = String.format(
            "{\"transactionId\":\"%s\",\"userId\":\"%s\",\"monto\":%.2f,\"cuentaDestino\":\"%s\"}",
            transactionId, userId, monto, cuentaDestino
        );
        transferenciaProducer.notificarTransferencia(evento);

        return "Transacción exitosa: " + transactionId;
    }
}
