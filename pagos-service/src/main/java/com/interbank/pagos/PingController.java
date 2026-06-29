package com.interbank.pagos;

// ¡Nuevos imports! Ahora usamos la ruta "auth.grpc" y la clase "ValidateResponse"
import com.interbank.auth.grpc.AuthServiceGrpc;
import com.interbank.auth.grpc.TokenRequest;
import com.interbank.auth.grpc.ValidateResponse;
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

        // ¡Usamos ValidateResponse tal como lo definió tu compañero!
        ValidateResponse response = authClient.validateToken(request);

        return "✅ ¡ÉXITO! Respuesta del Microservicio Auth -> ¿Es válido?: " + response.getIsValid() 
               + " | ID del Usuario: " + response.getUserId();
    }
}
