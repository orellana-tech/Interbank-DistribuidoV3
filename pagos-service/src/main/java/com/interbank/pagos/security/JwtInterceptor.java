package com.interbank.pagos.security;

import com.interbank.pagos.service.TokenCacheService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenCacheService tokenCacheService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Hash corto del token para usar como clave Redis
            String tokenHash = String.valueOf(token.hashCode());

            // ── 1. Revisar si el token ya está validado en Redis ──
            String datosCache = tokenCacheService.obtenerDatosToken(tokenHash);

            if (datosCache != null) {
                // CACHE HIT: usamos los datos guardados sin re-verificar la firma
                String[] partes = datosCache.split("\\|", 3);
                request.setAttribute("userId", partes[0]);
                request.setAttribute("username", partes[1]);
                request.setAttribute("roles", partes.length > 2 ? partes[2] : "");
                request.setAttribute("X-Token-Cache", "HIT");
                System.out.println("⚡ [TOKEN-CACHE] HIT - validación desde Redis");
                return true;
            }

            // ── 2. CACHE MISS: validar normalmente (trabajo criptográfico) ──
            if (jwtUtil.validateToken(token)) {
                Claims claims = jwtUtil.extractAllClaims(token);
                String userId = String.valueOf(claims.get("userId"));
                String username = String.valueOf(claims.get("sub"));
                String roles = String.valueOf(claims.get("roles"));

                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("roles", roles);

                // ── 3. Guardar en Redis para las próximas peticiones ──
                tokenCacheService.guardarToken(tokenHash, userId + "|" + username + "|" + roles);
                System.out.println("🔐 [TOKEN-CACHE] MISS - validado y guardado en Redis");

                return true;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Acceso Denegado: Token JWT ausente o invalido");
        return false;
    }
}
