# ShopFlow Auth Service

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.6.0-black.svg)](https://kafka.apache.org/)
[![JWT](https://img.shields.io/badge/JWT-0.12.3-yellow.svg)](https://github.com/jwtk/jjwt)

A robust, production-ready authentication microservice built with Spring Boot for the ShopFlow e-commerce platform. Implements secure user authentication, authorization, and user management with JWT tokens, OTP verification, and comprehensive security features.

## 🚀 Key Features

### 🔐 Authentication & Authorization
- **JWT-based Authentication**: Stateless authentication with access and refresh tokens
- **OTP Email Verification**: Secure email verification during registration and password reset
- **Multi-factor Ready**: Extensible architecture for additional authentication factors
- **Session Management**: Secure logout with token blacklisting

### 👥 User Management
- **User Registration**: Complete user onboarding with email verification
- **Login/Logout**: Secure authentication flows with proper session handling
- **Password Reset**: Secure password recovery via OTP
- **Admin Controls**: User banning/unbanning capabilities for administrators

### 🏗️ Microservice Architecture
- **Service Discovery**: Eureka client integration for dynamic service registration
- **Event-Driven**: Kafka integration for asynchronous event publishing
- **Configuration Management**: Spring Cloud Config ready
- **Health Monitoring**: Actuator endpoints for service health checks

### 🛡️ Security Features
- **Spring Security Integration**: Comprehensive security configuration
- **Token Blacklisting**: Prevents reuse of compromised tokens
- **Rate Limiting Ready**: Infrastructure for implementing rate limiting
- **Input Validation**: Bean validation with custom constraints
- **CORS Configuration**: Configurable cross-origin resource sharing

## 🏛️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │  Auth Service   │    │  Other Services │
│                 │    │                 │    │                 │
│ • Route requests│◄──►│ • JWT Auth      │◄──►│ • User data     │
│ • Load balancing│    │ • User Mgmt     │    │ • Business logic│
│ • Authentication│    │ • Token refresh │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│     Eureka      │    │   PostgreSQL    │    │      Redis      │
│   Discovery     │    │   User Data     │    │   OTP Cache     │
│                 │    │   + Flyway      │    │   Token Store   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         ▲                       ▲                       ▲
         └───────────────────────┼───────────────────────┘
                                 ▼
                   ┌─────────────────┐
                   │    Kafka        │
                   │ Event Streaming │
                   │                 │
                   └─────────────────┘
```

## 🛠️ Technology Stack

| Component | Technology | Version | Purpose |
|-----------|------------|---------|---------|
| **Framework** | Spring Boot | 3.2.5 | Microservice framework |
| **Language** | Java | 21 | Programming language |
| **Security** | Spring Security | 6.2.1 | Authentication & authorization |
| **Database** | PostgreSQL | 15+ | User data persistence |
| **Cache** | Redis | 7+ | OTP and session storage |
| **Messaging** | Apache Kafka | 3.6.0 | Event-driven communication |
| **Discovery** | Netflix Eureka | 4.0.3 | Service registration/discovery |
| **Migration** | Flyway | 10.10.0 | Database schema versioning |
| **JWT** | JJWT | 0.12.3 | JSON Web Token handling |
| **Mapping** | MapStruct | 1.5.5 | Object mapping |
| **Validation** | Hibernate Validator | 8.0.1 | Input validation |

## 📋 Prerequisites

- **Java**: JDK 21 or later
- **Maven**: 3.8+ for dependency management
- **PostgreSQL**: 15+ database server
- **Redis**: 7+ in-memory data store
- **Kafka**: 3.6.0 message broker
- **Eureka Server**: For service discovery (optional for local development)

## 🚀 Quick Start

### 1. Clone and Navigate
```bash
git clone <repository-url>
cd shopflow-backend/auth-service
```

### 2. Database Setup
```sql
-- Create database
CREATE DATABASE auth_db;

-- Update connection details in application.yaml if needed
```

### 3. Start Infrastructure
```bash
# From project root
docker-compose up -d zookeeper kafka redis
```

### 4. Configure Environment
```yaml
# src/main/resources/application.yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: your_username
    password: your_password

  data:
    redis:
      host: localhost
      port: 6379

  kafka:
    bootstrap-servers: localhost:9092
```

### 5. Build and Run
```bash
# Build the service
mvn clean compile

# Run the application
mvn spring-boot:run
```

The service will start on `http://localhost:8081`

## ⚙️ Configuration

### Core Configuration
```yaml
server:
  port: 8081

spring:
  application:
    name: auth-service

  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: postgres
    password: your_password

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  flyway:
    enabled: true
    locations: classpath:db/migration

  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms

  kafka:
    bootstrap-servers: localhost:9092
```

### JWT Configuration
```yaml
application:
  jwt:
    secret: "your-256-bit-secret-key-here"
    access-token-expiry-ms: 900000    # 15 minutes
    refresh-token-expiry-ms: 604800000 # 7 days

  otp:
    expiry-minutes: 5
```

### Eureka Configuration
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
```

## 📡 API Endpoints

### Public Endpoints

#### User Registration
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### Email Verification
```http
POST /api/auth/verify-email
Content-Type: application/json

{
  "email": "user@example.com",
  "otp": "123456"
}
```

#### User Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

#### Token Refresh
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Password Reset
```http
POST /api/auth/forgot-password
Content-Type: application/json

{
  "email": "user@example.com"
}
```

```http
POST /api/auth/reset-password
Content-Type: application/json

{
  "email": "user@example.com",
  "otp": "123456",
  "newPassword": "NewSecurePass123!"
}
```

### Protected Endpoints

#### Logout
```http
POST /api/auth/logout
Authorization: Bearer <access_token>
```

### Internal Endpoints (Service-to-Service)

#### Get User by ID
```http
GET /api/auth/users/internal/{userId}
```

#### Get All Users (Paginated)
```http
GET /api/auth/users/internal/all?page=0&size=20&sort=email
```

#### Ban User
```http
PUT /api/auth/users/internal/{userId}/ban
```

#### Unban User
```http
PUT /api/auth/users/internal/{userId}/unban
```

## 🗄️ Database Schema

### Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(is_active);
```

### Flyway Migrations
Database schema is managed through Flyway migrations located in `src/main/resources/db/migration/`.

## 🔒 Security Implementation

### JWT Token Flow
1. **Login**: User credentials → Validate → Generate access + refresh tokens
2. **Access**: Bearer token in Authorization header
3. **Refresh**: Use refresh token to get new access token
4. **Logout**: Blacklist access token, remove refresh token

### Password Security
- **BCrypt Hashing**: Industry-standard password hashing
- **Salt Generation**: Automatic salt generation per user
- **Complexity Requirements**: Enforced through validation

### OTP Security
- **Time-based**: 5-minute expiration in Redis
- **Single Use**: OTP invalidated after successful verification
- **Email Delivery**: Secure OTP delivery via email service

### Rate Limiting Considerations
- Infrastructure ready for Redis-based rate limiting
- Configurable limits per endpoint
- Protection against brute force attacks

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Test Coverage
- Service layer testing with Mockito
- Controller testing with MockMvc
- Repository testing with TestContainers (recommended)

## 📊 Monitoring & Observability

### Health Checks
```http
GET /actuator/health
```

### Application Info
```http
GET /actuator/info
```

### Metrics (when configured)
- JVM metrics
- HTTP request metrics
- Database connection pool metrics
- Custom business metrics

## 🔧 Development

### Code Style
- Follow Spring Boot conventions
- Use Lombok for boilerplate reduction
- MapStruct for DTO mapping
- Comprehensive logging with SLF4J

### Project Structure
```
auth-service/
├── src/main/java/com/shopflow/auth/
│   ├── AuthServiceApplication.java
│   ├── config/           # Configuration classes
│   ├── controller/       # REST controllers
│   ├── dto/             # Data transfer objects
│   ├── entity/          # JPA entities
│   ├── event/           # Kafka events
│   ├── exception/       # Custom exceptions
│   ├── publisher/       # Kafka publishers
│   ├── repository/      # Data repositories
│   ├── security/        # Security configuration
│   └── service/         # Business logic
├── src/main/resources/
│   ├── application.yaml
│   └── db/migration/    # Flyway scripts
└── src/test/            # Test classes
```

## 🚀 Deployment

### Docker Build
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/auth-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Kubernetes Deployment
- ConfigMap for application configuration
- Secret for sensitive data (JWT secret, DB credentials)
- Service for internal communication
- Ingress for external access

### Production Checklist
- [ ] Environment-specific configuration
- [ ] Secret management (Vault, AWS Secrets Manager)
- [ ] Database connection pooling optimization
- [ ] Redis cluster configuration
- [ ] Kafka cluster setup
- [ ] Monitoring and alerting setup
- [ ] Load balancing configuration
- [ ] SSL/TLS termination
- [ ] Backup and recovery procedures

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Write comprehensive unit tests
- Update documentation for API changes
- Follow conventional commit messages
- Ensure all tests pass before submitting PR

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support and questions:
- Create an issue in the repository
- Contact the development team
- Check the [Wiki](wiki) for detailed documentation

---

**Built with ❤️ for the ShopFlow platform**</content>
<parameter name="filePath">C:\Users\mural\Documents\intellij-Idea-Projects\shopflow-backend\auth-service\README.md
