package com.interbank.auth.grpc;

// Importamos lo que está definido en el proto
import com.interbank.grpc.AuthServiceGrpc;
import com.interbank.grpc.TokenRequest;
import com.interbank.grpc.AuthResponse; // Ahora coincide con el proto

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Map;

@GrpcService
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private static final Map<String, String> VALID_TOKENS = Map.of(
        "12345", "U-100",
        "admin", "U-001",
        "test-token", "U-200"
    );

    @Override
    public void validateToken(TokenRequest request,
                              StreamObserver<AuthResponse> responseObserver) { // Cambiado a AuthResponse

        String token = request.getToken();
        System.out.println("[AuthService] Validando token: " + token);

        AuthResponse response; // Cambiado a AuthResponse

        if (VALID_TOKENS.containsKey(token)) {
            String userId = VALID_TOKENS.get(token);
            System.out.println("[AuthService] Token válido. UserId: " + userId);

            response = AuthResponse.newBuilder()
                    .setIsValid(true)
                    .setUserId(userId)
                    .build();
        } else {
            System.out.println("[AuthService] Token inválido.");
            response = AuthResponse.newBuilder()
                    .setIsValid(false)
                    .setUserId("")
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
