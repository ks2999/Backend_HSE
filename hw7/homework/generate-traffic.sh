#!/usr/bin/env bash
# Генерирует трафик по ВСЕМ веткам if-else, чтобы наполнить метрики, трейсы и логи.
# Использование: ./generate-traffic.sh [BASE_URL]   (по умолчанию http://localhost:8080)
set -euo pipefail
BASE="${1:-http://localhost:8080}"
amounts=(-50 0 10 250 999 1500 50000 99999 100000 250000)   # invalid / small / medium / large
echo "Шлю запросы на $BASE/api/transactions/process ..."
for i in $(seq 1 40); do
  for a in "${amounts[@]}"; do
    curl -s -o /dev/null -X POST "$BASE/api/transactions/process?amount=$a" || true
  done
done
echo "Готово."
echo "  Метрики:  $BASE/actuator/prometheus"
echo "  Grafana:  http://localhost:3000  (admin/admin)"
echo "  Jaeger:   http://localhost:16686"
echo "  Kibana:   http://localhost:5601"
