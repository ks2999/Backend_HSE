# ДЗ — Observability: бизнес-логика if-else с логами, трейсами и метриками

## Что сделано

1. **Сервис с бизнес-логикой if-else** — [`TransactionService`](src/main/java/org/example/service/TransactionService.java).
   `process(amount)` разветвляется на 4 ветки, и в **каждой** ветке выставляются три сигнала:
   - **лог** (SLF4J → JSON `logback-spring.xml` → Logstash → Elasticsearch → **Kibana**);
   - **трейс** — событие `span.event("branch.*")` и теги `branch`/`outcome` на span'е
     `transaction.process` (OTLP → **Jaeger**);
   - **метрика** — счётчик `business_transactions_total{branch,outcome}`,
     гистограмма длительности `business_transaction_duration_seconds` и
     распределение сумм `business_transaction_amount` (Micrometer → Prometheus → **Grafana**).

   | Ветка | Условие | Исход |
   |-------|---------|-------|
   | `invalid` | `amount <= 0` | rejected |
   | `small`   | `< 1000` | auto-approved |
   | `medium`  | `< 100000` | approved |
   | `large`   | `>= 100000` | manual-review |

   Точка входа — [`TransactionController`](src/main/java/org/example/controller/TransactionController.java):
   `POST /api/transactions/process?amount=...`

2. **Борд в Grafana для метрик** — [`grafana/dashboards/business.json`](grafana/dashboards/business.json)
   («Business if-else metrics»): транзакции по веткам/исходам, p95 длительности, средняя сумма по веткам.
   Подхватывается автоматически через provisioning.

3. **Запрос PromQL с sum** — в [`grafana/dashboards/postgres.json`](grafana/dashboards/postgres.json)
   панель commit/rollback переведена на:
   ```promql
   sum(irate(pg_stat_database_xact_commit{instance="$instance", datname=~"$datname"}[5m]))
   ```

4. **Скриншоты** Kibana / Grafana / Jaeger — в папке [`screenshots/`](screenshots/): `grafana.png`, `jaeger.png`, `kibana.png`.

## Запуск

```bash
docker compose up -d --build        
./generate-traffic.sh              
```

| Сервис | URL |
|--------|-----|
| Приложение | http://localhost:8080/api/transactions/process?amount=500 (POST) |
| Метрики (Prometheus формат) | http://localhost:8080/actuator/prometheus |
| Grafana | http://localhost:3000 (admin/admin) |
| Jaeger | http://localhost:16686 |
| Kibana | http://localhost:5601 |
| Prometheus | http://localhost:9090 |

## Локальная сборка
```bash
./gradlew build    
```

## Скриншоты

### Grafana — дашборд «Business if-else metrics»
![Grafana — Business if-else metrics](screenshots/grafana.png)

Транзакции по веткам и по исходам (rate), доли веток (pie), p95 длительности обработки и средняя сумма транзакции по веткам.

### Jaeger — трейс обработки транзакции
![Jaeger — trace transaction.process](screenshots/jaeger.png)

Трейс `app: POST /api/transactions/process` с дочерним бизнес-span `transaction.process` (теги `branch`/`outcome`, события `branch.*`).

### Kibana — логи по веткам if-else
![Kibana — Discover logs](screenshots/kibana.png)

Discover по data view `logs-*` с фильтром `service:app` — JSON-логи приложения по веткам if-else с `trace_id` для связки с трейсами.
