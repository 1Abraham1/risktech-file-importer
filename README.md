# RiskTech File Importer

## Описание проекта

Данный сервис реализует функциональность загрузки файлов формата `.csv` и `.xlsx` с последующим автоматическим созданием таблицы в PostgreSQL по структуре файла и загрузкой всех данных.

Решение полностью соответствует требованиям тестового задания: не используется никакая библиотека, автоматически создающая таблицы из файлов. Вся логика — собственная: парсеры, генерация схемы таблицы, вставка данных.

---

## Функциональность

- REST API для загрузки файла (`multipart/form-data`)
- Поддержка форматов CSV и XLSX
- Автоматическое определение заголовков
- Определение типов данных по содержимому колонок
- Генерация SQL DDL: `CREATE TABLE ...`
- Вставка всех строк в PostgreSQL через Spring JDBC
- Централизованная обработка ошибок
- Удобная и расширяемая архитектура

---

## Используемые технологии

- Java 21
- Spring Boot 4 (Web MVC, JDBC)
- PostgreSQL
- Apache Commons CSV
- Apache POI (XLSX)
- HikariCP
- Lombok

---

## Структура проекта
```
src/main/java/com/abrik/risktech
├── controller # REST-контроллер
├── service # бизнес-логика (создание таблицы, вставка данных)
├── parser # парсеры CSV и XLSX
├── model # DTO и модели метаданных
├── util # вспомогательные компоненты
├── exception # обработка ошибок
└── config # инфраструктурные конфигурации (опционально)
```
---

## API

### POST `/api/import`

Загрузка файла и выполнение импорта.

**Запрос:**  
Тип: `multipart/form-data`  
Поле: `file`

**Пример (Postman):**
- Method: `POST`
- URL: `http://localhost:8080/api/import`
- Body → form-data → key=`file`, type=File → выбрать файл

**Пример ответа:**
```JSON
{
    "tableName": "example_20251129_190048",
    "columns": [
        {"name": "client_id", "sqlType": "BIGINT"},
        {"name": "client_fio", "sqlType": "VARCHAR(255)"},
        {"name": "client_income", "sqlType": "NUMERIC"}
    ],
    "rowsInserted": 5
}
```
---

## Как запустить

1. Установить PostgreSQL и создать базу:
``` sql
CREATE DATABASE risktech; 
```

2. Настроить `application.properties`:
```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/risktech
   spring.datasource.username=postgres
   spring.datasource.password=your_password
```

3. Запустить приложение:
```bash   
mvn clean spring-boot:run
```
---

## Как удалить все таблицы (helper)
Вставьте скрипт в запросник любого графического инструмента для управления базами данных Postgres (например pgAdmin) и запустите:
```sql
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public')
    LOOP
        EXECUTE 'DROP TABLE IF EXISTS public.' || quote_ident(r.tablename) || ' CASCADE';
    END LOOP;
END $$;
```

---

## Кратко об архитектуре

- Контроллер получает файл → передаёт сервису.
- `FileImportService` определяет расширение и вызывает соответствующий парсер.
- Парсер строит `TableData` (имена колонок, типы данных, сами строки).
- `TableSchemaService` генерирует и выполняет `CREATE TABLE`.
- `DataInsertService` вставляет данные в таблицу.
- Все ошибки централизованно обрабатываются через `GlobalExceptionHandler`.

---

## Итог

Проект демонстрирует:
- чистую архитектуру слоёв,
- работу с файлами,
- динамическую работу с PostgreSQL,
- умение писать собственные парсеры,
- навыки промышленного Java/Spring-подхода.
