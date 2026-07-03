package com.interbank.pagos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenCacheService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String PREFIJO = "token:";
    private static final long TTL_MINUTOS = 5;

    /**
     * Devuelve los datos cacheados de un token (formato: "userId|username|roles"),
     * o null si el token no está en cache.
     */
    public String obtenerDatosToken(String tokenHash) {
        return redisTemplate.opsForValue().get(PREFIJO + tokenHash);
    }

    /**
     * Guarda los datos de un token validado en Redis con expiración corta.
     */
    public void guardarToken(String tokenHash, String datos) {
        redisTemplate.opsForValue().set(
            PREFIJO + tokenHash,
            datos,
            TTL_MINUTOS,
            TimeUnit.MINUTES
        );
    }
}
