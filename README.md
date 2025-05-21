# Customer Transaction Processing System

This Spring Boot application processes transactional customer data with a focus on reliability, asynchronous handling, auditing, and retry mechanisms. It includes persistence of customer records, audit trails for each processing stage, and handling of failures with scheduled retries.

## Features

* Transactional processing of customer data
* Asynchronous post-commit operations using Spring Events
* File writing and SQS messaging simulation with audit logging
* Retry mechanism for failed records
* Scheduled cleanup of stale failed records

## Technologies Used

* Java 11+
* Spring Boot
* Spring Data JPA
* Spring Events
* Spring Scheduler
* Spring Async (ThreadPoolTaskExecutor)
* H2/PostgreSQL (JPA persistence)
* Lombok

## 📦 Tech Stack

| Technology         | Description                                |
|--------------------|--------------------------------------------|
| Spring Boot        | Application framework                      |
| Spring Data JPA    | ORM and repository layer                   |
| Spring Events      | Post-commit hooks                          |
| Spring Scheduling  | Retry and cleanup schedulers               |
| Spring Async       | Async execution of post-commit actions     |
| H2/PostgreSQL      | Database (switchable)                      |
| Lombok             | Reduces boilerplate code                   |
| SLF4J + Logback    | Logging                                    |
| REST API           | For submitting transactions                |

---

## ✅ Use Cases Covered

| Scenario                                  | Description                                                  |
|-------------------------------------------|--------------------------------------------------------------|
| Save customer transaction                 | Persists a list of customers into the DB                    |
| Async post-commit processing              | Event fired only after transaction commit                   |
| Write to file                             | Simulates file writing, randomly fails                      |
| Send to SQS                               | Simulates SQS push, randomly fails                          |
| Audit trail                               | Logs each stage per record: STARTED, SUCCESS, FAILED        |
| Duplicate ID handling                     | Avoids re-processing same `uniqueId` using memory cache     |
| Retry failed transactions                 | Uses `@Scheduled` to retry previously failed records         |
| Daily cleanup of failed >1-day-old        | Marks such records as INVALID                               |


## API Endpoints

### 1. Submit Transactional Customer Data

```
POST /api/transactions
```

**Request Body:**

```json
[
  {
    "uniqueId": "cust123",
    "payload": "{\"name\":\"John\",\"email\":\"john@example.com\"}"
  }
]
```

**Responses:**

* 200 OK: Transaction completed for...
* 500 Internal Server Error: Any unexpected server error

### 2. Get All Customers

```
GET /api/transactions/list
```

**Response:**

```json
[
  {
    "uniqueId": "cust123",
    "payload": "{\"name\":\"John\",\"email\":\"john@example.com\"}"
  },
  ...
]
```

### 3. Get Customer by ID

```
GET /api/transactions/get/customer/{id}
```

**Response:**

```json
{
  "uniqueId": "cust123",
  "payload": "{\"name\":\"John\",\"email\":\"john@example.com\"}"
}
```

**Error Response:**

* 404 Not Found: Customer not found for the given ID

## Processing Flow

### 1. Success Case

* Customer data is saved transactionally
* After commit, async event writes file and sends SQS message
* Audit logs are stored for WRITE\_FILE and SEND\_SQS stages
* Status updated to SUCCESS

### 2. Failure in File Write or SQS

* If WRITE\_FILE or SEND\_SQS fails, status updated to FAILED
* Retried in the background by scheduled tasks
* Only failed stages are retried

### 3. Retry Handling

* **5 Minute Retry:** Retries any FAILED customers via `@Scheduled`
* **1 Day Retry:** Marks customers as INVALID if they failed over a day ago

## Audit Logging

* Each stage (WRITE\_FILE, SEND\_SQS) is recorded with STARTED, SUCCESS, or FAILED
* Includes timestamp and error message if failed

## Large Transaction Support

* Async post-commit handling allows quick DB commits
* Separation of concerns ensures large payloads don't block the request thread
* Fine-grained failure recovery improves scalability and reliability

## How to Test

### Sample Test Flow

1. Submit customer data with POST `/api/transactions`
2. Use GET `/api/transactions/list` to confirm persistence
3. Use GET `/api/transactions/get/customer/{id}` to fetch specific records
4. Observe logs for retries and audit behavior

## Error Responses

* **Duplicate Processing:** Will be skipped and logged
* **Failure in File/SQS:** Will trigger retries
* **Missing Customer ID:** Returns 404

---

