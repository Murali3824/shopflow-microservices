# 🌟 Review Service - ShopFlow Backend Architecture

> A **production-grade microservice** for managing product reviews with intelligent purchase verification, real-time rating aggregation, and event-driven architecture.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Core Features](#core-features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Configuration](#configuration)
- [Getting Started](#getting-started)
- [Building & Deployment](#building--deployment)
- [Event-Driven Architecture](#event-driven-architecture)
- [Security](#security)
- [Performance & Scaling](#performance--scaling)
- [Contributing](#contributing)

---

## 📌 Overview

The **Review Service** is a critical component of the ShopFlow microservices ecosystem. It manages the entire lifecycle of product reviews, from submission through deletion, while maintaining data integrity through sophisticated validation mechanisms.

### Key Highlights:
- ✅ **Purchase-Based Review System** - Users can only review products they've actually purchased
- 🔄 **Event-Driven Rating Updates** - Real-time product rating aggregation via Kafka
- 🛡️ **Comprehensive Security** - JWT-based authentication with ownership verification
- 📊 **Pagination Support** - Efficient retrieval of large review datasets
- 🔗 **Service-to-Service Communication** - Feign clients for Order Service integration
- 📈 **Scalable & Distributed** - Eureka service discovery & distributed transactions

---

## 🎯 Core Features

### 1. **Review Management**
- **Submit Review**: Authenticated users can submit reviews with validation
- **Update Review**: Users can modify their own reviews (rating, title, body)
- **Delete Review**: Users can remove their reviews; admins can force-delete any review
- **Retrieve Reviews**: Public access to product reviews with pagination

### 2. **Purchase Verification**
- **Order Validation**: Integrates with Order Service to verify:
  - Order belongs to the authenticated user
  - Order status is "DELIVERED"
  - Product exists in the order
- **Duplicate Prevention**: One review per user per product

### 3. **Rating Aggregation**
- **Real-Time Updates**: Publishes rating events (average & total) to Kafka
- **Product Service Integration**: Product service consumes events to update display ratings
- **Statistical Accuracy**: Leverages database queries for precise calculations

### 4. **Audit & Compliance**
- **Ownership Checks**: Users can only edit/delete their own reviews
- **Admin Bypass**: Internal endpoints allow admin operations without ownership checks
- **Comprehensive Logging**: Detailed SLF4J logging for all operations

---

## 🛠 Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| **Java** | 21 | Core language; modern features & performance |
| **Spring Boot** | 3.2.5 | Application framework & auto-configuration |
| **Spring Cloud** | 2023.0.1 | Microservices ecosystem (Eureka, Feign, Config) |
| **Spring Data JPA** | Latest | ORM & database abstraction |
| **PostgreSQL** | Latest | Primary relational database |
| **Spring Security** | Latest | Authentication & authorization |
| **Kafka** | Via Spring Cloud Stream | Event publishing & async messaging |
| **Flyway** | 9.x | Database migration & versioning |
| **Lombok** | Latest | Boilerplate reduction |
| **Maven** | 3.8+ | Build & dependency management |

---

## 🏗 Architecture

### Service Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway / Client                      │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────▼────────────────┐
        │   Review Service (8088)     │
        │  ┌──────────────────────┐  │
        │  │  ReviewController    │  │
        │  └──────────┬───────────┘  │
        │             │              │
        │  ┌──────────▼────────────┐ │
        │  │  ReviewServiceImpl    │ │
        │  │  + submitReview      │ │
        │  │  + updateReview      │ │
        │  │  + deleteReview      │ │
        │  └──────────┬───────────┘ │
        │             │             │
        │  ┌──────────▼───────────┐  │
        │  │ ReviewRepository     │  │
        │  │ (Spring Data JPA)    │  │
        │  └──────────┬───────────┘  │
        └─────────────┼──────────────┘
                      │
        ┌─────────────┼─────────────┐
        │             │             │
        ▼             ▼             ▼
    PostgreSQL  orderServiceClient  KafkaTemplate
        │        (via Feign)         │
        │             │              │
        ▼             ▼              ▼
      [DB]      [Order Service]  [Kafka Topic]
               (Port 8081)      (product.rating.updated)
```

### Service Dependencies
- **Order Service**: Verify purchase eligibility
- **Eureka Server**: Service registration & discovery
- **Config Server**: Centralized configuration (optional)
- **Kafka Broker**: Event publication

---

## 📡 API Documentation

### Base URL
```
http://localhost:8088/api/reviews
```

### Authentication
All endpoints (except public GET) require JWT token in `Authorization: Bearer <token>` header.

---

### Endpoints

#### 1️⃣ **Submit Review** ⭐
```http
POST /api/reviews
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": "550e8400-e29b-41d4-a716-446655440001",
  "rating": 5,
  "title": "Excellent Product!",
  "body": "This product exceeded my expectations. Highly recommended!"
}
```

**Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "userId": "550e8400-e29b-41d4-a716-446655440003",
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": "550e8400-e29b-41d4-a716-446655440001",
  "rating": 5,
  "title": "Excellent Product!",
  "body": "This product exceeded my expectations...",
  "createdAt": "2026-05-06T10:30:00"
}
```

**Validation Rules:**
- ✅ Rating: 1-5 (integer only)
- ✅ Title: 1-255 characters, required
- ✅ Body: Required, unlimited length
- ✅ productId & orderId: Valid UUID format

**Error Codes:**
- `400` - Invalid input or validation failure
- `409` - Duplicate review (user already reviewed this product)
- `403` - Purchase verification failed
- `401` - Unauthorized (missing/invalid token)

---

#### 2️⃣ **Get Product Reviews** 📖
```http
GET /api/reviews/product/{productId}?page=0&size=10
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "userId": "550e8400-e29b-41d4-a716-446655440003",
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "rating": 5,
      "title": "Excellent Product!",
      "createdAt": "2026-05-06T10:30:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 42,
    "totalPages": 5
  }
}
```

**Query Parameters:**
- `page` (default: 0) - Zero-indexed page number
- `size` (default: 10) - Items per page

**Note:** This endpoint is **PUBLIC** (no authentication required)

---

#### 3️⃣ **Update Review** ✏️
```http
PUT /api/reviews/{reviewId}
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>

{
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "orderId": "550e8400-e29b-41d4-a716-446655440001",
  "rating": 4,
  "title": "Great Product - Updated",
  "body": "Updated review after more usage..."
}
```

**Response (200 OK):** Updated review object

**Error Codes:**
- `404` - Review not found
- `403` - Not review owner
- `401` - Unauthorized

---

#### 4️⃣ **Delete Review** 🗑️
```http
DELETE /api/reviews/{reviewId}
Authorization: Bearer <JWT_TOKEN>
```

**Response (204 No Content)**

**Error Codes:**
- `404` - Review not found
- `403` - Not review owner
- `401` - Unauthorized

---

#### 5️⃣ **Delete Review (Admin Internal)** 🔐
```http
DELETE /api/reviews/internal/{reviewId}
```

**Response (204 No Content)**

**Note:** This endpoint is protected by API Gateway/Network policies and bypasses ownership checks.

---

## 🗄 Database Schema

### Reviews Table

```sql
CREATE TABLE reviews (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  product_id UUID NOT NULL,
  order_id UUID NOT NULL,
  rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
  title VARCHAR(255) NOT NULL,
  body TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for query optimization
CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_user_product ON reviews(user_id, product_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC);
```

### Key Constraints
- **Foreign Key Reference**: `user_id` & `product_id` are logically (not enforced) linked to users & products
- **Unique Constraint**: One review per user per product is enforced at application level
- **Not Null Constraints**: All core fields are required
- **Rating Validation**: CHECK constraint ensures 1-5 range

### Database Queries
```sql
-- Get average rating and review count for a product
SELECT AVG(rating) AS average_rating, COUNT(*) AS total_reviews
FROM reviews
WHERE product_id = ?
GROUP BY product_id;

-- Get all reviews for a product (paginated)
SELECT * FROM reviews
WHERE product_id = ?
ORDER BY created_at DESC
LIMIT ? OFFSET ?;

-- Prevent duplicate reviews
SELECT COUNT(*) FROM reviews
WHERE user_id = ? AND product_id = ?;
```

---

## ⚙️ Configuration

### Application Properties (`application.yaml`)

```yaml
server:
  port: 8088

spring:
  application:
    name: review-service
  
  # Centralized Configuration Server
  config:
    import: optional:configserver:http://localhost:8888
  
  # Database Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/shopflow_review
    username: shopflow_user
    password: ${DB_PASSWORD}
  
  # JPA/Hibernate
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  # Flyway Migrations
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### Environment Variables

| Variable | Purpose | Example |
|----------|---------|---------|
| `EUREKA_URL` | Service Discovery | `http://localhost:8761/eureka` |
| `CONFIG_SERVER_URL` | Config Server | `http://localhost:8888` |
| `KAFKA_BROKERS` | Kafka Brokers | `localhost:9092` |
| `DB_URL` | Database URL | `jdbc:postgresql://localhost:5432/shopflow_review` |
| `DB_USER` | Database User | `shopflow_user` |
| `DB_PASSWORD` | Database Password | `secure_password` |

---

## 🚀 Getting Started

### Prerequisites
- Java 21 or higher
- Maven 3.8+
- PostgreSQL 12+
- Kafka 3.x (for event publishing)
- Eureka Server running
- Order Service accessible

### 1. Clone & Navigate
```bash
cd review-service
```

### 2. Configure Database
```bash
# Create database and user
psql -U postgres

CREATE DATABASE shopflow_review;
CREATE USER shopflow_user WITH PASSWORD 'your_secure_password';
ALTER ROLE shopflow_user SET client_encoding TO 'utf8';
ALTER ROLE shopflow_user CREATEDB;
ALTER ROLE shopflow_user SUPERUSER;
GRANT ALL PRIVILEGES ON DATABASE shopflow_review TO shopflow_user;
```

### 3. Build the Service
```bash
./mvnw clean package
```

### 4. Run the Service
```bash
./mvnw spring-boot:run
```

Or using the JAR:
```bash
java -jar target/review-service-0.0.1-SNAPSHOT.jar
```

### 5. Verify Service is Running
```bash
curl http://localhost:8088/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

---

## 📦 Building & Deployment

### Development Build
```bash
./mvnw clean install -DskipTests
```

### Production Build with Testing
```bash
./mvnw clean package -Pproduction
```

### Docker Build (if Dockerfile exists)
```bash
docker build -t shopflow/review-service:latest .
docker run -p 8088:8088 \
  -e DB_URL=jdbc:postgresql://postgres:5432/shopflow_review \
  -e DB_PASSWORD=password \
  shopflow/review-service:latest
```

### Docker Compose Integration
```bash
# From project root
docker-compose up review-service
```

---

## 📨 Event-Driven Architecture

### Kafka Events

#### **Product Rating Updated Event**

**Topic:** `product.rating.updated`

**Event Schema:**
```json
{
  "productId": "550e8400-e29b-41d4-a716-446655440000",
  "averageRating": 4.5,
  "totalReviews": 42,
  "timestamp": "2026-05-06T10:30:00Z"
}
```

**When Published:**
- ✅ After a review is submitted
- ✅ After a review is updated
- ✅ After a review is deleted

**Consumers:**
- Product Service - Updates display ratings
- Analytics Service - Consumer trends analysis
- Admin Dashboard - Real-time ratings

### Event Publishing Logic
```java
// Triggered after any review operation (submit/update/delete)
private void publishRatingEvent(UUID productId) {
    // 1. Query database for current ranking statistics
    List<Object[]> results = reviewRepository.getRatingStats(productId);
    
    // 2. Extract average and count
    Double average = calculateAverage(results);
    Long total = calculateTotal(results);
    
    // 3. Publish to Kafka
    reviewRatingKafkaTemplate.send(
        "product.rating.updated",
        productId.toString(),
        event
    );
}
```

---

## 🔐 Security

### Authentication
- **Mechanism:** JWT (JSON Web Tokens)
- **Provider:** Auth Service
- **Header:** `Authorization: Bearer <JWT_TOKEN>`

### Authorization
- **User Operations:** Users can only modify their own reviews
- **Admin Operations:** Admins can force-delete any review
- **Public Operations:** Reviews are publicly readable

### Security Best Practices
✅ **Ownership Verification**
```java
if (!review.getUserId().equals(userId)) {
    throw new UnauthorizedReviewAccessException(
        "You can only update your own review"
    );
}
```

✅ **Input Validation**
- All inputs validated using Jakarta Validation annotations
- SQL injection prevention via JPA parameterized queries

✅ **HTTPS in Production**
- Configure SSL/TLS at API Gateway level
- Spring Security HTTPS redirect

✅ **API Rate Limiting**
- Implement at API Gateway level
- Prevent review spam

---

## 📈 Performance & Scaling

### Database Optimization
- **Indexes**: Product, user, and composite indexes for fast retrieval
- **Pagination**: Prevents loading large datasets
- **Read-Only Transactions**: `@Transactional(readOnly = true)` for GET operations

### Caching Strategy (Future Enhancement)
```java
@Cacheable("productReviews")
public Page<ReviewResponse> getProductReviews(UUID productId, int page, int size)
```

### Scalability Considerations

| Aspect | Strategy |
|--------|----------|
| **State** | Stateless - horizontally scalable |
| **Database** | Connection pooling, read replicas for reporting |
| **Messaging** | Kafka partitioning by productId for parallel processing |
| **Deployment** | Container-based (Docker) with Kubernetes-ready configs |

### Load Testing
```bash
# Using Apache JMeter or similar
# Simulate 1000 concurrent users submitting reviews
jmeter -n -t load_test.jmx -l results.csv
```

---

## 🧪 Testing

### Unit Tests
```bash
./mvnw test
```

### Integration Tests
```bash
./mvnw verify
```

### Key Test Classes
- `ReviewServiceImplTest` - Business logic
- `ReviewControllerTest` - API endpoints
- `ReviewRepositoryTest` - Database operations

---

## 🤝 Contributing

### Code Standards
- Follow Google Java Style Guide
- Use meaningful variable names
- Add logging for debugging
- Write unit tests for new features

### Commit Message Format
```
[TYPE] Brief description

Detailed explanation if needed

Type: FEATURE | BUG | REFACTOR | DOCS | TEST
```

### Pull Request Checklist
- ✅ Code follows style guide
- ✅ Tests added/updated
- ✅ No breaking changes documented
- ✅ README updated if needed

---

## 📚 Additional Resources

### Related Services
- [Product Service](../product-service/README.md)
- [Order Service](../order-service/README.md)
- [Auth Service](../auth-service/README.md)

### Documentation
- [Spring Data JPA Guide](https://spring.io/projects/spring-data-jpa)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Apache Kafka Guide](https://kafka.apache.org/documentation/)

### Monitoring & Observability
- Health Check: `GET /actuator/health`
- Metrics: `GET /actuator/metrics`
- Application Insights: Integrated with Spring Boot Actuator

---

## 📞 Support & Contact

For issues or questions:
- 📧 **Email**: dev-team@shopflow.io
- 💬 **Slack**: #review-service
- 🐛 **Issue Tracker**: GitHub Issues
- 📋 **Wiki**: Team Documentation Portal

---

**Last Updated:** May 6, 2026  
**Version:** 1.0.0  
**Status:** 🟢 Production Ready

---

<div align="center">

### 🌟 Built with ❤️ by ShopFlow Development Team

**Elevating E-Commerce with Microservices Excellence**

</div>

