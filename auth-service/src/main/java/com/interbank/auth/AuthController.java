package com.interbank.auth;

import com.interbank.auth.dto.LoginRequest;
import com.interbank.auth.dto.LoginResponse;
import com.interbank.auth.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    // ─────────────────────────────────────────────────────────────
    // Usuarios mock del sistema (en producción usarías una BD)
    // ─────────────────────────────────────────────────────────────
    private static final Map<String, Map<String, Object>> USERS = Map.of(
        "admin", Map.of(
            "password", "admin123",
            "userId",   "U-001",
            "roles",    List.of("ROLE_ADMIN", "ROLE_USER")
        ),
        "user1", Map.of(
            "password", "pass123",
            "userId",   "U-100",
            "roles",    List.of("ROLE_USER")
        ),
        "pagos", Map.of(
            "password", "pagos123",
            "userId",   "U-200",
            "roles",    List.of("ROLE_PAGOS", "ROLE_USER")
        )
    );

    // ─────────────────────────────────────────────────────────────
    // POST /auth/login → genera el token JWT
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        String username = request.getUsername();
        String password = request.getPassword();

        // Verificar que el usuario existe
        if (!USERS.containsKey(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario no encontrado"));
        }

        Map<String, Object> userData = USERS.get(username);

        // Verificar contraseña
        if (!userData.get("password").equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Contraseña incorrecta"));
        }

        // Generar token JWT
        String userId = (String) userData.get("userId");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) userData.get("roles");

        String token = jwtUtil.generateToken(username, userId, roles);

        System.out.println("[AuthService] Token generado para usuario: " + username);

        return ResponseEntity.ok(new LoginResponse(token, userId, username, roles));
    }

    // ─────────────────────────────────────────────────────────────
    // POST /auth/validate → valida un token JWT
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody Map<String, String> body) {

        String token = body.getOrDefault("token", "");

        if (token.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("isValid", false, "error", "Token vacío"));
        }

        boolean isValid = jwtUtil.validateToken(token);

        if (isValid) {
            return ResponseEntity.ok(Map.of(
                "isValid",  true,
                "userId",   jwtUtil.getUserId(token),
                "username", jwtUtil.getUsername(token),
                "roles",    jwtUtil.getRoles(token)
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("isValid", false, "error", "Token inválido o expirado"));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /auth/health → estado del servicio
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status",      "UP",
            "service",     "auth-service",
            "puerto_http", "5001",
            "puerto_grpc", "9090",
            "jwt",         "habilitado"
        ));
    }
}
