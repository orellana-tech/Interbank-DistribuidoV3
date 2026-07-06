package com.interbank.pagos.service;

import com.interbank.grpc.PagosServiceGrpc;
import com.interbank.grpc.PaymentRequest;
import com.interbank.grpc.PaymentResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
public class PagosServiceImpl extends PagosServiceGrpc.PagosServiceImplBase {

    @Override
    public void processPayment(PaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {

        System.out.println("========== gRPC processPayment ==========");
        System.out.println("Usuario: " + request.getUserId() + " | Monto: $" + request.getAmount());

        // 1. Simulación de validación del token
        boolean tokenValido = true;

        if (!tokenValido) {
            System.out.println("❌ ERROR: Token inválido simulado.");
            PaymentResponse response = PaymentResponse.newBuilder()
                    .setSuccess(false)
                    .setTransactionId("RECHAZADO-AUTH")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        // 2. Ya NO se guarda en BD desde aquí.
        //    El guardado ahora ocurre en el servicio que llama (transferencia-service).
        System.out.println("✅ Pago procesado (gRPC). El llamador se encarga de persistir.");

        // 3. Devolvemos el transaction ID al llamador
        PaymentResponse response = PaymentResponse.newBuilder()
                .setSuccess(true)
                .setTransactionId("TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
