# ═══════════════════════════════════════════════════════════════
#        MANUAL COMPLETO — INTERBANK-DISTRIBUIDOV3 (KUBERNETES)
# ═══════════════════════════════════════════════════════════════
#
# Sistema bancario distribuido con microservicios, gRPC, Kafka,
# Kubernetes (K3s), monitoreo (Prometheus + Grafana), auto-escalado
# (HPA), cache distribuido (Redis: idempotencia + token cache),
# PostgreSQL con persistencia (PVC) y réplica (streaming replication).
#
# ═══════════════════════════════════════════════════════════════

```bash
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

nano ~/Interbank-DistribuidoV3/stress-tests/stress-test.sh
~/Interbank-DistribuidoV3/stress-tests/stress-test.sh


# ───────────────────────────────────────────────────────────────
# ESCENARIO G: VALIDAR KAFKA (eventos de pagos y transferencias)
# ───────────────────────────────────────────────────────────────

TOKEN=$(curl -s -X POST http://localhost/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -s -X POST http://localhost/api/transferir \
     -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"monto": 500.0, "cuentaDestino": "CTA-999"}' > /dev/null

# Ver el evento Kafka en los logs
kubectl logs -n interbank deployment/transferencia-service --tail 10 | grep -i "kafka\|evento"
kubectl logs -n interbank deployment/pagos-service --tail 10 | grep -i "kafka\|evento"


# ───────────────────────────────────────────────────────────────
# ESCENARIO H: HPA — AUTO-ESCALADO DE PODS
# ───────────────────────────────────────────────────────────────
# El HPA escala automáticamente los servicios stateless según el CPU:
#   auth-service:          1-5 réplicas (umbral 50% CPU)
#   pagos-service:         1-4 réplicas (umbral 60% CPU)
#   transferencia-service: 1-4 réplicas (umbral 60% CPU)

# 1. Ver el estado de los HPA (CPU actual vs umbral)
kubectl get hpa -n interbank

# 2. Aplicar los HPA (si no están creados)
kubectl apply -f ~/Interbank-DistribuidoV3/k8s/hpa-auth.yaml
kubectl apply -f ~/Interbank-DistribuidoV3/k8s/hpa-all.yaml

# 3. DEMOSTRAR EL ESCALADO EN VIVO
#    Terminal 1 — monitorear en tiempo real:
watch -n 2 'kubectl get hpa -n interbank; echo "───"; kubectl get pods -n interbank | grep -E "auth|pagos|transferencia"'

#    Terminal 2 — generar carga sobre los 3 servicios:
TOKEN=$(curl -s -X POST http://localhost/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

for i in $(seq 1 3000); do
  curl -s -X POST http://localhost/auth/login \
       -H "Content-Type: application/json" \
       -d '{"username": "admin", "password": "admin123"}' > /dev/null &
  curl -s -X POST http://localhost/pagos/procesar \
       -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
       -d '{"amount": 100, "cuentaDestino": "TEST"}' > /dev/null &
  curl -s -X POST http://localhost/api/transferir \
       -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
       -d '{"monto": 50, "cuentaDestino": "TEST"}' > /dev/null &
  if [ $((i % 30)) -eq 0 ]; then wait; echo "Ronda $i enviada..."; fi
done
# NOTA: Servicios con estado (postgres, kafka, zookeeper, prometheus,
#       grafana) NO tienen HPA por diseño.


# ───────────────────────────────────────────────────────────────
# ESCENARIO I: REDIS — IDEMPOTENCIA Y TOKEN CACHE
# ───────────────────────────────────────────────────────────────
# Redis (service: redis:6379) implementa dos patrones bancarios:
#   1. IDEMPOTENCIA → evita pagos duplicados (header Idempotency-Key)
#   2. TOKEN CACHE  → cachea validación JWT (coherente con HPA)
# NOTA: La idempotencia es OPCIONAL. Solo se activa si se envía el
#       header "Idempotency-Key". Sin ese header, funciona como siempre.

# ── VERIFICAR QUE REDIS RESPONDE ──
kubectl exec -n interbank deployment/redis -- redis-cli ping   # → PONG

# ── PROBAR IDEMPOTENCIA (evita duplicados) ──
TOKEN=$(curl -s -X POST http://localhost/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Primera petición (clave PAGO-001): se procesa normal
curl -i -X POST http://localhost/pagos/procesar \
     -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -H "Idempotency-Key: PAGO-001" \
     -d '{"amount": 100.50, "cuentaDestino": "CUENTA-12345"}'

# Segunda petición (MISMA clave): DUPLICADO desde Redis, NO reprocesa
curl -i -X POST http://localhost/pagos/procesar \
     -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -H "Idempotency-Key: PAGO-001" \
     -d '{"amount": 100.50, "cuentaDestino": "CUENTA-12345"}'

# ── PROBAR TOKEN CACHE (valida JWT una vez, reutiliza) ──
for i in 1 2 3 4 5; do
  curl -s -o /dev/null -X POST http://localhost/pagos/procesar \
       -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
       -H "Idempotency-Key: CACHE-TEST-$i" \
       -d '{"amount": 100, "cuentaDestino": "TEST"}'
done
kubectl logs -n interbank deployment/pagos-service --tail 20 | grep -i "TOKEN-CACHE"
# → 1 "MISS - validado y guardado" + varias "HIT - desde Redis"

# ── INSPECCIONAR CLAVES EN REDIS ──
kubectl exec -n interbank deployment/redis -- redis-cli KEYS "idempotency:*"
kubectl exec -n interbank deployment/redis -- redis-cli KEYS "token:*"
kubectl exec -n interbank deployment/redis -- redis-cli GET "idempotency:PAGO-001"
kubectl exec -n interbank deployment/redis -- redis-cli TTL "idempotency:PAGO-001"


# ───────────────────────────────────────────────────────────────
# ESCENARIO J: POSTGRESQL — PERSISTENCIA Y ZONA HORARIA  [NUEVO]
# ───────────────────────────────────────────────────────────────
# PostgreSQL tiene volumen persistente (PVC 2Gi): los datos sobreviven
# a reinicios y apagados de la VM. Zona horaria en America/Lima.

# ── VER LOS REGISTROS GUARDADOS ──
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "SELECT * FROM pagos ORDER BY id DESC LIMIT 10;"

# ── CONTAR REGISTROS ──
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "SELECT COUNT(*) FROM pagos;"

# ── VER LA HORA DEL SERVIDOR (debe estar en Lima, UTC-5) ──
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "SELECT now();"

# ── PROBAR PERSISTENCIA (los datos sobreviven al reinicio) ──
kubectl rollout restart deployment/postgres -n interbank
sleep 60
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "SELECT COUNT(*) FROM pagos;"
# NOTA: Solo el flujo gRPC (transferencias → pagos) guarda en BD.
#       El endpoint HTTP /pagos/procesar solo manda a Kafka, NO guarda.


# ───────────────────────────────────────────────────────────────
# ESCENARIO K: POSTGRESQL — RÉPLICA (STREAMING REPLICATION)  [NUEVO]
# ───────────────────────────────────────────────────────────────
# Arquitectura de alta disponibilidad:
#   postgres          → PRIMARIO (recibe escrituras)
#   postgres-replica  → RÉPLICA  (copia en tiempo real, solo lectura)
# Si el primario cae, la réplica tiene todos los datos.

# ── VERIFICAR ESTADO DE LA REPLICACIÓN ──

# 1. La réplica está en modo standby (solo lectura) → debe dar 't'
kubectl exec -n interbank deployment/postgres-replica -- psql -U equipo_dev -d interbank_dev -c "SELECT pg_is_in_recovery();"

# 2. Estado del streaming desde el primario → debe decir 'streaming'
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "SELECT client_addr, state, sync_state, replay_lag FROM pg_stat_replication;"

# 3. Comparar conteo en ambos (deben coincidir)
echo -n "Primario: "; kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -t -c "SELECT COUNT(*) FROM pagos;"
echo -n "Réplica:  "; kubectl exec -n interbank deployment/postgres-replica -- psql -U equipo_dev -d interbank_dev -t -c "SELECT COUNT(*) FROM pagos;"

# ── DEMOSTRAR REPLICACIÓN EN TIEMPO REAL ──
# Insertar en el primario y ver que aparece solo en la réplica
TOKEN=$(curl -s -X POST http://localhost/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
curl -s -X POST http://localhost/api/transferir \
     -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"monto": 9999.0, "cuentaDestino": "PRUEBA-REPLICA"}' > /dev/null
sleep 3
kubectl exec -n interbank deployment/postgres-replica -- psql -U equipo_dev -d interbank_dev -c "SELECT * FROM pagos WHERE monto=9999;"


# ═══════════════════════════════════════════════════════════════
# RECREAR LA RÉPLICA DESDE CERO (si hay que reconfigurarla)
# ═══════════════════════════════════════════════════════════════
# El primario (postgres:16) YA trae wal_level=replica y
# max_wal_senders=10 por defecto. Solo hay que:

# PASO 1 — Crear el usuario de replicación en el primario
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "CREATE USER replicador WITH REPLICATION LOGIN PASSWORD 'replica_pass';"

# PASO 2 — Permitir conexiones de replicación (pg_hba.conf)
kubectl exec -n interbank deployment/postgres -- bash -c "echo 'host replication replicador 0.0.0.0/0 scram-sha-256' >> /var/lib/postgresql/data/pgdata/pg_hba.conf"
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "SELECT pg_reload_conf();"

# PASO 3 — Desplegar la réplica (initContainer hace pg_basebackup automático)
kubectl apply -f ~/Interbank-DistribuidoV3/k8s/postgres-replica.yaml
sleep 40
kubectl get pods -n interbank -l app=postgres-replica



# ───────────────────────────────────────────────────────────────
# ESCENARIO L: EXPORTERS Y DASHBOARD COMPLETO v3  [NUEVO]
# ───────────────────────────────────────────────────────────────
# Dos exporters adicionales alimentan a Prometheus (6 targets en total):
#   postgres-exporter   → métricas de BD y replicación (puerto 9187)
#   kube-state-metrics  → réplicas, HPA y estado de pods (puerto 8080)
# Dashboard "Interbank — Dashboard Completo v3" (uid interbank-v3):
#   6 secciones: Estado general, Tráfico HTTP (solo negocio), Escalabilidad
#   HPA, Recursos JVM, PostgreSQL + Replicación, Kafka.
#   · Los paneles de tráfico EXCLUYEN el auto-monitoreo (/actuator/*)
#   · Contadores de peticiones REALES en el rango de tiempo visible
#   · Panel de errores muestra 0 explícito (no "No data")
#   · Refresh recomendado: 30s (10s satura Grafana en esta VM)

# Verificar los 6 targets de Prometheus (todos deben estar "up")
pkill -f "port-forward.*prometheus" 2>/dev/null
kubectl port-forward -n interbank service/prometheus 9096:9090 > /dev/null 2>&1 &
sleep 8
curl -s http://localhost:9096/api/v1/targets | python3 -c "
import sys, json
data = json.load(sys.stdin)
for t in sorted(data['data']['activeTargets'], key=lambda x: x['labels']['job']):
    print(t['labels']['job'], '->', t['health'])
"

# Verificar los exporters
kubectl get pods -n interbank -l 'app in (postgres-exporter,kube-state-metrics)'

# Reinstalar el dashboard (si hiciera falta): Grafana → Dashboards → Import
#   → Upload dashboards/interbank-dashboard-v3.json → Import (Overwrite)


# ───────────────────────────────────────────────────────────────
# ESCENARIO M: PRUEBA DE ESTRÉS PARALELO (TPS reales)  [NUEVO]
# ───────────────────────────────────────────────────────────────
# El stress-test.sh clásico es SECUENCIAL (cada curl espera al anterior):
# máximo ~5 req/s. Para medir TPS reales usa el script PARALELO, que
# alterna PAGOS y TRANSFERENCIAS y reporta el TPS al final.
# Incluye "freno": nice -n 15 (cede CPU a los servicios) + pausa configurable.
#
# Uso: ./stress-test-paralelo.sh [WORKERS] [PETICIONES_POR_WORKER] [PAUSA_MS]

# Prueba suave (recomendada para empezar): ~18 TPS, no congela la VM
~/Interbank-DistribuidoV3/stress-tests/stress-test-paralelo.sh 8 150 50

# Prueba media (si la suave respondió bien): ~30 TPS
~/Interbank-DistribuidoV3/stress-tests/stress-test-paralelo.sh 12 200 30

# ⚠️ NUNCA lanzar directo 30 workers sin pausa: congela la VM (3 CPUs
#    compartidos entre generador de carga y servicios). Escalar GRADUAL.
#    Si el load average (uptime) pasa de ~15, detener con Ctrl+C.
# Resultado de referencia en esta VM: 18.4 TPS sostenidos (8x150, pausa 50ms),
# lag de réplica < 1KB durante la carga, 0 errores HTTP.


# ───────────────────────────────────────────────────────────────
# ESCENARIO N: LIMPIAR BASE DE DATOS Y REDIS  [NUEVO]
# ───────────────────────────────────────────────────────────────
# Tras pruebas de estrés la tabla pagos acumula miles de registros.
# TRUNCATE vacía la tabla, reinicia los IDs y SE REPLICA SOLO a la réplica.

# 1. Ver cuántos registros hay en AMBAS tablas
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "SELECT 'pagos' AS tabla, COUNT(*) FROM pagos UNION ALL SELECT 'transferencias', COUNT(*) FROM transferencias;"

# 2. Vaciar AMBAS tablas (reinicia id a 1; el borrado viaja a la réplica)
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "TRUNCATE TABLE pagos RESTART IDENTITY;"
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "TRUNCATE TABLE transferencias RESTART IDENTITY;"

# 3. Verificar que primario Y réplica quedaron en 0
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "SELECT (SELECT COUNT(*) FROM pagos) AS pagos, (SELECT COUNT(*) FROM transferencias) AS transferencias;"
kubectl exec -n interbank deployment/postgres-replica -- psql -U equipo_dev -d interbank_dev -c "SELECT (SELECT COUNT(*) FROM pagos) AS pagos, (SELECT COUNT(*) FROM transferencias) AS transferencias;"

# 4. Limpiar Redis (claves de idempotencia y tokens; se regeneran solas)
kubectl exec -n interbank deployment/redis -- redis-cli FLUSHALL

# NOTA: una BD vacía pesa ~7.5MB igual (catálogo interno de PostgreSQL).
#       Es el peso base del motor, no acumulación de datos.


# ───────────────────────────────────────────────────────────────
# ESCENARIO O: LIMPIEZA DE TEMPORALES (Kubernetes y Docker)  [NUEVO]
# ───────────────────────────────────────────────────────────────
# Qué se limpia SOLO (automático):
#   · ReplicaSets viejos → revisionHistoryLimit: 2 (ya configurado)
#   · Imágenes sin usar en K3s → GC del kubelet si disco > 85%
#   · Logs de contenedores → rotación automática de K3s
#   · Claves Redis → expiran por TTL
# Qué se limpia MANUAL (ejecutar cuando se acumule):

# ReplicaSets con 0 pods (historial de despliegues)
kubectl delete replicaset -n interbank $(kubectl get replicaset -n interbank -o jsonpath='{range .items[?(@.spec.replicas==0)]}{.metadata.name} {end}')

# Imágenes huérfanas en K3s
sudo k3s crictl rmi --prune

# Imágenes Docker huérfanas y cache de builds (recupera varios GB)
docker image prune -f
docker builder prune -f

# Volúmenes Docker sin uso (datos viejos de la era Docker Compose)
docker volume prune -a -f

# Verificar espacio recuperado
docker system df
df -h /


# ───────────────────────────────────────────────────────────────
# ESCENARIO P: CONSULTAR LAS DOS TABLAS SEPARADAS  [NUEVO]
# ───────────────────────────────────────────────────────────────
# Cada endpoint guarda en su propia tabla, sin mezclas:
#   POST /pagos/procesar   → tabla pagos          (endpoint HTTP directo)
#   POST /api/transferir   → tabla transferencias (con cuenta_destino y transaction_id)
# El método gRPC processPayment ya NO guarda: delega en el llamador.

# ── Ver los últimos pagos HTTP ──
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "SELECT * FROM pagos ORDER BY id DESC LIMIT 10;"

# ── Ver las últimas transferencias ──
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "SELECT * FROM transferencias ORDER BY id DESC LIMIT 10;"

# ── Comparar totales entre primario y réplica (streaming activo) ──
echo "─── PRIMARIO ───"
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "SELECT 'pagos' AS tabla, COUNT(*) FROM pagos UNION ALL SELECT 'transferencias', COUNT(*) FROM transferencias;"
echo "─── RÉPLICA ───"
kubectl exec -n interbank deployment/postgres-replica -- psql -U equipo_dev -d interbank_dev -c "SELECT 'pagos' AS tabla, COUNT(*) FROM pagos UNION ALL SELECT 'transferencias', COUNT(*) FROM transferencias;"

# ── Suma total de dinero por tabla ──
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "SELECT 'pagos' AS tabla, SUM(monto) AS total FROM pagos UNION ALL SELECT 'transferencias', SUM(monto) FROM transferencias;"

# ── Verificar la estructura de ambas tablas (columnas) ──
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "\d pagos"
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "\d transferencias"


# ───────────────────────────────────────────────────────────────
# ACCESO A LOS DASHBOARDS DE MONITOREO
# ───────────────────────────────────────────────────────────────

# GRAFANA (acceso directo, ya tiene NodePort)
# Navegador: http://localhost:30300  (admin / admin123)
#   Dashboard: "Interbank - Dashboard de Microservicios"

# PROMETHEUS (requiere port-forward)
kubectl port-forward -n interbank service/prometheus 9096:9090 > /dev/null 2>&1 &
# Navegador: http://localhost:9096  → Status → Targets
pkill -f "port-forward.*prometheus"   # para detener


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
kubectl get hpa -n interbank                             # Ver auto-escaladores
kubectl get pvc -n interbank                             # Ver volúmenes persistentes
kubectl logs -n interbank deployment/pagos-service       # Logs de un servicio
kubectl logs -n interbank deployment/pagos-service -f    # Logs en tiempo real
kubectl rollout restart deployment/NOMBRE -n interbank   # Reiniciar un servicio
kubectl top pods -n interbank                            # Consumo por pod
kubectl top nodes                                        # Consumo del nodo
kubectl exec -n interbank deployment/redis -- redis-cli ping   # Probar Redis
free -h                                                  # RAM del sistema
uptime                                                   # Carga del sistema


# ───────────────────────────────────────────────────────────────
# SUBIR CAMBIOS A GITHUB
# ───────────────────────────────────────────────────────────────

cd ~/Interbank-DistribuidoV3

# 1. IMPORTANTE: actualizar este README antes de subir, si hubo cambios
# 2. Ver rama y cambios
git branch
git status

# 3. Crear rama nueva (opcional)
git checkout -b feature/nombre-descriptivo

# 4. Agregar archivos (el .gitignore excluye target/)
git add .

# 5. Confirmar
git commit -m "feat: descripcion del cambio"

# 6. Subir (usuario: orellana-tech + Personal Access Token)
git push origin feature/nombre-descriptivo
```

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
# REGLA 16: HPA solo aplica a servicios stateless (auth, pagos, transferencia)
# REGLA 17: La idempotencia es OPCIONAL: se activa solo con header Idempotency-Key
# REGLA 18: El token cache expira solo (5 min); las claves de idempotencia 24h
# REGLA 19: Actualizar este README antes de cada push a GitHub
# REGLA 20: Los datos de PostgreSQL persisten (PVC): NO se borran al reiniciar  [NUEVO]
# REGLA 21: Solo el flujo gRPC (transferencias) guarda en BD; HTTP /pagos solo Kafka  [NUEVO]
# REGLA 22: La réplica es SOLO LECTURA: no escribas directamente en ella  [NUEVO]
# REGLA 23: Fechas en hora de Lima (TZ en postgres, pagos y transferencia)
# REGLA 24: Dashboard v3 con refresh 30s (10s con 33+ paneles satura Grafana)  [NUEVO]
# REGLA 25: Grafana con 768Mi de límite (512Mi causaba OOMKilled / exit 137)  [NUEVO]
# REGLA 26: Estrés paralelo GRADUAL: empezar 8x150x50; nunca 30 workers directo  [NUEVO]
# REGLA 27: TRUNCATE en el primario se replica solo (también los borrados)  [NUEVO]
# REGLA 28: Los paneles de tráfico del dashboard v3 excluyen /actuator/*  [NUEVO]
# REGLA 29: Crear SIEMPRE una rama nueva en git para cada cambio/feature  [NUEVO]
# REGLA 30: El límite de TPS lo pone la VM (3 CPUs), no la arquitectura
# REGLA 31: Endpoint /pagos/procesar → guarda en tabla pagos  [NUEVO]
# REGLA 32: Endpoint /api/transferir → guarda en tabla transferencias  [NUEVO]
# REGLA 33: El gRPC processPayment ya NO persiste (solo procesa y responde)  [NUEVO]
# REGLA 34: Scripts de estrés viven en stress-tests/, no en la raíz  [NUEVO]
# REGLA 35: El dashboard de Grafana vive en dashboards/interbank-dashboard-v3.json  [NUEVO]
# REGLA 36: La instalación desde cero está en docs/README-Instalacion.md  [NUEVO]
# REGLA 37: Los recursos de todos los servicios están fijados en los YAML (fuente de verdad)  [NUEVO]
#
#
# ═══════════════════════════════════════════════════════════════
# PUERTOS Y ACCESOS (RESUMEN RÁPIDO)
# ═══════════════════════════════════════════════════════════════
# http://localhost/auth/login          → Login (genera JWT)
# http://localhost/pagos/procesar       → Procesar pago
# http://localhost/api/transferir       → Realizar transferencia
# http://localhost/ping-auth            → Validación gRPC (requiere JWT)
# http://localhost:30300                → Grafana (admin / admin123)
# http://localhost:9096                 → Prometheus (con port-forward)
#
#
# ═══════════════════════════════════════════════════════════════
# ESTADO ACTUAL DE LA ARQUITECTURA
# ═══════════════════════════════════════════════════════════════
# auth-service          → JWT + gRPC (5001 / 9090)
# pagos-service         → gRPC + Kafka [transacciones-topic] + Redis + BD tabla "pagos" (5002 / 9091)
#                         · Idempotencia (evita pagos duplicados)
#                         · Token cache distribuido (valida JWT una vez)
#                         · Endpoint HTTP /pagos/procesar guarda en tabla pagos
# transferencia-service → gRPC + Kafka [transferencias-topic] + BD tabla "transferencias" (5003 / 9092)
#                         · Guarda con cuenta_destino y transaction_id
#                         · El gRPC de pagos ya NO persiste (delega en transferencia-service)
# postgres              → PRIMARIO con persistencia (PVC 2Gi), TZ Lima (5432)
# postgres-replica      → RÉPLICA streaming (solo lectura), TZ Lima (5432)  [NUEVO]
# redis                 → Cache: idempotencia + token cache (6379)
# kafka-broker + zookeeper → Mensajería asíncrona
# kafka-exporter        → Métricas de Kafka para Prometheus (9308)
# postgres-exporter     → Métricas de BD y replicación (9187)  [NUEVO]
# kube-state-metrics    → Métricas de réplicas/HPA/pods (8080)  [NUEVO]
# prometheus            → Recolector de métricas (6 targets)
# grafana               → Dashboard Completo v3 (768Mi, PVC, refresh 30s)
# traefik (K3s)         → Ingress Controller (puerto 80)
# HPA                   → Auto-escalado de auth, pagos y transferencia
#
#
# ═══════════════════════════════════════════════════════════════
# PATRONES DE ARQUITECTURA IMPLEMENTADOS (nivel productivo bancario)
# ═══════════════════════════════════════════════════════════════
# · Microservicios desacoplados            (auth, pagos, transferencia)
# · Comunicación síncrona                  (gRPC entre servicios)
# · Comunicación asíncrona / eventos       (Kafka: 2 tópicos)
# · Autenticación centralizada             (JWT HS384)
# · Orquestación de contenedores           (Kubernetes K3s)
# · Enrutamiento / Ingress                 (Traefik puerto 80)
# · Observabilidad completa                (Prometheus + Grafana + 3 exporters)
# · Pruebas de carga con medición de TPS   (stress paralelo con control de recursos)
# · Eliminación de cold start              (startupProbe + warm-up postStart)
# · Auto-escalado horizontal               (HPA por CPU)
# · Idempotencia                           (Redis, evita cargos duplicados)
# · Cache de sesión / token distribuido    (Redis, coherente con HPA)
# · Persistencia de datos                  (PVC en PostgreSQL y Grafana)
# · Alta disponibilidad de datos           (réplica PostgreSQL streaming)
# · Zona horaria consistente               (America/Lima en todos los servicios)
