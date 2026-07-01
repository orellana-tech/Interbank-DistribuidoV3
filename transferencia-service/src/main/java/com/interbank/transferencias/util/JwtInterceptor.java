package com.interbank.transferencias.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired 
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Obtener el header Authorization
        String authHeader = request.getHeader("Authorization");
        
        // Validar si existe y tiene el formato Bearer <token>
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            // Validar token con JwtUtil
            if (jwtUtil.validateToken(token)) {
                return true; // Acceso permitido
            }
        }
        
        // Acceso denegado
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}