# ДЗ — использовать все типы хранилищ (+ Hazelcast)

Одно приложение по очереди обращается к **каждому** хранилищу (запись + чтение) и печатает результат.
Самодостаточно: не зависит от стартовых файлов `Seminar5`, зависимости из `mavenCentral`,
схемы таблиц приложение создаёт само (`CREATE TABLE IF NOT EXISTS`).

## Хранилища

| # | Хранилище | Тип | Как используется | Порт |
|---|-----------|-----|------------------|------|
| 1 | **PostgreSQL** | OLTP, реляционная | `JdbcTemplate`: create/insert/select | 5432 |
| 2 | **ClickHouse** | OLAP, колоночная | `JdbcTemplate`: batch insert 1000 строк + `GROUP BY` | 8123 |
| 3 | **Redis** | key-value | `StringRedisTemplate`: set/get с TTL | 6379 |
| 4 | **MongoDB** | документная | `MongoTemplate`: save/findById | 27017 |
| 5 | **S3 / MinIO** | объектная | AWS SDK v2: createBucket/putObject/getObject | 9090 |
| 6 | **Hazelcast** | in-memory data grid | embedded-член, `IMap` put/get — **доп.задание** | — |

Hazelcast подключён как **встроенный (embedded)** член — это и есть «подключить Hazelcast к проекту»
(ровно как в примере Baeldung из `Plan.md`), отдельный контейнер ему не нужен.

## Структура

```
homework/
├── docker-compose.yaml      # postgres, pgadmin, clickhouse, redis, mongodb, minio, app
├── Dockerfile               # сборка приложения (Spring Boot bootJar)
├── pgadmin/servers.json     # postgres заранее прописан в pgAdmin
└── src/main/
    ├── java/org/example/storages/
    │   ├── AllStoragesMain.java   # бины всех хранилищ + демо по каждому
    │   └── Author.java            # документ для MongoDB
    └── resources/logback.xml      # тихие логи, чтобы вывод демо был читаемым
```

## Запуск

### Вариант 1 — всё в Docker (рекомендуется)

```bash
cd homework
docker compose up --build
```

Поднимутся все хранилища и контейнер `app`, который по очереди обратится к каждому и
выведет результат. `app` отрабатывает один раз и завершается с кодом 0; остальные
сервисы продолжают работать (для pgAdmin/консоли MinIO).

### Вариант 2 — приложение с хоста

```bash
cd homework
docker compose up -d postgres pgadmin clickhouse redis mongodb minio
./gradlew bootRun
```

Каждый блок вывода помечается `[OK]` или `[FAIL] ...`. Приложение делает несколько
попыток подключения (хранилища прогреваются пару секунд), а если одно из них всё же
недоступно — остальные всё равно отработают.

Ожидаемый вывод (сокращённо):
```
===== PostgreSQL (OLTP, реляционная) =====
rows in users: 2
  #1 John Doe — Engineer
  #2 Jane Doe — Designer
[OK]
===== ClickHouse (OLAP, колоночная) =====
rows in events: 1000
aggregate (GROUP BY name):
  even = 500
  odd = 500
[OK]
===== Redis (key-value) =====
GET user:1 = John Doe  (ttl ≈ 600s)
[OK]
===== MongoDB (документная) =====
found:  Author{id='...', name='John Doe'}
[OK]
===== S3 / MinIO (объектное) =====
GET  -> Hello from MinIO/S3! ...
[OK]
===== Hazelcast (in-memory data grid) [доп.задание] =====
cluster: 1 member(s)
IMap 'demo' get(greeting) = hello
[OK]
```

## Доступы

| Хранилище | URL / параметры |
|-----------|-----------------|
| PostgreSQL | `localhost:5432` `mydatabase` `admin/password` |
| ClickHouse | `http://localhost:8123` `mydatabase` `admin/password` |
| Redis | `localhost:6379` |
| MongoDB | `mongodb://admin:password@localhost:27017/mydatabase?authSource=admin` |
| MinIO (S3) | API `http://localhost:9090`, консоль `http://localhost:9091`, `minioAccessKey/minioSecretKey` |
| pgAdmin | http://127.0.0.1:5050 (сервер `mydatabase` уже прописан, пароль `password`) |

Все параметры можно переопределить переменными окружения
(`PG_URL`, `CH_URL`, `REDIS_HOST`, `REDIS_PORT`, `MONGO_URI`, `S3_ENDPOINT`, …).

### Если порт 5432 уже занят локальным PostgreSQL

Приложение подключится к локальному серверу вместо контейнера (`FATAL: role "admin" does not exist`).
Решение: `brew services stop postgresql@15`, либо опубликовать контейнер на другом порту
(`ports: "5433:5432"`) и запустить с `PG_URL=jdbc:postgresql://localhost:5433/mydatabase ./gradlew bootRun`.

## Очистка

```bash
docker compose down -v
```
