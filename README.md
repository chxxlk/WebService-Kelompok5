# Saga Orchestrator

Saga Orchestrator adalah microservice yang mengelola pola **Saga Pattern** dengan pendekatan **orchestration** untuk mengkoordinasikan transaksi lintas layanan. Setiap langkah (step) memiliki mekanisme **compensating transaction** (rollback) jika terjadi kegagalan.

## Tech Stack

| Komponen | Teknologi |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Build Tool | Maven |
| HTTP Client | WebClient |
| Database | MongoDB Atlas (Cloud) |
| Pattern | Saga Orchestration + Compensating Transaction |

## Arsitektur

```
Client
  │
  ▼
┌─────────────────┐
│  SagaController │  GET /saga/run?failStep=2
└────────┬────────┘
         │
         ▼
┌──────────────────────┐
│  OrchestratorService │  Koordinasi Step 1 → 2 → 3
└────────┬─────────────┘         │ (jika gagal)
         │                       ▼
         ▼               Compensating
┌──────────────────┐     (rollback)
│  WebClientService│
└────────┬─────────┘
         │
         ▼
┌──────────────────┐     ┌─────────────────┐
│  MockController  │◄────│  MongoDB Atlas  │
│  /mock1, /mock2, │     │  (saga_logs)    │
│  /mock3          │     └─────────────────┘
└──────────────────┘
```

## Endpoint

### Saga Orchestration

| Method | Endpoint | Deskripsi |
|---|---|---|
| GET | `/saga/run` | Jalankan seluruh saga (Step 1 → 2 → 3 + kompensasi) |
| GET | `/saga/run?failStep=1` | Simulasi Step 1 gagal (order) |
| GET | `/saga/run?failStep=2` | Simulasi Step 2 gagal (payment) + compensate Step 1 |
| GET | `/saga/run?failStep=3` | Simulasi Step 3 gagal (inventory) + compensate Step 2 & 1 |
| GET | `/saga/step1` | Jalankan Step 1 saja (order) |
| GET | `/saga/step2` | Jalankan Step 2 saja (payment) |
| GET | `/saga/step3` | Jalankan Step 3 saja (inventory) |

### Mock Services (Simulasi Microservice)

| Method | Endpoint | Deskripsi |
|---|---|---|
| GET | `/mock1` | Order Service — buat pesanan |
| GET | `/mock2` | Payment Service — proses pembayaran |
| GET | `/mock3` | Inventory Service — cek stok |
| GET | `/mock1?fail=true` | Simulasi order gagal |
| GET | `/mock2?fail=true` | Simulasi payment gagal |
| GET | `/mock3?fail=true` | Simulasi inventory gagal |
| POST | `/mock1/cancel` | Cancel Order (kompensasi Step 1) |
| POST | `/mock2/refund` | Refund Payment (kompensasi Step 2) |
| POST | `/mock3/restock` | Restock Inventory (kompensasi Step 3) |

## Response Format

### Success

```json
{
  "sagaId": "uuid",
  "overallStatus": "SUCCESS",
  "order": {
    "service": "order-service",
    "status": "success",
    "data": { "orderId": "ORD-001", "amount": 150000, "currency": "IDR" }
  },
  "payment": {
    "service": "payment-service",
    "status": "success",
    "data": { "paymentId": "PAY-001", "method": "credit_card", "amount": 150000 }
  },
  "inventory": {
    "service": "inventory-service",
    "status": "success",
    "data": { "sku": "ITEM-001", "stock": 50, "warehouse": "WH-JKT" }
  },
  "compensations": [],
  "timestamp": "2026-07-22T03:00:00"
}
```

### Payment Gagal (failStep=2)

```json
{
  "sagaId": "uuid",
  "overallStatus": "COMPENSATED",
  "order": { "service": "order-service", "status": "success", "data": {...} },
  "payment": { "service": "payment-service", "status": "failed", "data": {...} },
  "inventory": null,
  "compensations": [
    { "step": 1, "service": "order-service", "action": "cancel", "status": "cancelled" }
  ],
  "timestamp": "2026-07-22T03:00:00"
}
```

### Inventory Gagal (failStep=3)

```json
{
  "sagaId": "uuid",
  "overallStatus": "COMPENSATED",
  "order": { "service": "order-service", "status": "success", "data": {...} },
  "payment": { "service": "payment-service", "status": "success", "data": {...} },
  "inventory": { "service": "inventory-service", "status": "failed", "data": null },
  "compensations": [
    { "step": 2, "service": "payment-service", "action": "refund", "status": "refunded" },
    { "step": 1, "service": "order-service", "action": "cancel", "status": "cancelled" }
  ],
  "timestamp": "2026-07-22T03:00:00"
}
```

## Compensating Transaction

| Step Gagal | Kompensasi |
|---|---|
| Step 2 (Payment) | Cancel Order (compensate Step 1) |

## Log Output

### Payment Gagal (failStep=2)

```
[Saga][uuid] ========== Starting Saga Orchestration ==========
[Saga][Step 1] Calling mock1 (order-service)...
[Saga][Step 1] Response: {service=order-service, status=success, ...}
[Saga][Step 1] Saved to MongoDB (id=...)
[Saga][Step 2] Calling mock2 (payment-service)...
[Saga][Step 2] Response: {service=payment-service, status=failed, ...}
[Saga][Step 2] Saved to MongoDB (id=...)
[Saga][uuid] Step 2 (payment) failed — initiating compensation
[Saga][uuid] Rolling back Step 1: cancelling order...
[Saga][Compensation][Step 1] Cancelling order...
[Saga][Compensation][Step 1] Response: {service=order-service, action=cancel, status=cancelled}
[Saga][Compensation][Step 1] Saved to MongoDB (id=...)
[Saga][uuid] ========== Payment Failed — Compensation Completed ==========
```

### Inventory Gagal (failStep=3)

```
[Saga][uuid] ========== Starting Saga Orchestration ==========
[Saga][Step 1] Calling mock1 (order-service)...
[Saga][Step 1] Response: {service=order-service, status=success, ...}
[Saga][Step 2] Calling mock2 (payment-service)...
[Saga][Step 2] Response: {service=payment-service, status=success, ...}
[Saga][Step 3] Calling mock3 (inventory-service)...
[Saga][Step 3] Response: {service=inventory-service, status=failed, ...}
[Saga][uuid] Step 3 (inventory) failed — initiating compensation
[Saga][uuid] Rolling back Step 2: refunding payment...
[Saga][Compensation][Step 2] Refunding payment...
[Saga][Compensation][Step 2] Response: {service=payment-service, action=refund, status=refunded}
[Saga][uuid] Rolling back Step 1: cancelling order...
[Saga][Compensation][Step 1] Cancelling order...
[Saga][Compensation][Step 1] Response: {service=order-service, action=cancel, status=cancelled}
[Saga][uuid] ========== Inventory Failed — Compensation Completed ==========
```

## Cara Menjalankan

```bash
# Build
./mvnw.cmd clean compile

# Run
./mvnw.cmd spring-boot:run

# Test — semua sukses
curl http://localhost:8080/saga/run

# Test — salah satu 
curl http://localhost:8080/saga/step1

# Test — payment gagal
curl "http://localhost:8080/saga/run?failStep=2"

# Test — mock langsung
curl http://localhost:8080/mock1
curl "http://localhost:8080/mock2?fail=true"
```

## Konfigurasi

`application.properties`:

```properties
spring.application.name=saga_orchestrator
spring.mongodb.uri=mongodb+srv://<user>:<pass>@<cluster>.mongodb.net/?appName=<app>
```

## Struktur Project

```
src/main/java/com/kelompok5/saga_orchestrator/
├── SagaOrchestratorApplication.java
├── config/
│   └── WebClientConfig.java
├── controller/
│   ├── MockController.java
│   └── SagaController.java
├── dto/
│   ├── SagaResponse.java
│   ├── StepResult.java
│   └── CompensationResult.java
├── model/
│   └── SagaLog.java
├── repository/
│   └── SagaLogRepository.java
└── service/
    ├── OrchestratorService.java
    └── WebClientService.java
```
