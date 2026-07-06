# 🏦 Interbank-Distribuido — Guía de Instalación Completa

Este documento cubre la instalación **desde cero en una VM Debian limpia** (sin nada previo instalado). Para la operación diaria del sistema ya funcionando, ver `README-Comandos.md`.

---

## 📌 ¿Qué es este proyecto?

Sistema bancario distribuido de nivel productivo, con arquitectura orientada a microservicios, mensajería asíncrona, alta disponibilidad de datos, auto-escalado y observabilidad completa.

### Stack tecnológico

| Capa | Componentes |
|---|---|
| **Aplicación** | 3 microservicios Java 21 + Spring Boot 3.2.4 (auth, pagos, transferencia) |
| **Comunicación** | gRPC (síncrona entre servicios) + Kafka (asíncrona, 2 tópicos) |
| **Datos** | PostgreSQL 16 con **réplica en streaming** (alta disponibilidad) |
| **Cache** | Redis (idempotencia + token cache distribuido) |
| **Orquestación** | Kubernetes K3s + Traefik Ingress |
| **Escalabilidad** | HPA (auto-escalado por CPU) en los 3 servicios |
| **Observabilidad** | Prometheus + Grafana + 3 exporters (kafka, postgres, kube-state) |

### Rutas expuestas (puerto 80)

- `POST /auth/login` — Login (devuelve JWT)
- `POST /pagos/procesar` — Pago directo (guarda en tabla `pagos`)
- `POST /api/transferir` — Transferencia (guarda en tabla `transferencias`)
- `GET  /ping-auth` — Validación gRPC (requiere JWT)

### ¿Por qué Traefik y no Nginx?

Traefik viene **incluido de fábrica en K3s** como Ingress Controller. Usarlo evita instalar y mantener un componente adicional (~150Mi de RAM que Nginx sumaría) para la misma función de enrutamiento. Alineado con el objetivo del proyecto: **mínimo consumo de recursos**.

---

## 💻 Requisitos de la VM

| Recurso | Recomendado |
|---|---|
| SO | Debian 12/13 (limpio) |
| RAM | **6 GB** (mínimo 4 GB, justo para pruebas de estrés) |
| CPU | **3 cores** |
| Disco | 50 GB |
| Red | Acceso a internet |

---

## 🧹 PARTE 0 — Limpieza (solo si NO es máquina limpia)

Si el equipo tuvo una versión previa del proyecto (Docker Compose, Nginx, contenedores antiguos), limpiar primero. Si es Debian recién instalado, **saltar esta parte**.

```bash
# Detener y eliminar TODOS los contenedores
# ⚠️ Borra TODOS los contenedores; si usas Docker para otras cosas, borrar selectivamente
docker stop $(docker ps -aq) 2>/dev/null
docker rm -f $(docker ps -aq) 2>/dev/null

# Redes de docker-compose antiguos
docker network prune -f

# Imágenes de versiones antiguas
docker rmi $(docker images -q --filter "reference=*interbank*") 2>/dev/null
docker rmi $(docker images -q --filter "reference=nginx*") 2>/dev/null
docker image prune -a -f

# Si hubo un intento previo de despliegue en K8s (⚠️ borra también los PVCs)
kubectl delete namespace interbank 2>/dev/null

# Verificación de lienzo limpio
docker ps -a && docker images && kubectl get ns
```

---

## 🚀 PARTE 1 — Instalación desde cero

Ejecutar en orden. Tiempo estimado: 30-45 min (mayormente descargas).

### 1.1 Sistema base y utilidades

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y git curl wget nano tree bc python3 apt-transport-https ca-certificates gnupg
```

### 1.2 Java 21 (JDK) y Maven

```bash
sudo apt install -y openjdk-21-jdk maven

# Verificar
java -version     # openjdk 21.x
mvn -version      # Apache Maven 3.x usando Java 21
```

> Si `openjdk-21-jdk` no está en tu Debian, instalar Temurin:
> ```bash
> sudo mkdir -p /etc/apt/keyrings
> wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
> echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo $VERSION_CODENAME) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
> sudo apt update && sudo apt install -y temurin-21-jdk
> ```

### 1.3 Docker + permisos

```bash
sudo apt install -y docker.io
sudo systemctl start docker
sudo systemctl enable docker

# ⚠️ CRÍTICO: agregar tu usuario al grupo docker
# (evita "permission denied ... docker.sock")
sudo usermod -aG docker $USER

# Aplicar el cambio de grupo (o cerrar sesión y volver a entrar)
newgrp docker

# Verificar (debe responder sin sudo)
docker ps
```

### 1.4 K3s (Kubernetes) + kubectl

```bash
# Instalar K3s (incluye kubectl, Traefik y metrics-server)
curl -sfL https://get.k3s.io | sh -

# ⚠️ CRÍTICO: acceso al kubeconfig para tu usuario
# (evita "error loading config file /etc/rancher/k3s/k3s.yaml: permission denied")
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc
export KUBECONFIG=~/.kube/config

# Verificar (debe listar 1 nodo en Ready)
kubectl get nodes
```

---

## 📦 PARTE 2 — Despliegue del proyecto

### 2.1 Clonar el repositorio

```bash
cd ~
git clone https://github.com/orellana-tech/Interbank-DistribuidoV3.git
cd Interbank-DistribuidoV3
```

### 2.2 Compilar los microservicios

```bash
mvn clean install -f auth-service/pom.xml -DskipTests
mvn clean install -f pagos-service/pom.xml -DskipTests
mvn clean install -f transferencia-service/pom.xml -DskipTests
```

> La primera compilación tarda (descarga dependencias). Siguientes son rápidas.

### 2.3 Construir imágenes propias e importarlas a K3s

> **Concepto clave:** los manifiestos usan `imagePullPolicy: Never` → K3s solo usa su almacén interno (containerd), independiente de Docker. Toda imagen debe importarse ANTES del despliegue.

```bash
docker build -t interbank-distribuidov3-auth-service:latest ./auth-service
docker build -t interbank-distribuidov3-pagos-service:latest ./pagos-service
docker build -t interbank-distribuidov3-transferencia-service:latest ./transferencia-service

docker save interbank-distribuidov3-auth-service:latest | sudo k3s ctr images import -
docker save interbank-distribuidov3-pagos-service:latest | sudo k3s ctr images import -
docker save interbank-distribuidov3-transferencia-service:latest | sudo k3s ctr images import -
```

### 2.4 Importar imágenes de terceros a K3s

```bash
for IMG in postgres:16 redis:7-alpine confluentinc/cp-kafka:7.4.4 \
           confluentinc/cp-zookeeper:7.4.4 danielqsj/kafka-exporter:latest \
           prom/prometheus:latest grafana/grafana:latest \
           quay.io/prometheuscommunity/postgres-exporter:v0.15.0 \
           registry.k8s.io/kube-state-metrics/kube-state-metrics:v2.12.0; do
  echo "🚀 Importando: $IMG"
  docker pull $IMG
  docker save $IMG | sudo k3s ctr images import -
done
```

### 2.5 Desplegar todo en Kubernetes

```bash
kubectl apply -f k8s/

# Si aparecen errores "namespaces interbank not found": es una condición de carrera
# (los servicios se intentaron crear antes que el namespace).
# Ejecutar OTRA VEZ el mismo comando (es idempotente):
kubectl apply -f k8s/
```

### 2.6 Configurar la replicación de PostgreSQL (OBLIGATORIO)

> Sin este paso, `postgres-replica` queda en `Init:CrashLoopBackOff` para siempre.
> Esperar primero a que el postgres **primario** esté `1/1 Running`:
> `kubectl get pods -n interbank -l app=postgres`

```bash
# 1. Crear el usuario de replicación en el primario
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "CREATE USER replicador WITH REPLICATION LOGIN PASSWORD 'replica_pass';"

# 2. Permitir conexiones de replicación (pg_hba.conf)
kubectl exec -n interbank deployment/postgres -- bash -c "echo 'host replication replicador 0.0.0.0/0 scram-sha-256' >> /var/lib/postgresql/data/pgdata/pg_hba.conf"

# 3. Recargar la configuración
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "SELECT pg_reload_conf();"

# 4. Reiniciar la réplica para que haga su copia inicial (pg_basebackup)
kubectl rollout restart deployment/postgres-replica -n interbank
```

### 2.7 Verificación

```bash
# Esperar el arranque completo (Java tarda ~2-3 min)
sleep 180
kubectl get pods -n interbank
```

**Esperado: 13 pods en `1/1 Running`**: auth, pagos, transferencia, postgres, postgres-replica, redis, kafka-broker, zookeeper, kafka-exporter, postgres-exporter, kube-state-metrics, prometheus, grafana.

```bash
# Prueba funcional
TOKEN=$(curl -s -X POST http://localhost/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Pago HTTP (guarda en tabla pagos)
curl -i -X POST http://localhost/pagos/procesar \
     -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"amount": 250, "cuentaDestino": "TEST-001"}'

# Transferencia gRPC (guarda en tabla transferencias)
curl -i -X POST http://localhost/api/transferir \
     -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
     -d '{"monto": 500, "cuentaDestino": "TEST-002"}'
```

---

## 🎨 PARTE 3 — Importar el dashboard de Grafana

Grafana viene con el datasource preconfigurado, pero el dashboard debe importarse manualmente una sola vez.

1. Abrir `http://localhost:30300` (admin / admin123)
2. **Dashboards → New → Import → Upload JSON file**
3. Seleccionar `dashboards/interbank-dashboard-v3.json`
4. Confirmar el datasource `Prometheus` → **Import**
5. Cambiar refresh a **30s** (no 10s, satura Grafana en esta VM) y guardar con 💾

Más detalles en `dashboards/README.md`.

---

## 🚑 TROUBLESHOOTING — Runbook

Problemas reales encontrados por el equipo, con causa y solución.

### 1) `permission denied ... /var/run/docker.sock`
- **Causa:** el usuario no está en el grupo `docker`.
- **Solución:** paso 1.3 → `sudo usermod -aG docker $USER` + `newgrp docker`.

### 2) `error loading config file "/etc/rancher/k3s/k3s.yaml": permission denied`
- **Causa:** el kubeconfig de K3s solo es legible por root.
- **Solución:** paso 1.4 → copiar a `~/.kube/config` con `chown` al usuario.

### 3) `Error from server (NotFound): namespaces "interbank" not found`
- **Causa:** `kubectl apply -f k8s/` crea recursos en paralelo; los servicios intentaron crearse antes del namespace.
- **Solución:** volver a ejecutar el **mismo** `kubectl apply -f k8s/` (idempotente).

### 4) Pods en `ErrImageNeverPull` (kafka, redis, exporters, etc.)
- **Causa:** `imagePullPolicy: Never` y la imagen no está en containerd. K3s no descarga de internet; su almacén es independiente de Docker.
- **Solución:** importar las imágenes (2.3 y 2.4). Rescate automático de las que falten:
```bash
IMAGES=$(kubectl get pods -n interbank -o jsonpath="{.items[?(@.status.containerStatuses[0].state.waiting.reason=='ErrImageNeverPull')].spec.containers[0].image}" | tr ' ' '\n' | sort -u)
for IMG in $IMAGES; do
  echo "🚀 Rescatando: $IMG"
  docker pull $IMG
  docker save $IMG | sudo k3s ctr images import -
done
```

### 5) `postgres-replica` en `Init:CrashLoopBackOff`
- **Causa:** el initContainer (`pg_basebackup`) es rechazado: falta el usuario `replicador` o la regla en `pg_hba.conf`.
- **Solución:** paso 2.6 completo (usuario + pg_hba + reload + restart de la réplica).

### 6) Servicio `Running` pero `0/1` READY (ej. pagos-service)
- **Causa:** efecto dominó — el readinessProbe (Spring Actuator) reporta DOWN porque Kafka/Redis aún no responden. Suele ser consecuencia del problema 4.
- **Solución:** resolver la dependencia caída y el servicio pasa a `1/1` solo.

### 7) Grafana lento, "carita triste" o reinicios con `Exit Code 137`
- **Causa:** OOMKilled — dashboard v3 con muchos paneles excede el límite si es muy bajo o el refresh muy agresivo.
- **Solución:** límite en 768Mi (ya fijado en `k8s/grafana.yaml`) y refresh del dashboard en **30s** (no 10s).

### 9) Error de usuario o permisos en PostgreSQL (`role does not exist`, `password authentication failed`, `permission denied for table`)
- **Causa:** el usuario `equipo_dev` o `replicador` no se creó correctamente, o las credenciales del `application.yml` no coinciden con las del deployment de postgres.
- **Solución:**
```bash
# Verificar usuarios existentes en la BD
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "\du"

# Si falta equipo_dev (raro, se crea desde el YAML):
kubectl exec -n interbank deployment/postgres -- psql -U postgres -c "CREATE USER equipo_dev WITH PASSWORD 'secreta_dev' SUPERUSER;"

# Si falta el usuario replicador (para la réplica):
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "CREATE USER replicador WITH REPLICATION LOGIN PASSWORD 'replica_pass';"

# Verificar que las credenciales del deployment coinciden con las de los YAML de auth/pagos/transferencia
kubectl get deployment postgres -n interbank -o yaml | grep -A 3 "POSTGRES_USER\|POSTGRES_PASSWORD"

# Si un servicio Java falla con "password authentication failed", revisar sus env:
kubectl get deployment pagos-service -n interbank -o yaml | grep -A 2 SPRING_DATASOURCE
```
- Los valores por defecto del proyecto son: usuario `equipo_dev`, contraseña `secreta_dev`, BD `interbank_dev`.

### 8) La primera petición tras encender la VM tarda varios segundos
- **Causa:** cold start de JVM + canales gRPC + pools. Es normal.
- **Solución:** ninguna — el warm-up (postStart) precalienta al arrancar; tras 2-3 min todo responde en ~150ms.

---

## 📁 Estructura del repositorio

```
Interbank-DistribuidoV3/
├── auth-service/            # Autenticación (JWT + gRPC)
├── pagos-service/           # Pagos (gRPC + Kafka + Redis + BD tabla pagos)
├── transferencia-service/   # Transferencias (gRPC + Kafka + BD tabla transferencias)
├── api-gateway/             # Legado Docker Compose (en K8s lo cubre Traefik)
├── k8s/                     # Manifiestos de Kubernetes
├── stress-tests/            # Scripts de prueba de carga + README
├── dashboards/              # Dashboard de Grafana + README
├── docs/                    # Documentación (este archivo + README-Comandos.md)
└── README.md                # Portada del repo
```

---

## 🗄️ Notas sobre las bases de datos

### Dos tablas separadas por operación

- **Tabla `pagos`** → recibe los pagos directos vía `POST /pagos/procesar`
- **Tabla `transferencias`** → recibe las transferencias vía `POST /api/transferir` (con `cuenta_destino` y `transaction_id`)

Cada endpoint escribe en SU tabla. El método gRPC `processPayment` (llamado desde transferencia-service) ya no persiste — solo procesa y devuelve el `transactionId`. La persistencia es responsabilidad del servicio que llama.

### Replicación en tiempo real

El primario replica en streaming al pod `postgres-replica` (solo lectura, patrón alta disponibilidad). Los INSERTs y también los DELETE/TRUNCATE se propagan automáticamente.

---

## 🕐 Notas sobre zona horaria

- **Bases de datos y contenedores**: `TZ=America/Lima` (UTC-5). Los registros se guardan con hora local.
- **Headers HTTP `Date`**: siempre en **GMT** por el estándar HTTP (RFC 7231). Esto es correcto y lo hacen todos los servidores web del mundo. No es un bug: el header dice `06:35 GMT` cuando en Lima son `01:35`.

---

## 👥 Flujo de trabajo del equipo

- Cada cambio va en una rama nueva: `feature/...`, `fix/...`, `chore/...`, `docs/...`
- Los `target/` y `.jar` no se suben (`.gitignore` los excluye)
- Actualizar los README antes de cada push si hubo cambios operativos
