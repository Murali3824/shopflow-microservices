# 📧 Notification Service

> **Enterprise-grade, event-driven notification microservice** for the ShopFlow e-commerce platform. Delivers real-time email notifications across the entire order lifecycle with robust error handling, HTML templating, and Kafka event streaming.

---

## 🎯 Service Overview

The **Notification Service** is a Spring Boot microservice that handles asynchronous email notifications triggered by domain events from other services in the ShopFlow ecosystem. It leverages **Apache Kafka** for event-driven communication, **Thymeleaf** for dynamic HTML email templates, and **PostgreSQL** for notification persistence and audit trails.

### Key Capabilities
- 📨 **Multi-channel Event Processing**: Order, Payment, Product, Return, and OTP events
- 🎨 **Rich HTML Email Templates**: Dynamic Thymeleaf-based email rendering
- 💾 **Notification Persistence**: Complete audit trail of all sent notifications
- 🔄 **Async Event Consumption**: Kafka-based event-driven architecture
- 🛡️ **Security-First Design**: Spring Security integration, request validation
- 📡 **Service Discovery**: Eureka client registration
- ⚙️ **Centralized Configuration**: Config server integration
- 🗄️ **Database Versioning**: Flyway migration support

---

## 🏗️ Architecture

### Technology Stack
| Component | Technology | Version |
|-----------|-----------|---------|
| **Runtime** | Java | 21+ |
| **Framework** | Spring Boot | 3.2.5 |
| **Cloud** | Spring Cloud | 2023.0.1 |
| **Message Queue** | Apache Kafka | Spring Kafka |
| **Database** | PostgreSQL | Latest |
| **Migration** | Flyway | 10.10.0 |
| **Email** | Spring Mail | JavaMail |
| **Templates** | Thymeleaf | Spring Starter |
| **Validation** | Jakarta Validation | Built-in |
| **Serialization** | Jackson | Latest |
| **Build Tool** | Maven | 3.x+ |

### Service Dependencies
```
┌─────────────────────────────────────────┐
│   Notification Service (Port 8087)      │
├─────────────────────────────────────────┤
│ ↓ Kafka Consumer ← Order Events         │
│ ↓ Kafka Consumer ← Payment Events       │
│ ↓ Kafka Consumer ← Product Events       │
│ ↓ Kafka Consumer ← Return Events        │
│ ↓ Kafka Consumer ← OTP Events           │
├─────────────────────────────────────────┤
│ → PostgreSQL (Notification Store)       │
│ → SMTP Server (Email Provider)          │
│ → Eureka Server (Service Registry)      │
│ → Config Server (Centralized Config)    │
└─────────────────────────────────────────┘
```

---

## 📂 Project Structure

```
notification-service/
├── src/main/java/com/shopflow/notification/
│   ├── NotificationServiceApplication.java          # Spring Boot entry point
│   ├── config/                                      # Configuration classes
│   ├── controller/
│   │   └── NotificationController.java              # REST endpoints for manual testing
│   ├── service/
│   │   ├── EmailService.java                        # Email service interface
│   │   ├── EmailServiceImpl.java                     # Email sending logic
│   │   ├── NotificationService.java                 # Notification business logic
│   │   ├── NotificationServiceImpl.java              # Implementation
│   │   ├── NotificationPersistenceService.java      # Persistence interface
│   │   └── NotificationPersistenceServiceImpl.java   # Persistence implementation
│   ├── consumer/
│   │   ├── OrderEventConsumer.java                  # Order events handler
│   │   ├── PaymentEventConsumer.java                # Payment events handler
│   │   ├── ProductEventConsumer.java                # Product events handler
│   │   ├── ReturnEventConsumer.java                 # Return events handler
│   │   └── OtpEventConsumer.java                    # OTP events handler
│   ├── entity/
│   │   └── Notification.java                        # JPA entity for notifications
│   ├── repository/
│   │   └── NotificationRepository.java              # Data access layer
│   ├── dto/                                         # Data transfer objects
│   ├── event/                                       # Event classes (consumed)
│   ├── exception/                                   # Custom exceptions
│   └── security/                                    # Security configurations
├── src/main/resources/
│   ├── application.yaml                             # Application configuration
│   ├── db/migration/                                # Flyway database migrations
│   └── templates/
│       ├── order-confirmation.html                  # Order confirmation email
│       ├── payment-receipt.html                     # Payment confirmation email
│       ├── order-shipped.html                       # Shipment notification email
│       ├── order-delivered.html                     # Delivery confirmation email
│       ├── refund-initiated.html                    # Refund email
│       ├── low-stock-alert.html                     # Stock alert email
│       └── otp-email.html                           # OTP verification email
├── pom.xml                                          # Maven dependencies
└── README.md                                        # This file
```

---

## 🚀 Getting Started

### Prerequisites
- **Java 21+** installed and configured
- **Maven 3.x+** for dependency management
- **PostgreSQL 12+** database running
- **Kafka** cluster running (for event consumption)
- **SMTP Server** configured (for email sending)

### Installation & Setup

#### 1. Clone the Repository
```bash
git clone https://github.com/Murali3824/shopflow-microservices.git
cd shopflow-microservices/notification-service
```

#### 2. Configure Environment Variables
Create or update `application.yaml`:
```yaml
server:
  port: 8087

spring:
  application:
    name: notification-service
  
  # Database Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/shopflow_notification
    username: your_db_user
    password: your_db_password
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  # Email Configuration
  mail:
    host: smtp.gmail.com
    port: 587
    username: your_email@gmail.com
    password: your_app_password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
  
  # Kafka Configuration
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: notification-service-group
      auto-offset-reset: earliest
  
  # Centralized Config Server
  config:
    import: optional:configserver:http://localhost:8888
  
  # Service Discovery
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

#### 3. Build the Application
```bash
mvn clean install
```

#### 4. Run Database Migrations
```bash
mvn flyway:migrate
```

#### 5. Start the Service
```bash
mvn spring-boot:run
```

The service will be available at: `http://localhost:8087`

---

## 📡 Event-Driven Architecture

### Kafka Topics & Consumers

The Notification Service consumes events from multiple Kafka topics:

#### **Order Events** (`OrderEventConsumer.java`)
- **Topic**: `order-placed-events`
- **Trigger**: Order placed in Order Service
- **Actions**: Sends order confirmation email

#### **Payment Events** (`PaymentEventConsumer.java`)
- **Topic**: `payment-completed-events`
- **Trigger**: Payment processed in Payment Service
- **Actions**: Sends payment receipt email

#### **Shipment Events** (`OrderEventConsumer.java`)
- **Topic**: `order-shipped-events`
- **Trigger**: Order shipped in Fulfillment Service
- **Actions**: Sends shipment notification with tracking

#### **Delivery Events** (`OrderEventConsumer.java`)
- **Topic**: `order-delivered-events`
- **Trigger**: Order delivered
- **Actions**: Sends delivery confirmation email

#### **Product Events** (`ProductEventConsumer.java`)
- **Topic**: `product-low-stock-events`
- **Trigger**: Product inventory below threshold
- **Actions**: Alerts seller about low stock

#### **Return/Refund Events** (`ReturnEventConsumer.java`)
- **Topic**: `return-approved-events`
- **Trigger**: Return request approved
- **Actions**: Sends refund initiation email

#### **OTP Events** (`OtpEventConsumer.java`)
- **Topic**: `otp-requested-events`
- **Trigger**: User requests OTP (Registration, Password Reset)
- **Actions**: Sends OTP verification email

---

## 📧 Email Templates

All email templates are built with **Thymeleaf** for dynamic variable substitution:

### Template Variables Reference

**Order Confirmation** (`order-confirmation.html`)
```html
Thymeleaf Variables:
- fullName: User's full name
- orderId: Order ID
- totalAmount: Order total price
- paymentMethod: Payment method used
```

**Payment Receipt** (`payment-receipt.html`)
```html
Thymeleaf Variables:
- fullName: User's full name
- orderId: Order ID
- amount: Payment amount
- gateway: Payment gateway (Stripe, PayPal, etc.)
- gatewayPaymentId: Transaction ID
```

**Order Shipped** (`order-shipped.html`)
```html
Thymeleaf Variables:
- fullName: User's full name
- orderId: Order ID
- trackingNumber: Shipping tracking number
```

**Order Delivered** (`order-delivered.html`)
```html
Thymeleaf Variables:
- fullName: User's full name
- orderId: Order ID
```

**Refund Initiated** (`refund-initiated.html`)
```html
Thymeleaf Variables:
- fullName: User's full name
- orderId: Order ID
- refundAmount: Refund amount
```

**Low Stock Alert** (`low-stock-alert.html`)
```html
Thymeleaf Variables:
- sellerName: Seller's name
- productName: Product name
- skuCode: Product SKU
- currentStock: Current inventory count
- lowStockThreshold: Alert threshold
```

**OTP Email** (`otp-email.html`)
```html
Thymeleaf Variables:
- name: User's name
- otp: One-time password
```

---

## 🔌 API Endpoints

### GET Health Check
```http
GET /actuator/health
```
**Response**: Service health status

### POST Send Manual Notification
```http
POST /api/notifications/send
Content-Type: application/json

{
  "recipientEmail": "user@example.com",
  "notificationType": "ORDER_CONFIRMATION",
  "data": {
    "orderId": "ORD-123456",
    "fullName": "John Doe",
    "totalAmount": 99.99,
    "paymentMethod": "Credit Card"
  }
}
```

### GET Notification History
```http
GET /api/notifications?userId=123&limit=10
```
**Response**: List of user's notification history

### GET Notification Details
```http
GET /api/notifications/{notificationId}
```
**Response**: Detailed notification information

---

## 📊 Database Schema

### Notification Entity
```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    failed_at TIMESTAMP,
    error_message TEXT,
    retry_count INT DEFAULT 0
);
```

### Status Enum
- `PENDING`: Awaiting processing
- `SENT`: Successfully delivered
- `FAILED`: Delivery failed
- `BOUNCED`: Email bounced

---

## 🔐 Security Features

### Authentication & Authorization
- **Spring Security** integration
- **JWT Token Validation** for inter-service communication
- **OAuth2 Support** (optional)
- **CORS Configuration** for cross-service requests

### Data Protection
- **SQL Injection Prevention**: Parameterized queries via JPA
- **Email Validation**: RFC 5322 compliant validation
- **Rate Limiting**: Kafka consumer group configs prevent overload
- **Error Handling**: Sensitive information never logged

---

## 🧪 Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Test Kafka Consumer Locally
```bash
# Ensure Kafka is running
# Send test event to Kafka topic
kafka-console-producer.sh --broker-list localhost:9092 --topic order-placed-events
```

---

## 📈 Monitoring & Logging

### Application Logging
- **Log Level**: Configurable via `application.yaml`
- **Log Format**: JSON (for ELK stack integration)
- **Key Metrics**:
  - Email delivery success/failure rates
  - Kafka consumer lag
  - Template rendering time
  - Database operation duration

### Example Log Output
```
2026-04-30T10:15:32.123Z INFO [notification-service] EmailServiceImpl : Email sent to user@example.com | template=order-confirmation | subject=Order Placed Successfully — #ORD-123456

2026-04-30T10:15:35.456Z ERROR [notification-service] EmailServiceImpl : Failed to send email to seller@shop.com | template=low-stock-alert | error=SMTP connection timeout
```

### Metrics Exposed
- `/actuator/metrics/mail.sent.count`
- `/actuator/metrics/mail.failed.count`
- `/actuator/metrics/kafka.consumer.lag`
- `/actuator/metrics/notification.persistence.time`

---

## 🐛 Troubleshooting

### Issue: Emails Not Being Sent
**Solution**:
1. Verify SMTP credentials in `application.yaml`
2. Check firewall rules for SMTP port (587/465)
3. Enable "Less secure apps" (Gmail)
4. Review logs for `MessagingException`

### Issue: Kafka Consumer Not Receiving Events
**Solution**:
1. Verify Kafka broker connectivity
2. Check topic existence: `kafka-topics.sh --list --bootstrap-server localhost:9092`
3. Verify consumer group: `kafka-consumer-groups.sh --list --bootstrap-server localhost:9092`
4. Check logs for consumer group lag

### Issue: Database Migration Failures
**Solution**:
1. Ensure PostgreSQL is running
2. Verify database credentials
3. Check migration file syntax in `src/main/resources/db/migration/`
4. Run: `mvn flyway:clean` (WARNING: Destructive) then `mvn flyway:migrate`

### Issue: Service Discovery Registration Failed
**Solution**:
1. Verify Eureka server is running on `http://localhost:8761`
2. Check `eureka.client.service-url.defaultZone` configuration
3. Review network connectivity

---

## 🚦 Performance Optimization

### Best Practices
- **Async Email Sending**: Non-blocking Kafka consumer processing
- **Connection Pooling**: HikariCP configured for database
- **Template Caching**: Thymeleaf caches compiled templates
- **Batch Processing**: Process multiple events in single transaction
- **Database Indexing**: Indexes on frequently queried columns

### Scaling Considerations
- **Horizontal Scaling**: Multiple instances share Kafka consumer group
- **Load Balancing**: Use API Gateway for REST endpoints
- **Database Optimization**: Connection pool tuning, query optimization
- **Message Queue Tuning**: Partition allocation for parallel consumption

---

## 📚 Related Services

- **Order Service**: Publishes `order-placed-events`
- **Payment Service**: Publishes `payment-completed-events`
- **Fulfillment Service**: Publishes `order-shipped-events`, `order-delivered-events`
- **Product Service**: Publishes `product-low-stock-events`
- **Return Service**: Publishes `return-approved-events`
- **Auth Service**: Publishes `otp-requested-events`

---

## 📝 Contributing

### Code Standards
- Follow **Google Java Style Guide**
- Use meaningful variable names
- Add JavaDoc for public methods
- Write unit tests for new features
- Keep methods focused and single-responsibility

### Commit Message Format
```
[FEATURE/BUG/DOCS] Brief description

Detailed explanation of changes.
- Specific change 1
- Specific change 2

Closes #ISSUE_NUMBER
```

---

## 📄 License

This project is part of the **ShopFlow Microservices** ecosystem.

---

## 👥 Support & Contact

For issues, questions, or contributions:
- 📧 **Email**: dev@shopflow.com
- 🐛 **Issues**: [GitHub Issues](https://github.com/Murali3824/shopflow-microservices/issues)
- 📖 **Documentation**: [Wiki](https://github.com/Murali3824/shopflow-microservices/wiki)

---

## 🔄 Version History

| Version | Date | Changes |
|---------|------|---------|
| 0.0.1 | 2026-04-30 | Initial release - Core notification service with Kafka integration |

---

**Last Updated**: April 30, 2026 | **Maintained by**: ShopFlow Development Team
