package com.interbank.transferencias.controller;

import com.interbank.transferencias.service.TransferenciaOrquestadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransferenciaController {

    private final TransferenciaOrquestadorService orquestadorService;

    public TransferenciaController(TransferenciaOrquestadorService orquestadorService) {
        this.orquestadorService = orquestadorService;
    }

    @PostMapping("/transferir")
    public ResponseEntity<String> transferir(
            @RequestHeader("Authorization") String token,
            @RequestBody TransferenciaPayload payload) {
        
        try {
            // Limpiamos el prefijo "Bearer " si el cliente lo envía en la cabecera
            String tokenLimpio = token.replace("Bearer ", "");
            
            String resultado = orquestadorService.ejecutarTransferencia(
                    tokenLimpio, 
                    payload.getMonto(), 
                    payload.getCuentaDestino()
            );
            return ResponseEntity.ok(resultado);
            
        } catch (SecurityException e) {
            // Error 401 si Auth dice que el token no vale
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (Exception e) {
            // Error 500 para cualquier otro fallo
            return ResponseEntity.internalServerError().body("Error interno en la transferencia: " + e.getMessage());
        }
    }
}

// DTO para recibir el JSON
class TransferenciaPayload {
    private double monto;
    private String cuentaDestino;
    
    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }
    public String getCuentaDestino() { return cuentaDestino; }
    public void setCuentaDestino(String cuentaDestino) { this.cuentaDestino = cuentaDestino; }
}