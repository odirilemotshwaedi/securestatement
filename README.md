# SecureStatement

SecureStatement is a Spring Boot API that generates PDF statements from customer transaction data. It stores statement metadata and transactions in PostgreSQL and returns time-limited JWT links for accessing statements.

## Requirements

- Docker Desktop with Docker Compose, or Java 25 and PostgreSQL 17 for local development
- Docker Compose supplies a development-only `JWT_SECRET`; set your own secret when running outside Compose

## Run with Docker Compose

With Docker Desktop running, run this from the repository root:

```sh
docker compose up --build
```

No Java or Maven installation is needed on the host. Compose builds the application image, starts PostgreSQL, waits for its health check, then starts the application. On first startup, the app loads the included `data/transaction.csv` into an empty transactions table. The API is available at <http://localhost:8080>.

Compose uses development-only database credentials and a development-only JWT signing secret by default. Do not use these defaults in a deployed environment. To override the JWT secret, set `JWT_SECRET` in your shell before starting Compose.

To view logs:

```sh
docker compose logs -f postgres app
```

To stop the services while retaining database data and generated statement files:

```sh
docker compose down
```

Compose stores PostgreSQL data and generated statements in named volumes. Database initialization scripts run only when PostgreSQL initializes an empty data directory. Removing volumes with `docker compose down --volumes` permanently deletes that persisted data; do this only when you intend to reset the local database and files.

## API

Interactive API documentation is available at <http://localhost:8080/swagger-ui.html>; the OpenAPI document is at <http://localhost:8080/v3/api-docs>.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/statements/` | Basic service response |
| `POST` | `/statements/generate/{customerId}` | Generate a statement and return a temporary access link |
| `GET` | `/statements/download?token={token}` | Validate a token and return the PDF file URL |
| `GET` | `/statements/file?token={token}` | Download the PDF statement

Generate a statement by sending ISO-8601 dates in the JSON body:

```powershell
curl.exe -X POST "http://localhost:8080/statements/generate/C101" `
  -H "Content-Type: application/json" `
  -d '{"startDate":"2026-10-01","endDate":"2026-10-31"}'
```

The response contains a tokenized download URL. Tokens expire after 30 minutes. Treat the tokenized URL as a secret and do not publish it.

## Transaction data

The API reads transactions from PostgreSQL's `transaction` table. Each row maps to a `Transaction` entity and has these columns:

| Column | Description |
| --- | --- |
| `transaction_id` | Source transaction identifier |
| `customer_id` | Customer associated with the transaction |
| `amount` | Transaction amount |
| `currency` | Currency code, such as `ZAR` |
| `location` | Transaction location |
| `timestamp` | Transaction date and time |

The bundled CSV contains development sample transactions for customer `C101` dated October 2026. On startup, the application imports it only when the transactions table is empty. To generate a sample statement, use a date range that includes those dates.

Flyway applies schema migrations from `src/main/resources/db/migration`. PostgreSQL records and generated PDF files persist in Compose volumes.

## Local development without Docker

Start a PostgreSQL 17 database and configure the following environment variables before running the application:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/securestatement_db"
$env:SPRING_DATASOURCE_USERNAME = "securestatement-api"
$env:SPRING_DATASOURCE_PASSWORD = "your-database-password"
$env:JWT_SECRET = [guid]::NewGuid().ToString() + [guid]::NewGuid().ToString()
.\mvnw.cmd spring-boot:run
```

The database user must have permissions to apply Flyway migrations. The PostgreSQL role and schema setup used by the Compose image is in `docker/postgres/init/`.

Run tests with:

```powershell
.\mvnw.cmd test
```