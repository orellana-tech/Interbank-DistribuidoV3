# Stress Tests — Interbank Distribuido

Scripts para probar el rendimiento del sistema bajo carga.

## Scripts disponibles

### `stress-test.sh` — Prueba secuencial (básica)

Ejecuta peticiones una tras otra. Simple, útil para validar que todo responde.
Máximo ~5 req/s (limitado por su naturaleza secuencial).

```bash
# Editar el script para ajustar ITERACIONES y ESPERA
nano stress-test.sh

# Ejecutar
./stress-test.sh
```

### `stress-test-paralelo.sh` — Prueba paralela (mide TPS reales)

Lanza N workers concurrentes que alternan PAGOS y TRANSFERENCIAS.
Incluye control de recursos (`nice -n 15` + pausa configurable) para no saturar la VM.
Reporta el TPS real al terminar.

```bash
# Uso: ./stress-test-paralelo.sh [WORKERS] [PETICIONES] [PAUSA_MS]

# Prueba suave (recomendada para empezar): ~18 TPS
./stress-test-paralelo.sh 8 150 50

# Prueba media (si la suave respondió bien): ~30 TPS
./stress-test-paralelo.sh 12 200 30
```

## ⚠️ Advertencias importantes

- **NUNCA lanzar `30 workers` sin pausa** en una VM de 3 CPUs: congela el equipo.
- **Escalar gradual**: empezar con la prueba suave, medir, subir de a poco.
- Si el load average (comando `uptime`) supera ~15, detener con Ctrl+C.
- El techo realista de TPS en esta VM está entre **50-150 TPS** — más allá el cuello de botella es el hardware, no la arquitectura.

## Antes de ejecutar

Verificar que todos los pods estén corriendo:

```bash
kubectl get pods -n interbank
```

Los 13 pods deben estar `1/1 Running`.

## Después de ejecutar (opcional)

Ver el efecto en Grafana (`http://localhost:30300`): dashboard "Interbank — Dashboard Completo v3".
Limpiar los datos de prueba si se desea:

```bash
kubectl exec -n interbank deployment/postgres -- psql -U equipo_dev -d interbank_dev -c "TRUNCATE TABLE pagos RESTART IDENTITY;"
kubectl exec -n interbank deployment/redis -- redis-cli FLUSHALL
```
