<div align="center">
  <h1>📦 ShopFlow Order Service</h1>
  <p><i>The central transactional engine orchestrating checkout, payment processing, and order lifecycle management for the ShopFlow ecosystem.</i></p>
</div>

---

## 📖 Overview

The **Order Service** is the backbone of the transactional capabilities within the ShopFlow microservices architecture. It securely manages the end-to-end lifecycle of customer orders, spanning from cart checkout and inventory reservation to payment validation and final order placement.

Engineered for high reliability and eventual consistency across distributed systems, it relies heavily on **PostgreSQL** for ACID-compliant transactional persistence, **Redis** for distributed locking and caching, and **Apache Kafka** for resilient, asynchronous event-driven choreographies.

## ✨ Key Features

- **Robust Order Lifecycle:** Comprehensive state machine handling covering `PENDING_PAYMENT`, `PROCESSING`, `SHIPPED`, `DELIVERED`, and `CANCELLED` states.
- **Distributed Transactions:** Coordinates with the Product Service to atomically reserve and deduct inventory during the checkout process.
- **Idempotency & Concurrency Control:** Leverages **Redis** for distributed locks, ensuring safe and idempotent webhook processing and concurrent order status updates.
- **Event-Driven Resilience:** Emits rich Kafka events (`order-created`, `order-status-updated`) to drive downstream integrations like notifications, billing, and analytics.
- **Secure Processing:** Employs stateless, token-based identity propagation to ensure customers can only access their own order history, with elevated overrides for Administrators.

## 🛠️ Technology Stack

- **Framework:** Spring Boot 3.2.x (Java 21)
- **Database:** PostgreSQL (via Spring Data JPA)
- **Caching & Locks:** Redis (via Spring Data Redis)
- **Message Broker:** Apache Kafka (JSON serialization)
- **Database Migrations:** Flyway
- **Service Discovery & Config:** Eureka Client & Spring Cloud Config
- **Inter-Service Communication:** Spring Cloud OpenFeign

## 🏗️ Architecture & Integrations

Operating at the heart of the system, the Order Service interacts extensively with neighboring domains:

*   **API Gateway / Identity:** Uses `X-User-Id` and `X-User-Role` headers propagated from the Gateway for fine-grained, localized authorization.
*   **Product Service:** Synchronous interaction via OpenFeign to fetch accurate product pricing, validate stock availability, and reserve product allocations atomically before committing an order.
*   **User Service:** Synchronous fetch to resolve user profile details, verified addresses, and contact context for fulfilling shipment details.
*   **Payment Integrations:** Future-proofed architecture to interface securely with payment gateways to authorize and capture funds via asynchronous workflows.

### Event Streams (Kafka Topics)

| Topic | Role | Description |
| :--- | :--- | :--- |
| `order-created` | **Producer** | Fired immediately upon successful checkout, triggering notifications (via Notification Service) to the customer and seller. |
| `order-status-updated` | **Producer** | Emitted when an order transitions state (e.g., shipped, delivered, cancelled). Vital for triggering user alerts or automated refund workflows. |

## 🗄️ Database Context (PostgreSQL)

Migrations are strictly managed via Flyway (`src/main/resources/db/migration`). Core database concepts generally map to:

*   `orders`: The root aggregate representing a customer purchase, tracking overall totals, payment statuses, and linked user IDs.
*   `order_items`: Granular line items explicitly associating historical snapshot prices, SKU identifiers, and quantities to a specific order.
*   `order_addresses`: Persistent, immutable snapshot of the shipping and billing addresses provided at checkout to ensure historical accuracy independent of user profile changes.

## 🚀 Getting Started

### Prerequisites

Ensure the following backing infrastructure components are accessible (typically via a global `docker-compose`):
*   PostgreSQL
*   Redis
*   Kafka & Zookeeper
*   Eureka Server
*   Central Config Server

### Local Development

1. **Clone & Navigate:**
   ```bash
   cd shopflow-backend/order-service
   ```

2. **Configuration Loader:**
   Ensure the Centralized Config Server is running (usually on `http://localhost:8888`). The Order Service relies on this to fetch runtime profiles, secrets, and database connection pools.

3. **Start the Service:**
   ```bash
   ./mvnw spring-boot:run
   ```
   *The service typically listens on port **8085** or dynamically allocates ports, registering itself seamlessly with Eureka.*

## 🔒 Security & Roles

Authentication resolves upstream at the API Gateway. The Order Service enforces localized execution authorization:

*   **Customer Routes:** `POST /api/orders` (Checkout flow), `GET /api/orders` (View personal order history). Operations are restricted strictly matching the injected `X-User-Id`.
*   **Admin Routes:** `PUT /api/orders/{id}/status` (Updating lifecycle state from fulfillment), allowing global viewing capabilities across all customers.
*   **Internal Access:** Dedicated `/internal/**` REST boundaries strictly locked down for mutual OpenFeign consumption from peer microservices.

---
*Built for the ShopFlow Microservices Ecosystem.*
