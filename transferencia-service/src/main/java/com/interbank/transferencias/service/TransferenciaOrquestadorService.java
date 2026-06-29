/*
package com.interbank.transferencias.service;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import com.interbank.auth.AuthServiceGrpc;
import com.interbank.auth.AuthRequest;
import com.interbank.auth.AuthResponse;
import com.interbank.pagos.PagosServiceGrpc;
import com.interbank.pagos.PagosRequest;
import com.interbank.pagos.PagosResponse;

@Service
public class TransferenciaOrquestadorService {

    // Cambia esto a 'false' cuando tus compañeros ya tengan sus servidores gRPC listos
    private final boolean MOCK_MODE = true; 

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authClient;

    @GrpcClient("pagos-service")
    private PagosServiceGrpc.PagosServiceBlockingStub pagosClient;

    public String ejecutarTransferencia(String token, double monto, String cuentaDestino) {
        
        // ==========================================
        // 1. PASO DE AUTENTICACIÓN
        // ==========================================
        boolean esTokenValido;
        
        if (MOCK_MODE) {
            // Simulamos que si el token es "token-invalido" falla, y cualquier otro pasa
            esTokenValido = !token.equals("token-invalido");
            System.out.println("[MOCK] Simulando validación de token. Veredicto: " + esTokenValido);
        } else {
            // Código real que llama a Auth por gRPC
            AuthRequest authReq = AuthRequest.newBuilder().setToken(token).build();
            AuthResponse authRes = authClient.validarToken(authReq);
            esTokenValido = authRes.getEsValido();
        }

        if (!esTokenValido) {
            throw new SecurityException("Token inválido o expirado. Operación denegada.");
        }

        // ==========================================
        // 2. PASO DE PAGO / TRANSFERENCIA
        // ==========================================
        String mensajeConfirmacion;
        boolean pagoExitoso;

        if (MOCK_MODE) {
            // Simulamos que el pago siempre es exitoso si estás en modo pruebas
            pagoExitoso = true;
            mensajeConfirmacion = "[MOCK] Transferencia simulada con éxito de $" + monto + " a la cuenta " + cuentaDestino;
            System.out.println(mensajeConfirmacion);
        } else {
            // Código real que llama a Pagos por gRPC
            PagosRequest pagosReq = PagosRequest.newBuilder()
                    .setMonto(monto)
                    .setCuentaDestino(cuentaDestino)
                    .build();
            PagosResponse pagosRes = pagosClient.procesarMovimiento(pagosReq);
            pagoExitoso = pagosRes.getExito();
            mensajeConfirmacion = pagosRes.getMensajeConfirmacion();
        }

        if (!pagoExitoso) {
            throw new RuntimeException("El pago fue rechazado: " + mensajeConfirmacion);
        }

        return mensajeConfirmacion;
    }
}
*/

package com.interbank.transferencias.service;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import com.interbank.auth.AuthServiceGrpc;
import com.interbank.auth.AuthRequest;
import com.interbank.auth.AuthResponse;
import com.interbank.pagos.PagosServiceGrpc;
import com.interbank.pagos.PagosRequest;
import com.interbank.pagos.PagosResponse;

@Service
public class TransferenciaOrquestadorService {

    // Inyectamos el cliente del servicio Auth
    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authClient;

    // Inyectamos el cliente del servicio Pagos
    @GrpcClient("pagos-service")
    private PagosServiceGrpc.PagosServiceBlockingStub pagosClient;

    public String ejecutarTransferencia(String token, double monto, String cuentaDestino) {
        // 1. Validar token con el microservicio de Auth
        AuthRequest authReq = AuthRequest.newBuilder().setToken(token).build();
        AuthResponse authRes = authClient.validarToken(authReq);

        if (!authRes.getEsValido()) {
            throw new SecurityException("Token inválido o expirado. Operación denegada.");
        }

        // 2. Si el token es válido, procesar el pago
        PagosRequest pagosReq = PagosRequest.newBuilder()
                .setMonto(monto)
                .setCuentaDestino(cuentaDestino)
                .build();
        PagosResponse pagosRes = pagosClient.procesarMovimiento(pagosReq);

        if (!pagosRes.getExito()) {
            throw new RuntimeException("El pago fue rechazado: " + pagosRes.getMensajeConfirmacion());
        }

        return pagosRes.getMensajeConfirmacion();
    }
}