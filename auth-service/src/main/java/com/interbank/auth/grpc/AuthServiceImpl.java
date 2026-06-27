package com.interbank.auth.grpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Map;

/**
 * Implementación del servicio gRPC AuthService.
 *
 * Lógica mock para validación de tokens:
 * - Token "12345"  → válido, userId = "U-100"
 * - Token "admin"  → válido, userId = "U-001"
 * - Cualquier otro → inválido
 */
@GrpcService
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    // Mapa mock de tokens válidos: token -> userId
    private static final Map<String, String> VALID_TOKENS = Map.of(
        "12345", "U-100",
        "admin", "U-001",
        "test-token", "U-200"
    );

    @Override
    public void validateToken(TokenRequest request,
                              StreamObserver<ValidateResponse> responseObserver) {

        String token = request.getToken();

        System.out.println("[AuthService] Validando token: " + token);

        ValidateResponse response;

        if (VALID_TOKENS.containsKey(token)) {
            // Token encontrado → respuesta válida
            String userId = VALID_TOKENS.get(token);
            System.out.println("[AuthService] Token válido. UserId: " + userId);

            response = ValidateResponse.newBuilder()
                    .setIsValid(true)
                    .setUserId(userId)
                    .build();
        } else {
            // Token no encontrado → respuesta inválida
            System.out.println("[AuthService] Token inválido.");

            response = ValidateResponse.newBuilder()
                    .setIsValid(false)
                    .setUserId("")
                    .build();
        }

        // Enviar respuesta y cerrar el stream
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
