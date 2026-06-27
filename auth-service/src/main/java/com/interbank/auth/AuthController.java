package com.interbank.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // GET /auth/health → verifica que el servicio está vivo
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "auth-service",
            "puerto_http", "5001",
            "puerto_grpc", "9090"
        ));
    }

    // POST /auth/validate → prueba de validación de token por HTTP
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, String> body) {
        String token = body.getOrDefault("token", "");

        Map<String, String> tokens = Map.of(
            "12345", "U-100",
            "admin", "U-001",
            "test-token", "U-200"
        );

        if (tokens.containsKey(token)) {
            return ResponseEntity.ok(Map.of(
                "isValid", true,
                "userId", tokens.get(token),
                "mensaje", "Token válido"
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "isValid", false,
                "userId", "",
                "mensaje", "Token inválido"
            ));
        }
    }
}
