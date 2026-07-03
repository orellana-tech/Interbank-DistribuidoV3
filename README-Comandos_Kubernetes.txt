# ═══════════════════════════════════════════════════════════════
#        GUÍA COMPLETA — INTERBANK-DISTRIBUIDOV3 (KUBERNETES)
# ═══════════════════════════════════════════════════════════════


# ───────────────────────────────────────────────────────────────
# ESCENARIO A: ARRANQUE NORMAL (al encender la VM)
# ───────────────────────────────────────────────────────────────
# K3s arranca automáticamente todos los pods al encender la VM.
# Solo hay que verificar y esperar a que estén listos.

# 1. Verificar que todos los pods están Running
kubectl get pods -n interbank

# 2. Si alguno no está 1/1, esperar ~2 minutos (Java arranca lento)
sleep 120
kubectl get pods -n interbank


# ───────────────────────────────────────────────────────────────
# ESCENARIO B: PRUEBAS FUNCIONALES (sin cambios de código)
# ───────────────────────────────────────────────────────────────

# 1. Generar token JWT
TOKEN=$(curl -s -X POST http://localhost/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "Token: $TOKEN"

# 2. Probar pagos
curl -i -X POST http://localhost/pagos/procesar \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"amount": 100.50, "cuentaDestino": "CUENTA-12345"}'

# 3. Probar transferencias
curl -i -X POST http://localhost/api/transferir \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"monto": 500.0, "cuentaDestino": "CTA-999"}'

# 4. Probar ping-auth (validación gRPC)
curl -i -H "Authorization: Bearer $TOKEN" \
     "http://localhost/pagos/ping-auth?token=admin"


# ───────────────────────────────────────────────────────────────
# ESCENARIO C: MEDIR TIEMPOS DE RESPUESTA
# ───────────────────────────────────────────────────────────────

TOKEN=$(curl -s -X POST http://localhost/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

echo "─── LOGIN ───"
time curl -s -X POST http://localhost/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}' > /dev/null

echo "─── PAGOS ───"
time curl -s -X POST http://localhost/pagos/procesar \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"amount": 100.50, "cuentaDestino": "CUENTA-12345"}' > /dev/null

echo "─── TRANSFERENCIAS ───"
time curl -s -X POST http://localhost/api/transferir \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"monto": 500.0, "cuentaDestino": "CTA-999"}' > /dev/null


# ───────────────────────────────────────────────────────────────
# ESCENARIO D: PRUEBA DE ESTRÉS
# ───────────────────────────────────────────────────────────────
# IMPORTANTE: editar el script antes con valores seguros
# Para VM de 3.8GB usar: ITERACIONES=200, ESPERA=0.5
# NUNCA usar 10000 peticiones a 0.1s (satura la VM)

nano ~/Interbank-DistribuidoV3/stress-test.sh
~/Interbank-DistribuidoV3/stress-test.sh


# ───────────────────────────────────────────────────────────────
# ESCENARIO E: CON CAMBIOS DE CÓDIGO (recompilar UN servicio)
# ───────────────────────────────────────────────────────────────
# Reemplaza "pagos-service" por el servicio que modificaste

cd ~/Interbank-DistribuidoV3

# 1. Compilar con Maven
mvn clean install -f pagos-service/pom.xml -DskipTests

# 2. Reconstruir la imagen Docker
docker build -t interbank-distribuidov3-pagos-service:latest ./pagos-service

# 3. Importar la nueva imagen a K3s
docker save interbank-distribuidov3-pagos-service:latest | sudo k3s ctr images import -

# 4. Reiniciar el pod para que tome la nueva imagen
kubectl rollout restart deployment/pagos-service -n interbank

# 5. Esperar y verificar
sleep 120
kubectl get pods -n interbank


# ───────────────────────────────────────────────────────────────
# ESCENARIO F: CAMBIOS EN TODOS LOS SERVICIOS (recompilación total)
# ───────────────────────────────────────────────────────────────

cd ~/Interbank-DistribuidoV3

# 1. Compilar los 4 servicios
mvn clean install -f auth-service/pom.xml -DskipTests
mvn clean install -f pagos-service/pom.xml -DskipTests
mvn clean install -f transferencia-service/pom.xml -DskipTests
mvn clean install -f api-gateway/pom.xml -DskipTests

# 2. Reconstruir las imágenes Docker
docker build -t interbank-distribuidov3-auth-service:latest ./auth-service
docker build -t interbank-distribuidov3-pagos-service:latest ./pagos-service
docker build -t interbank-distribuidov3-transferencia-service:latest ./transferencia-service

# 3. Importar las imágenes a K3s
docker save interbank-distribuidov3-auth-service:latest | sudo k3s ctr images import -
docker save interbank-distribuidov3-pagos-service:latest | sudo k3s ctr images import -
docker save interbank-distribuidov3-transferencia-service:latest | sudo k3s ctr images import -

# 4. Reiniciar todos los pods de aplicación
kubectl rollout restart deployment/auth-service -n interbank
kubectl rollout restart deployment/pagos-service -n interbank
kubectl rollout restart deployment/transferencia-service -n interbank

# 5. Esperar y verificar
sleep 180
kubectl get pods -n interbank


# ───────────────────────────────────────────────────────────────
# ACCESO A LOS DASHBOARDS DE MONITOREO
# ───────────────────────────────────────────────────────────────

# GRAFANA (acceso directo, ya tiene NodePort)
# Abrir en navegador: http://localhost:30300
#   Usuario:    admin
#   Contraseña: admin123
#   Dashboard: "Interbank - Dashboard de Microservicios"

# PROMETHEUS (requiere port-forward)
kubectl port-forward -n interbank service/prometheus 9096:9090 > /dev/null 2>&1 &
# Abrir en navegador: http://localhost:9096
#   Status → Targets (ver los 4 servicios en verde)

# Para detener el port-forward de Prometheus:
pkill -f "port-forward.*prometheus"


# ───────────────────────────────────────────────────────────────
# COMANDOS ÚTILES DE KUBERNETES
# ───────────────────────────────────────────────────────────────

kubectl get pods -n interbank                            # Ver pods
kubectl get all -n interbank                             # Ver todos los recursos
kubectl get services -n interbank                        # Ver servicios
kubectl get ingress -n interbank                         # Ver el ingress
kubectl logs -n interbank deployment/pagos-service       # Ver logs de un servicio
kubectl logs -n interbank deployment/pagos-service -f    # Logs en tiempo real
kubectl rollout restart deployment/NOMBRE -n interbank   # Reiniciar un servicio
kubectl top pods -n interbank                            # Consumo de recursos por pod
kubectl top nodes                                        # Consumo del nodo
free -h                                                  # RAM del sistema


# ───────────────────────────────────────────────────────────────
# VERIFICACIÓN DEL MONITOREO
# ───────────────────────────────────────────────────────────────

# Ver que Prometheus scrapea los 4 targets (deben estar "up")
kubectl port-forward -n interbank service/prometheus 9096:9090 > /dev/null 2>&1 &
sleep 5
curl -s http://localhost:9096/api/v1/targets | python3 -c "
import sys, json
data = json.load(sys.stdin)
for t in data['data']['activeTargets']:
    print(t['labels']['job'], '->', t['health'])
"


# ───────────────────────────────────────────────────────────────
# REGLAS IMPORTANTES
# ───────────────────────────────────────────────────────────────
# REGLA 1: Sin cambios de código → NO uses Maven ni Docker, solo los curl
# REGLA 2: Con cambios → mvn install → docker build → k3s import → rollout restart
# REGLA 3: Los pods tardan ~2 min en arrancar (Java es lento al inicio)
# REGLA 4: Al apagar/encender la VM, K3s levanta los pods automáticamente
# REGLA 5: Solo pagos-service usa Kafka (los demás no lo necesitan)
# REGLA 6: Si un pod falla → kubectl logs -n interbank deployment/NOMBRE
# REGLA 7: Rutas: /auth/ (auth), /pagos/ (pagos), /api/transferir (transferencias)
# REGLA 8: Puerto 80 (Traefik Ingress) es la puerta de entrada en Kubernetes
# REGLA 9: Prometheus NO es accesible por navegador sin port-forward
# REGLA 10: Grafana persiste dashboards aunque se reinicie (volumen PVC)
# REGLA 11: Prueba de estrés: máx 200 iteraciones / 0.5s en VM de 3.8GB
# REGLA 12: Si Grafana se congela → kubectl rollout restart deployment/grafana


# ───────────────────────────────────────────────────────────────
# PUERTOS Y ACCESOS (RESUMEN)
# ───────────────────────────────────────────────────────────────
# http://localhost/auth/login          → Login (genera JWT)
# http://localhost/pagos/procesar       → Procesar pago
# http://localhost/api/transferir       → Realizar transferencia
# http://localhost:30300                → Grafana (admin/admin123)
# http://localhost:9096                 → Prometheus (con port-forward)