# SecureStatement

SecureStatement is a Spring Boot API that generates PDF statements from customer transaction data. It stores statement metadata and transactions in PostgreSQL and returns time-limited JWT links for accessing statements.

## Project structure

```text
securestatement/
├── data/
│   └── transaction.csv              # Sample transactions loaded on first startup
├── docker/
│   └── postgres/
│       ├── DockerFile                # PostgreSQL image and initialization scripts
│       └── init/
│           └── 01-create-user.sql    # Creates the application database role
├── src/
│   └── main/
│       ├── java/com/securestatement/app/
│       │   ├── StatementApplication.java
│       │   ├── api/
│       │   │   ├── controller/       # HTTP endpoints
│       │   │   └── dto/              # API request and response types
│       │   ├── config/               # Database and OpenAPI configuration
│       │   ├── data/                 # CSV reader and startup data loader
│       │   ├── domain/
│       │   │   ├── entity/           # JPA entities
│       │   │   └── repository/       # Spring Data repositories
│       │   ├── security/             # JWT token handling
│       │   └── service/              # Statement generation logic
│       └── resources/
│           ├── application.properties
│           └── db/migration/         # Flyway database migrations
│   └── test/
│       └── java/com/securestatement/app/service/
│           └── StatementServiceTest.java
├── Dockerfile                        # Builds and packages the Spring Boot app
├── docker-compose.yml                # Runs the app and PostgreSQL
├── pom.xml                           # Maven dependencies and build settings
├── seed-data.sql                     # Optional SQL sample data
└── README.md
```

## Requirements

- Docker Desktop with Docker Compose, or Java 25 and PostgreSQL 17 for local development
- Docker Compose supplies a development-only `JWT_SECRET`; set your own secret when running outside Compose

## Build and run with Docker Compose

After cloning or saving the project into your local machine, open the command prompt/terminal and navigate to the path where your project is stored then with Docker Desktop running, run this from the repository root:

```sh
docker compose build
```

This builds the application and PostgreSQL images. No Java or Maven installation is needed on the host.

Start the application and database:

```sh
docker compose up
```

Or build and start in one command:

```sh
docker compose up --build
```

Compose starts PostgreSQL, waits for its health check, then starts the application. On first startup, the app loads the included `data/transaction.csv` into an empty transactions table. The API is available at <http://localhost:8080>.

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

Open the interactive Swagger UI at <http://localhost:8080/swagger-ui.html>.

The OpenAPI specification is available at <http://localhost:8080/v3/api-docs>.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/statements/generate/{customerId}` | Generate a statement and return a temporary time limited access link |
| `GET` | `/statements/download?token={token}` | Returns the download link to store the PDF statement to your local machine |

Generate a statement by sending ISO-8601 dates in the JSON body:

```powershell
curl.exe -X POST "http://localhost:8080/statements/generate/C101" `
  -H "Content-Type: application/json" `
  -d '{"startDate":"2026-10-01","endDate":"2026-10-31"}'
```

Generate statement using Swagger-ui
```Swagger-ui
To generate a token using the 'POST' method, Enter any customer id between 'C101 and C106' and to for example value on schema use these statement dates {"startDate":"2026-10-01","endDate":"2026-10-31"}. The generated token will be visible on the response body then you can use it to get the download link and store the statement.
Alternatively navigate to the data directory to see the sample of transactions stored in the transaction.csv file.
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

## Test

With Java 25 installed, run the Maven test lifecycle from the repository root:

```powershell
.\mvnw.cmd test
```

The Maven wrapper downloads Maven if needed. Unit tests under `src/test` use mocked repositories and temporary files, so they do not require a running PostgreSQL instance.