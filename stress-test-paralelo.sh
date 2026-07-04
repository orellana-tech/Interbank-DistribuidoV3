#!/bin/bash
# ─────────────────────────────────────────
# STRESS TEST PARALELO con control de recursos
# Uso: ./stress-test-paralelo.sh [WORKERS] [PETICIONES] [PAUSA_MS]
# ─────────────────────────────────────────
WORKERS=${1:-5}
PETICIONES=${2:-100}
PAUSA_MS=${3:-50}       # pausa entre peticiones de cada worker (milisegundos)
BASE_URL="http://localhost"

PAUSA=$(echo "scale=3; $PAUSA_MS / 1000" | bc)

TOKEN=$(curl -s -X POST $BASE_URL/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

[ -z "$TOKEN" ] && { echo "ERROR: sin token"; exit 1; }

TOTAL=$((WORKERS * PETICIONES))
echo "═══════════════════════════════════════"
echo " STRESS: $WORKERS workers x $PETICIONES peticiones = $TOTAL total"
echo " Pausa por worker: ${PAUSA_MS}ms | Prioridad reducida (nice)"
echo "═══════════════════════════════════════"

INICIO=$(date +%s.%N)

for w in $(seq 1 $WORKERS); do
  (
    for i in $(seq 1 $PETICIONES); do
      if [ $((i % 2)) -eq 0 ]; then
        nice -n 15 curl -s -o /dev/null --max-time 10 -X POST $BASE_URL/pagos/procesar \
             -H "Authorization: Bearer $TOKEN" \
             -H "Content-Type: application/json" \
             -d '{"amount": 10, "cuentaDestino": "STRESS"}'
      else
        nice -n 15 curl -s -o /dev/null --max-time 10 -X POST $BASE_URL/api/transferir \
             -H "Authorization: Bearer $TOKEN" \
             -H "Content-Type: application/json" \
             -d '{"monto": 10, "cuentaDestino": "STRESS"}'
      fi
      sleep $PAUSA
    done
  ) &
done
wait

FIN=$(date +%s.%N)
DURACION=$(echo "$FIN - $INICIO" | bc)
TPS=$(echo "scale=1; $TOTAL / $DURACION" | bc)

echo "═══════════════════════════════════════"
echo " Total:    $TOTAL peticiones"
echo " Duración: ${DURACION%.*} segundos"
echo " TPS REAL: $TPS peticiones/segundo"
echo "═══════════════════════════════════════"
