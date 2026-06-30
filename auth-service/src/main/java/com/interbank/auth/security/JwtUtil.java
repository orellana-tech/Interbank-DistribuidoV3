package com.interbank.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * JwtUtil.java — Clase Utilitaria JWT Reutilizable
 * ============================================================
 * INSTRUCCIONES PARA COMPAÑEROS (pagos-service, transferencia-service):
 *
 * 1. Copia este archivo a tu paquete de seguridad.
 * 2. Asegúrate de tener las dependencias jjwt en tu pom.xml.
 * 3. Asegúrate de tener en tu application.yml:
 *    jwt:
 *      secret: SW50ZXJiYW5rLVNlY3JldC1LZXktMjAyNi1NaWNyb3NlcnZpY2lvcy1BcmNoaXRlY3R1cmE=
 * 4. Inyecta esta clase con @Autowired donde la necesites.
 * ============================================================
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // ─────────────────────────────────────────
    // Obtener la clave firmada desde el secret
    // ─────────────────────────────────────────
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ─────────────────────────────────────────
    // GENERAR TOKEN JWT
    // ─────────────────────────────────────────

    /**
     * Genera un token JWT firmado con HS256.
     *
     * @param username nombre del usuario
     * @param userId   ID único del usuario
     * @param roles    lista de roles/permisos
     * @return token JWT como String
     */
    public String generateToken(String username, String userId, List<String> roles) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + (60 * 60 * 1000); // 1 hora de expiración

        return Jwts.builder()
                .setSubject(username)
                .addClaims(Map.of(
                        "userId", userId,
                        "roles",  roles,
                        "issuer", "interbank-auth-service"
                ))
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(expMillis))
                .signWith(getSigningKey())
                .compact();
    }

    // ─────────────────────────────────────────
    // VALIDAR TOKEN JWT
    // ─────────────────────────────────────────

    /**
     * Verifica si el token es válido:
     * - Firma criptográfica correcta
     * - No expirado
     *
     * @param token JWT a validar
     * @return true si es válido, false si no
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token); // si no lanza excepción, es válido
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("[JwtUtil] Token inválido: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────
    // EXTRAER DATOS DEL TOKEN
    // ─────────────────────────────────────────

    /**
     * Extrae todos los claims del token.
     */
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extrae el username (subject) del token.
     */
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extrae el userId del token.
     */
    public String getUserId(String token) {
        return getClaims(token).get("userId", String.class);
    }

    /**
     * Extrae los roles del token.
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return getClaims(token).get("roles", List.class);
    }

    /**
     * Verifica si el token ha expirado.
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
}
