package com.interbank.pagos.service;

import com.interbank.grpc.PagosServiceGrpc;
import com.interbank.grpc.PaymentRequest;
import com.interbank.grpc.PaymentResponse;
import com.interbank.pagos.entity.EstadoPago;
import com.interbank.pagos.entity.Pago;
import com.interbank.pagos.repository.PagoRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;

@GrpcService
public class PagosServiceImpl extends PagosServiceGrpc.PagosServiceImplBase {

    @Autowired
    private PagoRepository pagoRepository;

    @Override
    public void processPayment(PaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
        
        System.out.println("========== NUEVO INTENTO DE PAGO ==========");
        System.out.println("Usuario: " + request.getUserId() + " | Monto: $" + request.getAmount());
        
        // 1. Simulación (Mock) de Dev 1: Fingimos que validamos el token localmente
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

        // 2. Si el token es válido, guardamos el pago en PostgreSQL
        Pago nuevoPago = new Pago();
        nuevoPago.setUserId(request.getUserId());
        nuevoPago.setMonto(request.getAmount());
        nuevoPago.setEstado(EstadoPago.APROBADO);
        
        // ¡Magia de JPA! Esto hace el INSERT en la base de datos
        pagoRepository.save(nuevoPago);
        System.out.println("✅ PAGO APROBADO y guardado en BD con ID interno: " + nuevoPago.getId());

        // 3. Respondemos al que nos llamó (Dev 3 - Transferencias)
        PaymentResponse response = PaymentResponse.newBuilder()
                .setSuccess(true)
                .setTransactionId("TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
