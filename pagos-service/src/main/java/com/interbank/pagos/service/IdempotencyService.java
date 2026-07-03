package com.interbank.pagos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class IdempotencyService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String PREFIJO = "idempotency:";
    private static final long TTL_HORAS = 24;

    /**
     * Revisa si una clave de idempotencia ya fue procesada.
     * Devuelve el resultado guardado, o null si es nueva.
     */
    public String obtenerResultadoPrevio(String idempotencyKey) {
        return redisTemplate.opsForValue().get(PREFIJO + idempotencyKey);
    }

    /**
     * Guarda el resultado de una operación bajo su clave de idempotencia.
     * Expira automáticamente tras 24 horas.
     */
    public void guardarResultado(String idempotencyKey, String resultado) {
        redisTemplate.opsForValue().set(
            PREFIJO + idempotencyKey,
            resultado,
            TTL_HORAS,
            TimeUnit.HOURS
        );
    }
}
