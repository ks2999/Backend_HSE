# ДЗ — две PostgreSQL в разных сетях, pgAdmin, Flyway, HikariCP

Самодостаточная реализация задания (не зависит от стартовых файлов в `Seminar2`).

## Что внутри

| Компонент | Описание |
|-----------|----------|
| **postgres1** | Первая БД `users_db`, сеть `net1`, на хосте порт **5432** |
| **postgres2** | Вторая БД `products_db`, сеть `net2`, на хосте порт **5433** |
| **pgadmin** | Один pgAdmin, подключён к **обеим** сетям → видит обе БД (http://127.0.0.1:5050) |
| **flyway1** | Отдельный сервис миграций для `postgres1` (`migrations/db1`) |
| **flyway2** | Отдельный сервис миграций для `postgres2` (`migrations/db2`) |
| **app** | Java-приложение: два пула **HikariCP**, по паре запросов к каждой БД |

### Про «разные сети»
- `postgres1` живёт только в `net1`, `postgres2` — только в `net2`, поэтому
  **базы изолированы и не видят друг друга**.
- `pgadmin` и `app` подключены сразу к `net1` **и** `net2`, поэтому достают обе базы
  по их docker-именам (`postgres1`, `postgres2`).
- Каждый `flyway` сидит в сети своей БД.

```
        net1                         net2
   ┌───────────┐                ┌───────────┐
   │ postgres1 │                │ postgres2 │
   └─────┬─────┘                └─────┬─────┘
         │  flyway1            flyway2 │
         │                            │
      ┌──┴────────────┬───────────────┴──┐
      │   pgadmin     │       app        │   (оба контейнера в обеих сетях)
      └───────────────┴──────────────────┘
```

## Структура

```
homework/
├── docker-compose.yaml         # 2x postgres, pgadmin, 2x flyway, app
├── migrations/
│   ├── db1/                    # миграции Flyway для users_db
│   │   ├── V1__init_users.sql
│   │   └── V2__seed_users.sql
│   └── db2/                    # миграции Flyway для products_db
│       ├── V1__init_products.sql
│       └── V2__seed_products.sql
├── pgadmin/
│   └── servers.json            # обе БД зарегистрированы в pgAdmin заранее
└── app/                        # Java-приложение (Gradle + HikariCP)
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── Dockerfile
    └── src/main/java/org/example/Main.java
```

## Запуск

### Вариант 1 — всё в Docker (рекомендуется)

```bash
cd homework
docker compose up --build
```

Что произойдёт по порядку:
1. поднимутся `postgres1` и `postgres2` (ждут healthcheck);
2. `flyway1` и `flyway2` накатят миграции в свои БД;
3. `app` соберётся, дождётся успешного завершения обоих flyway и
   выведет результаты запросов к обеим базам.

Ожидаемый вывод приложения:

```
=== БД №1 (users_db) через Hikari ===
Всего пользователей: 3
  #1  john_doe     john@example.com
  #2  jane_smith   jane@example.com
  #3  bob_brown    bob@example.com

=== БД №2 (products_db) через Hikari ===
Средняя цена товара: 69.12
  #3  Monitor      199.00
  #1  Keyboard     49.99
```

### Вариант 2 — приложение с хоста

Поднять только инфраструктуру и накатить миграции:

```bash
cd homework
docker compose up -d postgres1 postgres2 pgadmin flyway1 flyway2
```

Запустить приложение локально (по умолчанию ходит на `localhost:5432` и `localhost:5433`):

```bash
cd app
./gradlew run
```

## pgAdmin — подключение к обеим БД

1. Открыть http://127.0.0.1:5050
   (десктоп-режим, логин не спрашивается).
2. Слева в группе **Homework** уже есть два сервера:
   `postgres1 (users_db)` и `postgres2 (products_db)`.
3. При первом раскрытии каждого спросит пароль — ввести `password`
   (галочка *Save password* запомнит его).

Реквизиты для ручного добавления, если потребуется:

| | postgres1 | postgres2 |
|--|--|--|
| Host (внутри docker) | `postgres1` | `postgres2` |
| Port | 5432 | 5432 |
| Database | users_db | products_db |
| Username | admin | admin |
| Password | password | password |

## Параметры подключения

| Параметр | Значение |
|----------|----------|
| Пользователь | `admin` |
| Пароль | `password` |
| БД №1 | `users_db` (хост: `localhost:5432`, docker: `postgres1:5432`) |
| БД №2 | `products_db` (хост: `localhost:5433`, docker: `postgres2:5432`) |

Приложение читает адреса из переменных окружения `DB1_URL`, `DB2_URL`,
`DB_USER`, `DB_PASSWORD` — поэтому один и тот же код работает и с хоста, и из контейнера.

## Очистка

```bash
docker compose down -v   # -v удалит тома с данными БД
```
