#!/bin/bash

# ─────────────────────────────────────────
# CONFIGURACIÓN (cambia estos valores)
# ─────────────────────────────────────────
ITERACIONES=10000          # Cuántas veces ejecutar el bucle
ESPERA=0.10                # Segundos entre cada iteración
AMOUNT=100.50           # Monto para pagos
MONTO=500.0             # Monto para transferencias
CUENTA_PAGOS="CUENTA-12345"
CUENTA_TRANSFERENCIAS="CTA-999"

# URL base — Kubernetes usa el puerto 80 (Traefik Ingress)
BASE_URL="http://localhost"

# ─────────────────────────────────────────
# COLORES
# ─────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}   INTERBANK - STRESS TEST (Kubernetes) ${NC}"
echo -e "${YELLOW}   Iteraciones: $ITERACIONES | Espera: ${ESPERA}s${NC}"
echo -e "${YELLOW}========================================${NC}"

# ─────────────────────────────────────────
# PASO 1: Generar token JWT
# ─────────────────────────────────────────
echo -e "\n${YELLOW}[AUTH] Generando token JWT...${NC}"
TOKEN=$(curl -s -X POST $BASE_URL/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}' \
     | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

if [ -z "$TOKEN" ]; then
    echo -e "${RED}[ERROR] No se pudo obtener el token. ¿Están los pods activos?${NC}"
    exit 1
fi

echo -e "${GREEN}[OK] Token obtenido correctamente${NC}"

# ─────────────────────────────────────────
# BUCLE PRINCIPAL
# ─────────────────────────────────────────
for i in $(seq 1 $ITERACIONES); do
    echo -e "\n${YELLOW}─── Iteración $i/$ITERACIONES ───${NC}"

    # Renovar token cada 50 iteraciones
    if [ $((i % 50)) -eq 0 ]; then
        echo -e "${YELLOW}[AUTH] Renovando token...${NC}"
        TOKEN=$(curl -s -X POST $BASE_URL/auth/login \
             -H "Content-Type: application/json" \
             -d '{"username": "admin", "password": "admin123"}' \
             | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
        echo -e "${GREEN}[OK] Token renovado${NC}"
    fi

    # Probar pagos
    PAGO=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/pagos/procesar \
         -H "Authorization: Bearer $TOKEN" \
         -H "Content-Type: application/json" \
         -d "{\"amount\": $AMOUNT, \"cuentaDestino\": \"$CUENTA_PAGOS\"}")

    if [ "$PAGO" == "200" ]; then
        echo -e "${GREEN}[PAGOS]         HTTP $PAGO ✅${NC}"
    else
        echo -e "${RED}[PAGOS]         HTTP $PAGO ❌${NC}"
    fi

    # Probar transferencias
    TRANSFERENCIA=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/transferir \
         -H "Authorization: Bearer $TOKEN" \
         -H "Content-Type: application/json" \
         -d "{\"monto\": $MONTO, \"cuentaDestino\": \"$CUENTA_TRANSFERENCIAS\"}")

    if [ "$TRANSFERENCIA" == "200" ]; then
        echo -e "${GREEN}[TRANSFERENCIA] HTTP $TRANSFERENCIA ✅${NC}"
    else
        echo -e "${RED}[TRANSFERENCIA] HTTP $TRANSFERENCIA ❌${NC}"
    fi

    sleep $ESPERA
done

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}   TEST COMPLETADO: $ITERACIONES iteraciones${NC}"
echo -e "${GREEN}========================================${NC}"
