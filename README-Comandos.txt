# ═══════════════════════════════════════════════════════════════
#        MANUAL COMPLETO — INTERBANK-DISTRIBUIDOV3 (KUBERNETES)
# ═══════════════════════════════════════════════════════════════


# ───────────────────────────────────────────────────────────────
# ESCENARIO A: COMPILACIÓN TOTAL Y DESPLIEGUE COMPLETO
# (Primera vez, o tras cambios en varios servicios)
# ───────────────────────────────────────────────────────────────

# 1. Ir al directorio del proyecto
cd ~/Interbank-DistribuidoV3

# 2. Compilar los 4 servicios en orden
mvn clean install -f auth-service/pom.xml -DskipTests
mvn clean install -f pagos-service/pom.xml -DskipTests
mvn clean install -f transferencia-service/pom.xml -DskipTests
mvn clean install -f api-gateway/pom.xml -DskipTests

# 3. Reconstruir las imágenes Docker de los microservicios
docker build -t interbank-distribuidov3-auth-service:latest ./auth-service
docker build -t interbank-distribuidov3-pagos-service:latest ./pagos-service
docker build -t interbank-distribuidov3-transferencia-service:latest ./transferencia-service

# 4. Importar las imágenes a K3s (containerd de Kubernetes)
docker save interbank-distribuidov3-auth-service:latest | sudo k3s ctr images import -
docker save interbank-distribuidov3-pagos-service:latest | sudo k3s ctr images import -
docker save interbank-distribuidov3-transferencia-service:latest | sudo k3s ctr images import -

# 5. Reiniciar los pods de aplicación para tomar las nuevas imágenes
kubectl rollout restart deployment/auth-service -n interbank
kubectl rollout restart deployment/pagos-service -n interbank
kubectl rollout restart deployment/transferencia-service -n interbank

# 6. Esperar a que arranquen (Java tarda ~2 min por servicio)
sleep 180

# 7. Verificar que todos los pods están Running
kubectl get pods -n interbank


# ───────────────────────────────────────────────────────────────
# ESCENARIO B: ARRANQUE NORMAL (al encender la VM, sin cambios)
# ───────────────────────────────────────────────────────────────
# K3s arranca automáticamente todos los pods al encender la VM.
# Solo hay que verificar y esperar a que estén listos.

# 1. Verificar el estado de los pods
kubectl get pods -n interbank

# 2. Si alguno no está 1/1, esperar ~2 minutos
sleep 120
kubectl get pods -n interbank


# ───────────────────────────────────────────────────────────────
# ESCENARIO C: CAMBIOS EN UN SOLO SERVICIO (recompilar uno)
# Reemplaza "pagos-service" por el servicio que modificaste
# ───────────────────────────────────────────────────────────────

cd ~/Interbank-DistribuidoV3

# 1. Compilar el servicio
mvn clean install -f pagos-service/pom.xml -DskipTests

# 2. Reconstruir la imagen Docker
docker build -t interbank-distribuidov3-pagos-service:latest ./pagos-service

# 3. Importar la nueva imagen a K3s
docker save interbank-distribuidov3-pagos-service:latest | sudo k3s ctr images import -

# 4. Reiniciar el pod
kubectl rollout restart deployment/pagos-service -n interbank

# 5. Esperar y verificar
sleep 150
kubectl get pods -n interbank


# ───────────────────────────────────────────────────────────────
# ESCENARIO D: PRUEBAS FUNCIONALES (validar que todo responde)
# ───────────────────────────────────────────────────────────────

# 1. Generar token JWT
TOKEN=$(curl -s -X POST http://localhost/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "Token: $TOKEN"

# 2. Probar pagos (usa gRPC a auth + PostgreSQL + Kafka)
curl -i -X POST http://localhost/pagos/procesar \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"amount": 100.50, "cuentaDestino": "CUENTA-12345"}'

# 3. Probar transferencias (usa gRPC a auth + pagos + Kafka)
curl -i -X POST http://localhost/api/transferir \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"monto": 500.0, "cuentaDestino": "CTA-999"}'

# 4. Probar ping-auth (validación gRPC pagos → auth)
curl -i -H "Authorization: Bearer $TOKEN" \
     "http://localhost/ping-auth?token=admin"


# ───────────────────────────────────────────────────────────────
# ESCENARIO E: MEDIR TIEMPOS DE RESPUESTA
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
# ESCENARIO F: PRUEBA DE ESTRÉS
# ───────────────────────────────────────────────────────────────
# Editar el script antes con valores seguros.
# Con 6GB RAM / 3 CPUs: ITERACIONES=500, ESPERA=0.3 es seguro.
# Evita valores extremos (10000 a 0.1s) que saturan la VM.

nano ~/Interbank-DistribuidoV3/stress-test.sh
~/Interbank-DistribuidoV3/stress-test.sh


# ───────────────────────────────────────────────────────────────
# ESCENARIO G: VALIDAR KAFKA (eventos de pagos y transferencias)
# ───────────────────────────────────────────────────────────────

# Generar token y hacer una transferencia
TOKEN=$(curl -s -X POST http://localhost/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -s -X POST http://localhost/api/transferir \
     -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"monto": 500.0, "cuentaDestino": "CTA-999"}' > /dev/null

# Ver el evento Kafka en los logs de transferencias
kubectl logs -n interbank deployment/transferencia-service --tail 10 | grep -i "kafka\|evento"

# Ver el evento Kafka en los logs de pagos
kubectl logs -n interbank deployment/pagos-service --tail 10 | grep -i "kafka\|evento"


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
#   Status → Targets (ver los 4 servicios + kafka en verde)

# Detener el port-forward de Prometheus
pkill -f "port-forward.*prometheus"


# ───────────────────────────────────────────────────────────────
# VERIFICACIÓN DEL MONITOREO (targets de Prometheus)
# ───────────────────────────────────────────────────────────────

kubectl port-forward -n interbank service/prometheus 9096:9090 > /dev/null 2>&1 &
sleep 5
curl -s http://localhost:9096/api/v1/targets | python3 -c "
import sys, json
data = json.load(sys.stdin)
for t in data['data']['activeTargets']:
    print(t['labels']['job'], '->', t['health'])
"


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
uptime                                                   # Carga del sistema (load average)


# ───────────────────────────────────────────────────────────────
# SUBIR CAMBIOS A GITHUB
# ───────────────────────────────────────────────────────────────

cd ~/Interbank-DistribuidoV3

# 1. Ver en qué rama estás y qué cambió
git branch
git status

# 2. Crear una rama nueva (opcional, para features)
git checkout -b feature/nombre-descriptivo

# 3. Agregar solo los archivos de código (el .gitignore excluye target/)
git add .

# 4. Confirmar los cambios
git commit -m "feat: descripcion del cambio"

# 5. Subir a GitHub (usuario: orellana-tech + Personal Access Token)
git push origin feature/nombre-descriptivo


# ═══════════════════════════════════════════════════════════════
# REGLAS IMPORTANTES
# ═══════════════════════════════════════════════════════════════
# REGLA 1:  Sin cambios de código → NO uses Maven ni Docker, solo los curl
# REGLA 2:  Con cambios → mvn install → docker build → k3s import → rollout restart
# REGLA 3:  Los pods tardan ~2 min en arrancar (Java es lento al inicio)
# REGLA 4:  Al apagar/encender la VM, K3s levanta los pods automáticamente
# REGLA 5:  Tras encender la VM, el primer arranque es lento (cold start normal)
# REGLA 6:  pagos-service y transferencia-service usan Kafka (auth NO)
# REGLA 7:  Si un pod falla → kubectl logs -n interbank deployment/NOMBRE
# REGLA 8:  Rutas: /auth (auth), /pagos (pagos), /ping-auth (gRPC), /api/transferir
# REGLA 9:  Puerto 80 (Traefik Ingress) es la puerta de entrada en Kubernetes
# REGLA 10: Prometheus NO es accesible por navegador sin port-forward
# REGLA 11: Grafana persiste dashboards aunque se reinicie (volumen PVC)
# REGLA 12: Prueba de estrés: máx 500 iteraciones / 0.3s con 6GB RAM
# REGLA 13: Si Grafana se congela → kubectl rollout restart deployment/grafana
# REGLA 14: NUNCA subir target/ a GitHub (el .gitignore ya los excluye)
# REGLA 15: El endpoint /ping-auth requiere token JWT en el header (Bearer)


# ═══════════════════════════════════════════════════════════════
# PUERTOS Y ACCESOS (RESUMEN RÁPIDO)
# ═══════════════════════════════════════════════════════════════
# http://localhost/auth/login          → Login (genera JWT)
# http://localhost/pagos/procesar       → Procesar pago
# http://localhost/api/transferir       → Realizar transferencia
# http://localhost/ping-auth            → Validación gRPC (requiere JWT)
# http://localhost:30300                → Grafana (admin / admin123)
# http://localhost:9096                 → Prometheus (con port-forward)


# ═══════════════════════════════════════════════════════════════
# ESTADO ACTUAL DE LA ARQUITECTURA
# ═══════════════════════════════════════════════════════════════
# auth-service          → JWT + gRPC (puerto 5001 / 9090)
# pagos-service         → gRPC + Kafka [transacciones-topic] (5002 / 9091)
# transferencia-service → gRPC + Kafka [transferencias-topic] (5003 / 9092)
# postgres              → Base de datos (5432)
# kafka-broker + zookeeper → Mensajería asíncrona
# kafka-exporter        → Métricas de Kafka para Prometheus
# prometheus            → Recolector de métricas
# grafana               → Dashboards (con persistencia PVC)
# traefik (K3s)         → Ingress Controller (puerto 80)