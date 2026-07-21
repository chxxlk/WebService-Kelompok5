# Orchestrator Saga - Kelompok 5

Sistem orkestrasi saga pattern untuk mengkoordinasi transaksi mikroservis menggunakan Spring Boot dan MongoDB Cloud (Atlas).

## Arsitektur

```
Client
  │
  ▼
┌──────────────────────────┐
│   OrderController        │  POST /api/orders
│   (Orchestrator Entry)   │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│  OrderSagaOrchestrator   │  Core saga logic + compensation
│  (Service Layer)         │
└──────────┬───────────────┘
           │ WebClient (HTTP)
     ┌─────┼─────────┐
     ▼     ▼         ▼
┌────────┐┌────────┐┌──────────┐
│ mock1  ││ mock2  ││  mock3   │
│ Order  ││Payment ││Inventory │
│Service ││Service ││ Service  │
└────────┘└────────┘└──────────┘
           │
           ▼
┌──────────────────────────┐
│       MongoDB Atlas       │
│  - orders collection      │
│  - sagalogs collection    │
└──────────────────────────┘
```

## Saga Flow

### Happy Path (Semua step berhasil)

```
Step 1: POST /mock1/order      → Create order       → Save order (CREATED) → Simpan response
Step 2: POST /mock2/payment    → Process payment    → Update order (PAID)  → Simpan response
Step 3: POST /mock3/inventory  → Reserve inventory  → Update order (COMPLETED) → Simpan response
```

**Response JSON** berisi aggregated data dari ketiga step.

### Compensation (Ada step yang gagal)

```
Step 3 gagal → Update order (CANCELLED) → Rollback Step 2 (refund) → Rollback Step 1 (cancel order)
```

Setiap rollback di-log dengan jelas di console dan MongoDB.

## MongoDB Collections

### orders
```json
{
  "_id": "abc12345",
  "productName": "Laptop ASUS",
  "quantity": 1,
  "totalPrice": 12000000,
  "status": "COMPLETED",
  "createdAt": "2026-07-21T10:00:00",
  "updatedAt": "2026-07-21T10:00:02"
}
```

**Status progression:** `CREATED` → `PAID` → `COMPLETED` | `CANCELLED` (on compensation)

### sagalogs
```json
{
  "sagaId": "uuid-saga-id",
  "step": "CREATE_ORDER",
  "status": "ORDER_CREATED",
  "message": "Order created: abc12345",
  "timestamp": "2026-07-21T10:00:00"
}
```

## API Endpoints

### Orchestration

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Place order (memulai saga) |

**Request Body:**
```json
{
  "productName": "Laptop ASUS",
  "quantity": 1,
  "totalPrice": 12000000
}
```

**Response (Success):**
```json
{
  "sagaId": "uuid-saga-id",
  "orderId": "abc12345",
  "status": "COMPLETED",
  "message": "Order placed successfully",
  "executedSteps": ["CREATE_ORDER", "PROCESS_PAYMENT", "RESERVE_INVENTORY"],
  "step1CreateOrder": {
    "orderId": "abc12345",
    "status": "SUCCESS",
    "message": "Order created successfully"
  },
  "step2ProcessPayment": {
    "paymentId": "pay12345",
    "status": "SUCCESS",
    "message": "Payment processed successfully"
  },
  "step3ReserveInventory": {
    "reservationId": "inv12345",
    "status": "SUCCESS",
    "message": "Inventory reserved successfully"
  }
}
```

**Response (Compensated):**
```json
{
  "sagaId": "uuid-saga-id",
  "orderId": "abc12345",
  "status": "COMPENSATED",
  "message": "Payment failed, saga compensated: ...",
  "executedSteps": ["CREATE_ORDER"],
  "step1CreateOrder": { "orderId": "abc12345", "status": "SUCCESS", ... },
  "step2ProcessPayment": null,
  "step3ReserveInventory": null
}
```

### Mock Microservices

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/mock1/order` | Order Service - Create order |
| DELETE | `/mock1/order/{orderId}` | Order Service - Cancel order (compensation) |
| POST | `/mock2/payment` | Payment Service - Process payment |
| POST | `/mock2/payment/refund/{orderId}` | Payment Service - Refund (compensation) |
| POST | `/mock3/inventory` | Inventory Service - Reserve stock |
| DELETE | `/mock3/inventory/{orderId}` | Inventory Service - Release stock (compensation) |

## Logging

Setiap step orchestrator mencatat log yang jelas di console:

```
========================================
Saga [abc-123] STARTED - Product: Laptop ASUS, Qty: 1, Total: 12000000
========================================
Saga [abc-123] Step 1 - Calling Order Service (POST /mock1/order)...
[Order Service] Order created: def45678
Saga [abc-123] Step 1 SUCCESS - Response: {orderId=def45678, status=SUCCESS, ...}
Saga [abc-123] Step 2 - Calling Payment Service (POST /mock2/payment)...
[Payment Service] Payment processed: pay12345 for order: def45678
Saga [abc-123] Step 2 SUCCESS - Response: {paymentId=pay12345, status=SUCCESS, ...}
Saga [abc-123] Step 3 - Calling Inventory Service (POST /mock3/inventory)...
[Inventory Service] Inventory reserved: inv98765 for order: def45678
Saga [abc-123] Step 3 SUCCESS - Response: {reservationId=inv98765, status=SUCCESS, ...}
========================================
Saga [abc-123] COMPLETED - All 3 steps succeeded
========================================
```

Kompensasi juga di-log:
```
Saga [abc-123] ═══ COMPENSATION STARTED ═══ for steps: [CREATE_ORDER, PROCESS_PAYMENT]
Saga [abc-123] Compensating PROCESS_PAYMENT - Calling refund...
Saga [abc-123] Compensation OK - Payment refunded for order: def45678
Saga [abc-123] Compensating CREATE_ORDER - Calling cancel...
Saga [abc-123] Compensation OK - Order cancelled: def45678
Saga [abc-123] ═══ COMPENSATION COMPLETED ═══
```

Semua log juga disimpan di MongoDB collection `sagalogs` dan order status di `orders`.

## Tech Stack

- **Java 17**
- **Spring Boot 4.1.0**
- **Spring WebFlux** (WebClient untuk HTTP calls antar service)
- **Spring Web MVC** (REST API)
- **Spring Data MongoDB** (MongoDB Atlas Cloud)
- **SLF4J / Logback** (logging)

## Running the Project

### Run Tests
```bash
# Windows
.\mvnw.cmd test

# Linux/Mac
./mvnw test
```

### Run Application
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

**Note:** Update MongoDB credentials in `src/main/resources/application.properties` before running.

## Setup

1. Buat akun di [MongoDB Atlas](https://www.mongodb.com/atlas)
2. Buat cluster gratis
3. Copy connection string
4. Edit `src/main/resources/application.properties`:
   ```
   spring.mongodb.uri=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/orchestrator-saga?retryWrites=true&w=majority
   ```
5. Jalankan:
   ```
   ./mvnw spring-boot:run
   ```

## Struktur Proyek

```
src/main/java/com/kelompok5/orchestrator/
├── OrchestratorSagaApplication.java     # Entry point
├── config/
│   └── WebClientConfig.java             # WebClient configuration
├── controller/
│   ├── OrderController.java             # POST /api/orders
│   ├── MockController.java              # Mock Order Service
│   ├── PaymentController.java           # Mock Payment Service
│   └── InventoryController.java         # Mock Inventory Service
├── dto/
│   ├── OrderRequest.java                # Request DTO
│   └── OrderResponse.java              # Aggregated response DTO
├── model/
│   ├── Order.java                       # MongoDB document
│   ├── SagaLog.java                     # Saga execution log document
│   ├── SagaStep.java                    # Enum step
│   └── SagaStatus.java                  # Enum status
├── repository/
│   ├── OrderRepository.java             # MongoDB repo orders
│   └── SagaLogRepository.java           # MongoDB repo saga logs
└── service/
    └── OrderSagaOrchestrator.java       # Core orchestration + compensation
```
