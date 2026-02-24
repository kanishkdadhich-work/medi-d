# Medi-D Testing Guide

## 1) Backend Pagination (new)

These are additive endpoints; frontend pagination still works unchanged.

- `GET /api/admin/users/paged?page=0&size=10&sortBy=username&direction=asc`
- `GET /api/appointments/paged?page=0&size=10&sortBy=appointmentTime&direction=desc`
- `GET /api/patients/paged?page=0&size=10&sortBy=createdAt&direction=desc`
- `GET /api/medicines/paged?page=0&size=10&sortBy=name&direction=asc`
- `GET /api/prescriptions/queue/paged?page=0&size=10&sortBy=createdAt&direction=desc`
- `GET /api/pharmacy/queue/paged?page=0&size=10&sortBy=createdAt&direction=desc`

Response shape:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 123,
  "totalPages": 13,
  "first": true,
  "last": false
}
```

## 2) Postman Collection Usage

Import:
- `postman/Medi-D-Resilience-Tests.postman_collection.json`

Run order:
1. Run folder `00 Auth Bootstrap` first.
2. Then run `01`, `02`, `03`, `04` folders.

Collection variables to verify/update:
- `baseUrl`
- credentials (`adminUsername`, `doctorUsername`, `pharmacistUsername`, passwords)
- seed IDs (`validAppointmentId`, `dispensePrescriptionId`, etc.)

## 3) Concurrency Test in Postman (Double Dispense)

Goal: prove only one request dispenses, second fails.

Steps:
1. Open request `02 Business Logic & Edge Cases -> Double dispense race - Request A` in one tab.
2. Open `Double dispense race - Request B` in second tab.
3. Keep same `dispensePrescriptionId` in variables.
4. Click `Send` on both tabs as close together as possible.
5. Expected:
- one request: `200`
- other request: `400/409` style failure (already dispensed / invalid state)

Why this should pass now:
- prescription row is locked with `PESSIMISTIC_WRITE` before status transition.

## 4) API Bombarding / Load Testing

## A) Quick Postman Runner burst
- Select request: `03 API Bombing / Stress -> Queue endpoint smoke`
- Runner settings:
  - Iterations: `1000`
  - Delay: `0 ms`
- This is not true concurrency but gives burst pressure.

## B) JMeter (recommended for 500 concurrent)

1. Thread Group:
- Threads (users): `500`
- Ramp-up: `5` seconds
- Loop count: `1` (or more)

2. HTTP Request Defaults:
- Protocol: `http`
- Server: `localhost`
- Port: `8080`

3. HTTP Header Manager:
- `Authorization: Bearer <pharmacist token>`

4. Sampler:
- `GET /api/prescriptions/queue`

5. Listeners:
- Summary Report
- Aggregate Report

Watch metrics:
- error %
- p95 latency
- throughput
- DB connections usage

## C) Locust example

```python
from locust import HttpUser, task, between

TOKEN = "<pharmacist-token>"

class MedidUser(HttpUser):
    wait_time = between(0.1, 0.5)

    @task
    def queue(self):
        self.client.get(
            "/api/prescriptions/queue",
            headers={"Authorization": f"Bearer {TOKEN}"}
        )
```

Run:

```bash
locust -f locustfile.py --host=http://localhost:8080
```

Then set users `500` and hatch rate `100` in UI.

## 5) Load-Safety knobs already set

In `application-dev.properties`:
- `spring.datasource.hikari.maximum-pool-size`
- `spring.datasource.hikari.minimum-idle`
- timeouts and max lifetime

Tune via env vars:
- `DB_POOL_MAX`, `DB_POOL_MIN_IDLE`, `DB_POOL_CONN_TIMEOUT_MS`, etc.

## 6) Logging overhead during load

- Debug logs are disabled by default (`INFO`).
- Keep `logging.level.com.medid=INFO` for stress tests.
- Avoid `DEBUG` while bombarding to prevent heavy log I/O.
