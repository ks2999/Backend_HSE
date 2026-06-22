# ДЗ — Spring JDBC → JPA, аудит и N+1

ДЗ состоит из двух частей:

1. **Перенос примеров на JPA** — все примеры из стартового
   `src/main/java/org/example/jdbc/JdbcTemplateMain.java` (Spring `JdbcTemplate`)
   переписаны на **JPA** (`EntityManager` + JPQL/native query), с сохранением имён
   методов для сверки 1:1 (транзакции, уровни изоляции, propagation).
2. **Заданное на дом** — **Spring Data JPA Auditing** и проблема **N+1**
   (см. раздел [«На дом»](#на-дом--spring-audit-и-n1)).

Самодостаточно: не зависит от стартовых файлов `Seminar4`. Зависимости берутся
из `mavenCentral`, схему БД создаёт Hibernate (`ddl-auto=update`).

## Соответствие JDBC → JPA

| JDBC (`JdbcTemplate`) | JPA (`EntityManager`) |
|---|---|
| `jdbcTemplate.update("INSERT ...")` | `em.persist(entity)` |
| `jdbcTemplate.execute("DELETE FROM users")` | `em.createQuery("DELETE FROM User").executeUpdate()` |
| `jdbcTemplate.query("SELECT ...", rowMapper)` | `em.createQuery("SELECT u FROM User u", User.class)` |
| `queryForObject("SELECT COUNT(*) ...")` | `SELECT COUNT(u) FROM User u` |
| `string_agg(...)` | native query через `em.createNativeQuery(...)` |
| `batchUpdate(...)` | `users.forEach(em::persist)` + `hibernate.jdbc.batch_size` |
| `TransactionTemplate` (+ isolation/propagation) | то же, менеджер транзакций — `JpaTransactionManager` |

## Что воспроизведено (методы в `JdbcExamplesInJpa`)

- **CRUD / запросы:** `insertUser`, `insertMultipleUsers`, `printUsers`, `countUsers`, `concatUsersName`, `clearUsers`
- **Транзакции:** `insertWithTransaction`, `insertWithTransactionRollback`, `testReadNotCommited`
- **Уровни изоляции:** `notDirtyRead`, `dirtyRead`, `repeatableRead`, `notRepeatableRead`, `phantomRead`, `notPhantomRead`, `anomalyExample` (write skew), `notAnomalyExample` (SERIALIZABLE)
- **Propagation:** `propagationRequired`, `propagationSupport`, `propagationRequiredNew`

## На дом — Spring Audit и N+1

Обе части запускаются аргументом и не мешают примерам выше.

### 1. Spring Data JPA Auditing (`--args="audit"`)

Пакет `org.example.jpa.audit`. Базовый класс `Auditable` (`@MappedSuperclass` +
`@EntityListeners(AuditingEntityListener.class)`) добавляет сущности `Article`
поля `createdDate / lastModifiedDate / createdBy / lastModifiedBy`. Аудит включён
через `@EnableJpaAuditing` (`AuditConfig`), «текущего пользователя» отдаёт бин
`AuditorAware` (в реальном проекте — из Spring Security).

Демо создаёт статью от имени `alice`, затем меняет её от имени `bob`:

```
после создания (пользователь alice):
  createdBy=alice,  createdDate=...968744Z
  lastModifiedBy=alice, lastModifiedDate=...968744Z
после изменения (пользователь bob):
  createdBy=alice,  createdDate=...968744Z          <- НЕ изменились
  lastModifiedBy=bob, lastModifiedDate=...111497Z   <- обновились
```

### 2. Проблема N+1 (`--args="nplus1"`)

Пакет `org.example.jpa.nplus1`. Связь `Author` 1—∞ `Book` (ленивая). Реальное
число SQL-запросов считается через Hibernate `Statistics`:

| Подход | Запросов (3 автора × 3 книги) |
|--------|------------------------------|
| Ленивая загрузка в цикле (N+1) | **4** (1 + N) |
| `JOIN FETCH` (`findAllWithBooksFetch`) | **1** |
| `@EntityGraph` (`findAllWithBooksGraph`) | **1** |

Третий приём — `@BatchSize` / `hibernate.default_batch_fetch_size`: ленивые
коллекции догружаются пачками (N запросов → ⌈N / размер пачки⌉). В демо показаны
первые два как самые наглядные «в один запрос».

## Важные отличия JPA от «голого» JDBC (отражены в коде комментариями)

1. **Запись требует транзакции.** У `JdbcTemplate` каждый `update` вне транзакции
   авто-коммитился; в JPA `persist` без активной транзакции невозможен. Поэтому
   `propagationSupport` вложен во внешнюю транзакцию (а не пишет «вне транзакции»).
2. **Кэш первого уровня (persistence context).** Повторное чтение управляемой сущности
   в одной транзакции вернёт её из кэша. Чтобы увидеть неповторяющееся чтение, перед
   повторным запросом делаем `em.clear()` (см. `repeatableRead`/`notRepeatableRead`).

## Запуск

1. Поднять БД и pgAdmin:
   ```bash
   cd homework
   docker compose up -d
   ```
2. Запустить нужную часть:
   ```bash
   ./gradlew bootRun --args="audit"    # Spring Data JPA Auditing
   ./gradlew bootRun --args="nplus1"   # проблема N+1 и её решения
   ./gradlew bootRun                   # примеры JDBC -> JPA
   ```
   Для примеров JDBC→JPA конкретный метод выбирается раскомментированием строки в
   `JdbcExamplesInJpa.main(...)` (по умолчанию активен `insertUser`).

pgAdmin: http://127.0.0.1:5050 (без логина). Сервер `mydatabase` уже прописан,
при первом подключении ввести пароль `password`.

### Параметры подключения

| | значение |
|--|--|
| URL | `jdbc:postgresql://localhost:5432/mydatabase` |
| User / Password | `admin` / `password` |

### Если порт 5432 уже занят (например, локальный PostgreSQL)

Если на машине уже крутится свой PostgreSQL на 5432, приложение подключится к нему,
а не к контейнеру (ошибка вида `FATAL: role "admin" does not exist`). Варианты:

- остановить локальный сервер: `brew services stop postgresql@15`, либо
- не меняя файлов, указать другой порт через переменную окружения, заранее
  опубликовав контейнер на нём (`docker compose` → `ports: "5433:5432"`):
  ```bash
  SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/mydatabase ./gradlew bootRun
  ```

## Очистка

```bash
docker compose down -v
```
