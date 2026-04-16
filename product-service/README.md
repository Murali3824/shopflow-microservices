<div align="center">
  <h1>🛍️ ShopFlow Product Service</h1>
  <p><i>The core engine powering product catalog, inventory, and search for the ShopFlow ecosystem.</i></p>
</div>

---

## 📖 Overview

The **Product Service** is a mission-critical microservice in the ShopFlow ecosystem responsible for managing the entire lifecycle of products, categories, variants (SKUs), and product media. Engineered for high performance and scalability, it leverages **PostgreSQL** as its primary system of record, **Elasticsearch** for lightning-fast full-text search, and **MinIO** for scalable image storage.

It acts as the central hub for the storefront, integrating tightly with the Order Service for inventory management and the Review Service for real-time rating aggregations.

## ✨ Key Features

- **Robust Catalog Management:** Hierarchical category structures with parent-child relationships and slug-based routing.
- **Advanced SKU System:** Variant-level management for pricing and inventory tracking.
- **High-Performance Search:** Synchronized Elasticsearch indexing for optimized full-text, fuzzy, and faceted product searches.
- **Event-Driven Inventory:** Real-time stock reservation and asynchronous low-stock threshold alerting via **Apache Kafka**.
- **Secure Media Storage:** Direct integration with **MinIO** for reliable product image uploads and primary image management.
- **Granular Access Control:** Role-Based Access Control (RBAC) ensuring Sellers can only modify their own catalog while Admins retain global oversight.

## 🛠️ Technology Stack

- **Framework:** Spring Boot 3.2.x (Java 21)
- **Primary Database:** PostgreSQL (via Spring Data JPA)
- **Search Engine:** Elasticsearch (via Spring Data Elasticsearch)
- **Object Storage:** MinIO (S3-compatible)
- **Message Broker:** Apache Kafka
- **Database Migrations:** Flyway
- **Service Discovery & Config:** Eureka Client & Spring Cloud Config
- **Inter-Service Communication:** Spring Cloud OpenFeign

## 🏗️ Architecture & Integrations

The Product Service operates within a broader microservices choreography:

*   **Gateway / Auth:** Relies on the API Gateway to forward downstream `X-User-Id` and `X-User-Role` headers for stateless authentication.
*   **Seller Service:** Communicates via Feign Client to validate seller status (`APPROVED`) before allowing product creation.
*   **Order Service:** Exposes internal endpoints for synchronous stock reduction/reservation during checkout flows.
*   **Review Service:** Consumes Kafka events to asynchronously update the aggregated `avg_rating` on product records.

### Event Streams (Kafka Topics)

| Topic | Role | Description |
| :--- | :--- | :--- |
| `product-rating-updated` | **Consumer** | Listens for new reviews to update the `avg_rating` on products, ensuring the search index stays fresh. |
| `product-low-stock` | **Producer** | Emits an event when a SKU's stock falls below its defined `low_stock_threshold`, triggering notification workflows. |

## 🗄️ Database Context (PostgreSQL)

Managed via Flyway migrations (`src/main/resources/db/migration`), the schema includes:

*   `categories`: Self-referencing table for multi-level taxonomy.
*   `products`: Core product metadata, linked to Sellers.
*   `product_skus`: Variants containing pricing and dynamic stock quantities.
*   `product_images`: MinIO URL references and primary image flags.

## 🚀 Getting Started

### Prerequisites

Ensure the following infrastructure components are running (typically via the global `docker-compose.yml`):
*   PostgreSQL
*   Elasticsearch
*   Kafka & Zookeeper
*   MinIO
*   Eureka Server & Config Server

### Local Development

1. **Clone & Navigate:**
   ```bash
   cd shopflow-backend/product-service
   ```

2. **Configuration:**
   The service fetches its configuration from the Centralized Config Server (`http://localhost:8888`). Ensure the Config Server is running and pointing to the correct environment configurations.

3. **Run the Service:**
   ```bash
   ./mvnw spring-boot:run
   ```
   *The service typically runs on port **8084**.*

## 🔒 Security & Authorization

Security is handled at the gateway layer. The Product Service trusts the Gateway to provide authenticated identity headers:
- `X-User-Id`: The UUID of the requester.
- `X-User-Role`: Determines capabilities (`SELLER`, `ADMIN`, or anonymous user).

* **Public Routes:** `GET /api/products/**` (Browsing & Search)
* **Seller Routes:** `POST/PUT/PATCH /api/products/**` (Own products only)
* **Admin Routes:** Modifying categories, deleting any product.
* **Internal Routes:** `**/internal/**` (Protected and only accessible via Feign clients from other services).

---
*Built for the ShopFlow Microservices Ecosystem.*
