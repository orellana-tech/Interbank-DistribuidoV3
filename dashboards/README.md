# Dashboards de Grafana — Interbank Distribuido

Dashboards preconfigurados para importar en Grafana.

## Dashboard disponible

### `interbank-dashboard-v3.json` — Dashboard Completo v3

Vista integral del sistema con **36 paneles en 6 secciones**:

1. **🏦 Estado General** — Auth, Pagos, Transferencia, Kafka, PostgreSQL (ACTIVO/CAÍDO) + réplicas de BD conectadas
2. **📊 Tráfico HTTP y Errores** — Peticiones por segundo (solo tráfico de negocio, sin monitoreo interno), errores 4xx/5xx, latencia promedio y máxima, contadores en el rango visible
3. **📈 Escalabilidad (HPA)** — Réplicas por servicio en tiempo real, actuales vs máximo, reinicios de contenedores
4. **💾 Recursos JVM** — Memoria, CPU y threads de los 3 servicios
5. **🐘 PostgreSQL + Replicación** — Conexiones activas, tamaño de la BD, lag de replicación, transacciones/s, filas insertadas/s
6. **📨 Kafka** — Brokers activos, particiones, lag de encolamiento, mensajes/s por tópico

Datasource: Prometheus (uid `prometheus`, ya configurado en `k8s/grafana.yaml`)
Refresh recomendado: **30s** (10s satura Grafana en esta VM)

## Cómo importar

### Requisitos previos

- Grafana corriendo: `kubectl get pods -n interbank -l app=grafana` debe mostrar `1/1 Running`
- Prometheus con targets activos: los 6 targets (auth, pagos, transferencia, kafka, postgres, kube-state-metrics) deben estar en `up`

### Pasos

1. Abrir Grafana en `http://localhost:30300`
2. Credenciales por defecto: `admin` / `admin123`
3. Menú lateral → **Dashboards** → botón **New** → **Import**
4. Clic en **Upload JSON file** y seleccionar `dashboards/interbank-dashboard-v3.json`
5. Confirmar el datasource `Prometheus` (viene autoconfigurado)
6. Clic en **Import**

Si el dashboard ya existía con el mismo `uid`, Grafana pedirá confirmar **Overwrite** — aceptar.

### Ajustar refresh

Una vez importado:
- Esquina superior derecha → cambiar refresh de `10s` a **`30s`**
- Guardar con el ícono 💾 para que quede persistente

## Notas

- El dashboard usa `uid: interbank-v3` — cualquier futura versión con el mismo uid reemplaza esta.
- Grafana persiste los dashboards importados en su PVC, así que sobreviven a reinicios.
- Los paneles de tráfico HTTP **excluyen** las peticiones de auto-monitoreo (`/actuator/*`), por lo que sin tráfico real muestran `0` en lugar de una línea con carga permanente.
