package com.interbank.pagos;

// Ajustamos los imports para que coincidan con el contrato del proto (AuthResponse)
import com.interbank.grpc.AuthServiceGrpc; // Paquete genérico gRPC
import com.interbank.grpc.TokenRequest;    // Mensaje definido en proto
import com.interbank.grpc.AuthResponse;    // Cambiado de ValidateResponse a AuthResponse
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authClient;

    @GetMapping("/ping-auth")
    public String hacerPing(@RequestParam String token) {
        System.out.println("Enviando token al contrato real de Dev 1...");
        
        TokenRequest request = TokenRequest.newBuilder()
                .setToken(token)
                .build();

        // Ahora usamos AuthResponse para que coincida con el proto
        AuthResponse response = authClient.validateToken(request);

        return "✅ ¡ÉXITO! Respuesta del Microservicio Auth -> ¿Es válido?: " + response.getIsValid() 
               + " | ID del Usuario: " + response.getUserId();
    }
}
