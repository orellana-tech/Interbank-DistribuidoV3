package com.interbank.transferencias.service;

import com.interbank.transferencias.util.JwtUtil;
import com.interbank.transferencias.kafka.TransferenciaProducer;
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

    // Inyectamos el productor de Kafka
    @Autowired
    private TransferenciaProducer transferenciaProducer;

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

        // ── NUEVO: Enviar evento a Kafka tras transferencia exitosa ──
        String evento = String.format(
            "{\"transactionId\":\"%s\",\"userId\":\"%s\",\"monto\":%.2f,\"cuentaDestino\":\"%s\"}",
            transactionId, userId, monto, cuentaDestino
        );
        transferenciaProducer.notificarTransferencia(evento);

        return "Transacción exitosa: " + transactionId;
    }
}
