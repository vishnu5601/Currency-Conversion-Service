# Currency Orders API

A Quarkus microservice that accepts an order in USD, converts it to a target currency using a live exchange rate, persists it, and streams it into a searchable analytics pipeline — all fully containerized.

```
Client → Quarkus (REST) → PostgreSQL (system of record)
                        ↘ Kafka → Consumer → OpenSearch → OpenSearch Dashboards (live analytics)
```

The two writes above are independent: Postgres holds the durable record of every order, while the Kafka → OpenSearch branch is a separate, asynchronous copy optimized for search and dashboards.

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [Configuration](#configuration)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)
- [OpenSearch Dashboards](#opensearch-dashboards)
- [Troubleshooting](#troubleshooting)

## Features

- `POST /api/v1/orders` — accepts a customer ID, USD amount, and target currency; returns the converted amount
- `GET /api/v1/orders/{id}` — fetch a single order
- `GET /api/v1/orders?customerId=...` — list orders, optionally filtered by customer
- Live exchange rate lookups via [open.er-api.com](https://www.exchangerate-api.com), using Quarkus REST Client Reactive
- Durable persistence in PostgreSQL (Flyway-managed schema)
- Event-driven indexing: every order publishes an `ORDER_CREATED` event to Kafka, consumed and indexed into OpenSearch
- A live OpenSearch Dashboards board showing total revenue, revenue trend, popular currencies, and top customers
- Consistent, structured JSON error responses for validation failures, unknown currencies, and upstream API failures
- Unit tests (`@QuarkusTest` + `@InjectMock`) covering conversion math and failure handling
- Integration tests (REST Assured) covering the full HTTP surface, including error paths
- Multi-stage Docker build and a `docker-compose.yml` that stands up the entire stack

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21, Quarkus 3.39 |
| REST | RESTEasy Reactive |
| External API client | Quarkus REST Client Reactive |
| Persistence | PostgreSQL 16, Hibernate ORM with Panache, Flyway |
| Messaging | Apache Kafka (SmallRye Reactive Messaging) |
| Search / Analytics | OpenSearch 2.14, OpenSearch Dashboards |
| Testing | JUnit 5, Mockito, REST Assured |
| Build | Maven (via `mvnw`) |
| Containerization | Docker, multi-stage Dockerfile, Docker Compose |

## Architecture

```
┌─────────┐        ┌──────────────────────────────────────────┐
│ Client  │───────▶│              Quarkus app                  │
└─────────┘        │  ┌───────────────┐   ┌──────────────────┐│      ┌───────────────────┐
                    │  │ OrderResource │──▶│   OrderService    ││─────▶│  Exchange Rate API │
                    │  └───────────────┘   └──────────────────┘│      │  (external, HTTPS) │
                    │                          │  │  │          │      └───────────────────┘
                    │                          │  │  └──────────┼─────▶ PostgreSQL
                    │                          │  └─────────────┼─────▶ Kafka (order-events)
                    │                          │                │              │
                    │                    OrderEventConsumer ◀───┼──────────────┘
                    │                          │                │
                    │                          ▼                │
                    │                    OpenSearchClient ───────┼─────▶ OpenSearch (orders index)
                    └────────────────────────────────────────────┘              │
                                                                                 ▼
                                                                     OpenSearch Dashboards
```

All five services run on a single Docker Compose network (`app-network`).

## Prerequisites

- Docker and Docker Compose
- (Optional, for local dev outside Docker) Java 21 and no separate Maven install needed — the project uses the Maven Wrapper (`./mvnw`)

## Quick Start

Clone the repository and start the full stack:

```bash
git clone <your-repo-url>
cd quarkus-api
docker compose up -d --build
```

Wait for all services to report healthy:

```bash
docker compose ps
```

Create your first order:

```bash
curl -i -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-1001","amountUSD":150.00,"targetCurrency":"EUR"}'
```

View it in OpenSearch Dashboards at [http://localhost:5601](http://localhost:5601).

### Service Ports

| Service | Port | Notes |
|---|---|---|
| Quarkus app | `8080` | REST API |
| PostgreSQL | `5432` | `orders_user` / `orders_pass` / `orders_db` |
| Kafka | `9092` (host), `29092` (internal) | See [Troubleshooting](#troubleshooting) for why there are two |
| OpenSearch | `9200` | HTTPS, security enabled |
| OpenSearch Dashboards | `5601` | Web UI |

## API Reference

### Create an order

```
POST /api/v1/orders
Content-Type: application/json
```

**Request**
```json
{
  "customerId": "CUST-1001",
  "amountUSD": 150.00,
  "targetCurrency": "EUR"
}
```

**Response — `201 Created`**
```json
{
  "orderId": 1,
  "customerId": "CUST-1001",
  "amountUSD": 150.00,
  "targetCurrency": "EUR",
  "convertedAmount": 138.50,
  "status": "PROCESSED",
  "createdAt": "2026-03-31T10:00:00Z"
}
```

### Get an order

```
GET /api/v1/orders/{id}
```

Returns `200` with the order, or `404` if it doesn't exist.

### List orders

```
GET /api/v1/orders
GET /api/v1/orders?customerId=CUST-1001
```

Returns `200` with an array of orders.

### Error responses

Every error follows the same shape:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Unsupported or unknown target currency: XXX",
  "timestamp": "2026-09-04T06:07:28.669Z"
}
```

| Scenario | Status |
|---|---|
| Missing/invalid field (e.g. missing `customerId`, negative `amountUSD`) | `400` |
| Malformed JSON body (wrong field type) | `400` |
| Unknown or malformed currency code | `400` |
| Order not found | `404` |
| Exchange rate provider unreachable or errored | `502` |
| Unexpected server error | `500` |

## Configuration

Key settings live in `src/main/resources/application.properties`. When running via Docker Compose, these are overridden with environment variables using Quarkus's `QUARKUS_X_Y_Z` convention — see `docker-compose.yml` for the exact values.

| Property | Purpose |
|---|---|
| `quarkus.datasource.jdbc.url` | PostgreSQL connection |
| `quarkus.rest-client.exchange-rate-api.url` | Exchange rate API base URL |
| `kafka.bootstrap.servers` | Kafka broker address |
| `quarkus.rest-client.opensearch-api.url` | OpenSearch base URL |
| `quarkus.rest-client.opensearch-api.username` / `.password` | OpenSearch credentials |
| `quarkus.tls.trust-all` | Accepts OpenSearch's self-signed cert (dev only — never use in production) |

## Running Tests

Run everything (unit + integration):

```bash
./mvnw clean test
```

Run only unit tests (mocked dependencies, no external calls):

```bash
./mvnw test -Dtest=OrderServiceTest
```

Run only integration tests (REST Assured, hits the real running app, Postgres, Kafka, and the live exchange rate API):

```bash
./mvnw test -Dtest=OrderResourceIT
```

> Integration tests require Postgres, Kafka, and network access to the exchange rate API to be available.

## Project Structure

```
src/main/java/org/project/
├── client/           # REST Client Reactive interfaces (ExchangeRateClient, OpenSearchClient)
├── dto/              # Request/response payloads
├── event/            # Kafka event payload, producer, consumer
├── exception/        # Custom exceptions + JAX-RS exception mappers
├── model/            # Panache entity (Order) and enums
├── resource/         # JAX-RS REST endpoints
└── service/          # Business logic

src/main/resources/
├── application.properties
└── db/migration/     # Flyway SQL migrations

src/test/java/org/project/
├── service/          # Unit tests (@QuarkusTest + @InjectMock)
└── resource/         # Integration tests (REST Assured)

Dockerfile            # Multi-stage build → runnable container image
docker-compose.yml    # Full stack: Postgres, Kafka, OpenSearch, Dashboards, app
```

## OpenSearch Dashboards

Once the stack is running and at least one order has been created:

1. Open [http://localhost:5601](http://localhost:5601) and log in.
2. **☰ → Stack Management → Index Patterns → Create index pattern** → name it `orders`, time field `createdAt`.
3. **☰ → Visualize** to build or view the four saved visualizations:
   - **Total Gross Revenue** — Metric, `Sum(amountUSD)`
   - **Revenue Trend Over Time** — Line chart, Date Histogram on `createdAt` vs `Sum(amountUSD)`
   - **Popular Target Currencies** — Donut, Terms aggregation on `targetCurrency`
   - **Top 5 Customers by Revenue** — Data table, Terms on `customerId.keyword`, ordered by `Sum(amountUSD)`
4. **☰ → Dashboards** to view them combined, with auto-refresh enabled.

## Troubleshooting

**Kafka: `Connection to node ... could not be established` from inside a container**
Kafka's default `apache/kafka` image only advertises `localhost:9092`, which only works for clients on the host machine. The `docker-compose.yml` in this repo configures a second, internal listener (`kafka:29092`) specifically for container-to-container traffic — this is why the Quarkus service in Compose points at `kafka:29092`, not `kafka:9092`.

**OpenSearch: `401 Unauthorized` from the Java REST client, but `curl` with the same password works**
Quarkus REST Client Reactive does not automatically send Basic Auth from plain `username`/`password` config properties. The `OpenSearchClient` interface uses `@ClientBasicAuth` explicitly to fix this.

**OpenSearch Dashboards shows a login page unexpectedly, or rejects the password**
Confirm the actual password baked into the running container rather than assuming:
```bash
docker exec opensearch env | grep -i admin
```

**Port 8080 already in use**
```bash
sudo lsof -i :8080
kill -9 <PID>
```

**Maven Wrapper fails with a SHA-256 checksum error during Docker build**
Remove the pinned checksum in `.mvn/wrapper/maven-wrapper.properties`:
```bash
sed -i '/distributionSha256Sum/d' .mvn/wrapper/maven-wrapper.properties
```

**Fresh start (wipe all data)**
```bash
docker compose down -v
docker compose up -d --build
```
# quarkus-api

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/quarkus-api-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): Build RESTful web services and APIs using Jakarta REST (formerly JAX-RS)
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): Simplified JPA/Hibernate data access layer with active record and repository patterns
- JDBC Driver - PostgreSQL ([guide](https://quarkus.io/guides/datasource)): Connect to the PostgreSQL database via JDBC

## Provided Code

### Hibernate ORM

Create your first JPA entity

[Related guide section...](https://quarkus.io/guides/hibernate-orm)


[Related Hibernate with Panache section...](https://quarkus.io/guides/hibernate-orm-panache)


### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
