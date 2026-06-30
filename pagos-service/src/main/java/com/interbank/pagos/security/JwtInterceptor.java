package com.interbank.pagos.security;

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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            if (jwtUtil.validateToken(token)) {
                // Si el token es válido, extraemos los datos y los dejamos en el request
                Claims claims = jwtUtil.extractAllClaims(token);
                request.setAttribute("userId", claims.get("userId"));
                request.setAttribute("username", claims.get("sub"));
                request.setAttribute("roles", claims.get("roles"));
                return true; // ¡Déjalo pasar!
            }
        }

        // Si no hay token o es inválido, pateamos la petición con un error 401 Unauthorized
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Acceso Denegado: Token JWT ausente o invalido");
        return false;
    }
}
