# API Gateway 🚪

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.0-blue.svg)](https://spring.io/projects/spring-cloud)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)

## 🚀 Overview

The **API Gateway** is the single entry point for all client requests in the ShopFlow Backend microservices architecture. Built on Spring Cloud Gateway (WebFlux), it acts as a reverse proxy, routing traffic to downstream services while enforcing security, rate limiting, and centralized error handling. Think of it as the vigilant bouncer of your e-commerce platform—authenticating users, blocking unauthorized access, and directing requests to the right party.

This gateway integrates seamlessly with Eureka for service discovery, Config Server for dynamic configuration, and Redis for JWT token blacklisting, ensuring high availability, scalability, and security in a reactive, non-blocking manner.

## 🎯 Key Features

- **Centralized Routing**: Single point of entry for all API calls, routing to microservices based on paths.
- **JWT Authentication**: Validates Bearer tokens on every request, whitelisting public endpoints.
- **User Context Propagation**: Injects user identity headers (ID, email, role) into downstream requests.
- **Token Blacklisting**: Integrates with Redis to invalidate tokens (e.g., on logout).
- **Reactive & Non-Blocking**: Built on WebFlux for high throughput and low latency.
- **Eureka Discovery**: Auto-discovers service instances for load balancing and failover.
- **Global Error Handling**: Consistent JSON error responses across all services.
- **Public Route Whitelisting**: Allows unauthenticated access to auth endpoints, product catalogs, etc.
- **Caching**: Uses Caffeine for in-memory caching of frequently accessed data.

## 🏛️ Architecture

### How It Works

1. **Request Ingress**: All client requests hit the gateway first (e.g., `http://gateway:8080/api/products`).
2. **Authentication Check**: JWT filter validates tokens unless the route is whitelisted.
3. **User Context Injection**: Adds `X-User-Id`, `X-User-Email`, `X-User-Role` headers for downstream services.
4. **Routing & Load Balancing**: Routes to discovered service instances via Eureka.
5. **Response Egress**: Aggregates responses, handles errors uniformly.

### Integration Flow

```
[Client] → [API Gateway] → [Eureka] → [Target Microservice]
     ↓            ↓            ↓            ↓
  Sends Request  Authenticates  Discovers     Processes
  with JWT       & Routes       Service        Request
```

### Supported Routes

- **Auth Service**: `/api/auth/*` (login, register, etc.)
- **Product Service**: `/api/products/*`, `/api/categories/*`
- **Order Service**: `/api/orders/*`
- **Payment Service**: `/api/payments/*`
- **Review Service**: `/api/reviews/*`
- **User Service**: `/api/users/*`
- **Seller Service**: `/api/sellers/*`
- **Admin Service**: `/api/admin/*`
- **Notification Service**: `/api/notifications/*`

### Security Model

- **Public Routes**: Auth endpoints, product reads, webhooks—no JWT required.
- **Protected Routes**: All others require valid Bearer token.
- **Token Validation**: HMAC-SHA256 signed JWTs with configurable secret.
- **Blacklisting**: Redis-backed invalidation for logout/revocation.

## 🛠️ Setup & Installation

### Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **Redis** (for token blacklisting)
- **Eureka Server** and **Config Server** running
- Access to downstream microservices

### Local Development

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-org/shopflow-backend.git
   cd shopflow-backend/api-gateway
   ```

2. **Start Dependencies**:
   - Redis: `redis-server`
   - Eureka & Config Server: `docker-compose up eureka-server config-server`

3. **Build & Run**:
   ```bash
   ./mvnw clean compile
   ./mvnw spring-boot:run
   ```
   Gateway starts on port 8080 (configurable via config).

4. **Verify**:
   - Health: `curl http://localhost:8080/actuator/health`
   - Public Route: `curl http://localhost:8080/api/products`
   - Protected Route: `curl -H "Authorization: Bearer <token>" http://localhost:8080/api/orders`

### Docker Deployment

1. **Build Image**:
   ```bash
   docker build -t shopflow/api-gateway:latest .
   ```

2. **Run with Compose**:
   ```bash
   docker-compose up api-gateway
   ```

### Configuration

Configurations are centralized in Config Server (`api-gateway.yaml`):

- **JWT Secret**: HMAC key for token signing.
- **Redis**: Host, port, timeout for blacklisting.
- **Logging**: Debug levels for gateway and filters.

For custom routing, add route definitions in `application.yml` or via Config Server.

## 📖 Usage

### Client Integration

Clients interact only with the gateway—no direct service access.

1. **Obtain Token**: POST to `/api/auth/login` for JWT.
2. **Include in Requests**: `Authorization: Bearer <jwt>`
3. **Handle Responses**: JSON errors with consistent format.

### Example Requests

```bash
# Public: Get products
curl http://localhost:8080/api/products

# Authenticated: Get user orders
curl -H "Authorization: Bearer eyJ..." http://localhost:8080/api/orders
```

### Headers Propagated

Downstream services receive:
- `X-User-Id`: User ID from token
- `X-User-Email`: User email
- `X-User-Role`: User role (e.g., CUSTOMER, SELLER, ADMIN)

### Error Responses

All errors return JSON:
```json
{
  "timestamp": "2025-01-01T00:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token has expired"
}
```

## 🔧 Advanced Configuration

### Adding Routes

In Config Server, add to `api-gateway.yaml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: custom-route
          uri: lb://custom-service
          predicates:
            - Path=/api/custom/**
          filters:
            - StripPrefix=1
```

### Rate Limiting

Integrate with Redis for rate limiting (future enhancement).

### CORS & Security

Configure CORS in routes for web clients.

## 🧪 Testing

Run tests:
```bash
./mvnw test
```

Includes unit tests for filters, integration tests for routing.

## 🤝 Contributing

Contributions welcome! See [Contributing Guidelines](../CONTRIBUTING.md).

## 📄 License

Licensed under Apache License 2.0.

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/your-org/shopflow-backend/issues)
- **Docs**: [ShopFlow Wiki](https://github.com/your-org/shopflow-backend/wiki)

---

**Securing the gateway to your microservices empire. Authenticate, route, conquer! 🛡️**
