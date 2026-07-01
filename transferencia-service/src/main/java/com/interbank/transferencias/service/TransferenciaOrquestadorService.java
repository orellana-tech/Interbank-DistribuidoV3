package com.interbank.transferencias.service;

import com.interbank.transferencias.util.JwtUtil;
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

    // Inyectamos el JwtUtil para decodificar el token
    @Autowired
    private JwtUtil jwtUtil;

    public String ejecutarTransferencia(String token, double monto, String cuentaDestino) {
        
        // Extraemos el ID del usuario dinámicamente del token
        String userId = jwtUtil.extractUserId(token);
        
        if (userId == null) {
            throw new SecurityException("No se pudo extraer el identificador del usuario del token.");
        }

        // Procesar pago con el ID dinámico
        PaymentRequest pagosReq = PaymentRequest.newBuilder()
                .setAmount(monto)
                .setUserId(userId) // ¡Ahora es dinámico!
                .setToken(token)    
                .build();
        
        PaymentResponse pagosRes = pagosClient.processPayment(pagosReq);

        if (!pagosRes.getSuccess()) {
            throw new RuntimeException("El pago fue rechazado por el servicio de pagos.");
        }

        return "Transacción exitosa: " + pagosRes.getTransactionId();
    }
}