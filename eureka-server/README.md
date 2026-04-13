# 🔍 Eureka Server - Service Registry & Discovery

[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.0-brightgreen?style=flat-square)](https://spring.io/projects/spring-cloud)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen?style=flat-square)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](#license)

> **The backbone of ShopFlow's microservices architecture** - A highly available, scalable service registry that enables automatic service discovery, load balancing, and fault tolerance across distributed systems.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
  - [Prerequisites](#prerequisites)
  - [Installation & Setup](#installation--setup)
  - [Running the Server](#running-the-server)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Service Registration](#service-registration)
- [Monitoring & Management](#monitoring--management)
- [Troubleshooting](#troubleshooting)
- [Performance Tuning](#performance-tuning)
- [Contributing](#contributing)
- [Resources](#resources)

---

## 🎯 Overview

**Eureka Server** is the central service registry and discovery mechanism for the ShopFlow microservices architecture. It leverages Netflix's Eureka framework integrated with Spring Cloud to provide:

- **Dynamic Service Registration**: Microservices automatically register themselves at startup
- **Automatic Service Discovery**: Applications discover other services without hardcoded URLs
- **Health Monitoring**: Real-time health status of registered services
- **Load Balancing**: Client-side load balancing through service instances
- **Fault Tolerance**: Graceful handling of service failures and network partitions
- **Distributed System Management**: Central hub for managing 10+ interdependent microservices

### Why Eureka?

In a microservices ecosystem, services need to discover and communicate with each other dynamically. Eureka eliminates the need for:
- Static IP addresses or hardcoded URLs
- Manual service endpoint management
- Complex DNS configuration

---

## ✨ Key Features

| Feature | Description |
|---------|-------------|
| **Self-Registration** | Services auto-register when starting; auto-deregister on shutdown |
| **Client-Side Discovery** | Clients maintain local cache of service registry for fast lookups |
| **Heartbeat Mechanism** | Services send periodic heartbeats (30s intervals) to maintain registration |
| **Service Zones** | Support for multi-zone deployments and failover strategies |
| **RESTful API** | Complete REST API for querying and managing services |
| **Web Dashboard** | Interactive UI for viewing registered services and their instances |
| **Resilience** | Handles network partitions gracefully with self-preservation mode |
| **Security-Ready** | Integration points for authentication and authorization |

---

## 🏗️ Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Eureka Server (8761)                     │
│                  ┌──────────────────┐                       │
│                  │   Service        │                       │
│                  │   Registry       │                       │
│                  │   Database       │                       │
│                  └──────────────────┘                       │
│                    ▲     ▲     ▲     ▲                      │
│         ┌──────────┘     │     │     └──────────┐           │
│         │                │     │                │           │
│    Heartbeat        Heartbeat  │            Heartbeat       │
│    (30s)            (30s)      │            (30s)           │
│         │                │     │                │           │
└─────────┼────────────────┼─────┼────────────────┼──────────┘
          │                │     │                │
      ┌───▼──┐         ┌───▼──┐┌───▼──┐      ┌───▼──┐
      │User  │         │Order ││Auth  │      │Admin │
      │Svc   │         │Svc   ││Svc   │  ... │Svc   │
      └──────┘         └──────┘└──────┘      └──────┘

                   Microservices Layer
```

### Request Flow

```
1. Service Startup
   └─> Eureka Client registers instance
       └─> Sends initial heartbeat
           └─> Service marked as UP

2. Client Lookup
   └─> Application queries Eureka for service location
       └─> Receives list of available instances
           └─> Caches locally for performance
               └─> Applies load balancing strategy

3. Inter-Service Communication
   └─> Service A discovers Service B
       └─> Ribbon selects an instance
           └─> Request forwarded with retry logic
               └─> Fallback if instance fails

4. Service Deregistration
   └─> Service shutdown detected
       └─> Graceful deregistration sent to Eureka
           └─> Other services notified
               └─> Traffic redirected to healthy instances
```

---

## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **Maven 3.8.1** or higher
- **Spring Cloud 2025.1.0** compatible environment
- Minimum **512MB RAM** (recommended **1GB** for production)
- Port **8761** available (default Eureka port)

### Installation & Setup

#### 1. Clone/Navigate to Repository

```bash
cd C:\Users\mural\Documents\intellij-Idea-Projects\shopflow-backend\eureka-server
```

#### 2. Verify Java Installation

```bash
java -version  # Should show Java 21+
mvn -version   # Should show Maven 3.8.1+
```

#### 3. Build the Project

```bash
# Using Maven wrapper (recommended)
./mvnw clean build

# Or using installed Maven
mvn clean package
```

#### 4. Create Application Properties (if needed)

The default configuration is in `src/main/resources/application.yml`

### Running the Server

#### Option A: Using Maven

```bash
./mvnw spring-boot:run
```

#### Option B: Using JAR File

```bash
./mvnw clean package
java -jar target/eureka-server-0.0.1-SNAPSHOT.jar
```

#### Option C: Docker

```bash
# Build Docker image
docker build -t shopflow/eureka-server .

# Run container
docker run -d \
  -p 8761:8761 \
  --name eureka-server \
  shopflow/eureka-server
```

#### Verify Installation

Once running, verify with:

```bash
# Test Eureka health
curl http://localhost:8761/eureka/status

# Access Web Dashboard
open http://localhost:8761
```

**Expected Output:**
- Web dashboard shows "Instances currently registered with Eureka"
- Status page shows "UP" status

---

## ⚙️ Configuration

### Current Configuration (application.yml)

```yaml
server:
  port: 8761                           # Eureka server port

spring:
  application:
    name: eureka-server               # Application identifier

eureka:
  instance:
    hostname: localhost               # Instance hostname
  client:
    register-with-eureka: false       # Don't register server with itself
    fetch-registry: false              # Server doesn't need registry copy
    service-url:
      defaultZone: http://localhost:8761/eureka/
  server:
    wait-time-in-ms-when-sync-empty: 0  # Quick startup
```

### Advanced Configuration Options

#### High Availability Configuration

```yaml
eureka:
  instance:
    hostname: eureka-server-1.example.com
    prefer-ip-address: false
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://eureka-server-2.example.com:8761/eureka/,http://eureka-server-3.example.com:8761/eureka/
  server:
    enable-self-preservation: true
    eviction-interval-timer-in-ms: 60000
    response-cache-update-interval-ms: 30000
```

#### Production Configuration

```yaml
server:
  port: 8761
  compression:
    enabled: true
    min-response-size: 1024

spring:
  application:
    name: eureka-server
  security:
    basic:
      enabled: true
      user:
        name: eureka
        password: ${EUREKA_PASSWORD}  # Use environment variable

eureka:
  instance:
    hostname: eureka.shopflow.prod
    prefer-ip-address: false
    lease-renewal-interval-in-seconds: 30
    lease-expiration-duration-in-seconds: 90
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://eureka-server-standby.shopflow.prod:8761/eureka/
  server:
    enable-self-preservation: true
    eviction-interval-timer-in-ms: 60000
    renewal-percent-threshold: 0.85
    expected-client-renewal-interval-seconds: 30
    max-threads-for-peer-replication: 20
```

#### Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `SERVER_PORT` | 8761 | Eureka server port |
| `EUREKA_PASSWORD` | - | Authentication password |
| `EUREKA_HOSTNAME` | localhost | Hostname for registration |
| `EUREKA_ENABLE_SELF_PRESERVATION` | true | Enable self-preservation mode |

---

## 🔌 API Documentation

### Core Endpoints

#### 1. Application Registration

**Register a new application instance**

```http
POST /eureka/apps/{appName}
Content-Type: application/json

{
  "instance": {
    "instanceId": "service-instance-1",
    "hostName": "192.168.1.100",
    "ipAddr": "192.168.1.100",
    "port": {
      "$": 8080,
      "@enabled": true
    },
    "appName": "USER-SERVICE",
    "status": "UP"
  }
}

Response: 204 No Content
```

#### 2. Query All Applications

**Get registry of all registered applications**

```http
GET /eureka/apps

Response: 200 OK
{
  "applications": {
    "versions__delta": 1,
    "apps__hashcode": "UP_1_",
    "application": [
      {
        "name": "USER-SERVICE",
        "instance": [
          {
            "instanceId": "user-service-1",
            "hostName": "user-svc.local",
            "status": "UP",
            ...
          }
        ]
      }
    ]
  }
}
```

#### 3. Query Specific Application

**Get instances of a specific application**

```http
GET /eureka/apps/{appName}

Example: GET /eureka/apps/USER-SERVICE

Response: 200 OK
{
  "application": {
    "name": "USER-SERVICE",
    "instance": [...]
  }
}
```

#### 4. Get Specific Instance

**Get details of a specific instance**

```http
GET /eureka/apps/{appName}/{instanceId}

Example: GET /eureka/apps/USER-SERVICE/user-service-1

Response: 200 OK
{
  "instance": {
    "instanceId": "user-service-1",
    "appName": "USER-SERVICE",
    "status": "UP",
    ...
  }
}
```

#### 5. Heartbeat (Keep-Alive)

**Send heartbeat to keep registration active**

```http
PUT /eureka/apps/{appName}/{instanceId}

Response: 200 OK
```

#### 6. Deregister Instance

**Remove instance from registry**

```http
DELETE /eureka/apps/{appName}/{instanceId}

Response: 200 OK
```

#### 7. Server Status

**Check Eureka server health and statistics**

```http
GET /eureka/status

Response: 200 OK
HTML page with server statistics
```

#### 8. Status Update

**Change instance status**

```http
PUT /eureka/apps/{appName}/{instanceId}/status?value=DOWN

Possible values: UP, DOWN, STARTING, OUT_OF_SERVICE, UNKNOWN
```

---

## 📱 Service Registration

### How Services Register

#### Step 1: Add Eureka Client Dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

#### Step 2: Enable Eureka Client

```java
@SpringBootApplication
@EnableEurekaClient
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

#### Step 3: Configure Application

```yaml
server:
  port: 8081

spring:
  application:
    name: user-service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    lease-renewal-interval-in-seconds: 30
    lease-expiration-duration-in-seconds: 90
```

#### Step 4: Verify Registration

```bash
# Check if service is registered
curl http://localhost:8761/eureka/apps/USER-SERVICE

# Should return the service with UP status
```

### Health Checks

Services can implement custom health checks:

```java
@Component
public class CustomHealthIndicator extends AbstractHealthIndicator {
    @Override
    protected void doHealthCheck(Health.Builder builder) {
        // Custom health check logic
        if (isServiceHealthy()) {
            builder.up();
        } else {
            builder.down();
        }
    }
}
```

---

## 📊 Monitoring & Management

### Web Dashboard

Access the interactive dashboard:

```
http://localhost:8761
```

**Dashboard Features:**
- Real-time list of registered services
- Instance status and health
- Refresh registry view
- Service instance details

### Metrics & Statistics

**Available Endpoints:**

```bash
# Server statistics
curl http://localhost:8761/eureka/status

# Detailed registry information
curl http://localhost:8761/eureka/apps

# Health endpoint (if actuator enabled)
curl http://localhost:8761/actuator/health
```

### Enable Spring Boot Actuator (Optional)

Add to `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

Then add dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Monitoring Tools Integration

**Prometheus Metrics:**
```bash
curl http://localhost:8761/actuator/prometheus
```

**Health Check:**
```bash
curl http://localhost:8761/actuator/health
```

---

## 🔧 Troubleshooting

### Common Issues & Solutions

#### Issue 1: Port 8761 Already in Use

**Symptoms:** `Address already in use`

**Solution:**
```bash
# Find process using port 8761
netstat -ano | findstr :8761

# Kill the process (replace PID)
taskkill /PID <PID> /F

# Or use different port
java -jar eureka-server.jar --server.port=8762
```

#### Issue 2: Services Not Registering

**Symptoms:** Dashboard shows "No instances available"

**Check:**
```bash
# 1. Verify Eureka server is running
curl http://localhost:8761/eureka/status

# 2. Check service-url configuration
# In client service, verify:
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

# 3. Check logs for registration errors
tail -f logs/eureka-server.log
```

#### Issue 3: Instances Marked as DOWN

**Symptoms:** Dashboard shows DOWN status but service is running

**Causes & Solutions:**
```yaml
# Increase heartbeat intervals
eureka:
  instance:
    lease-renewal-interval-in-seconds: 30      # Default: 30
    lease-expiration-duration-in-seconds: 90   # Default: 90

# Disable self-preservation if needed
eureka:
  server:
    enable-self-preservation: false
```

#### Issue 4: High Memory Usage

**Symptoms:** Java process consuming excessive memory

**Solutions:**
```bash
# Limit heap size
java -Xmx512m -Xms256m -jar eureka-server.jar

# Enable garbage collection logging
java -Xmx512m -Xms256m \
  -XX:+PrintGCDetails \
  -XX:+PrintGCDateStamps \
  -Xloggc:gc.log \
  -jar eureka-server.jar
```

#### Issue 5: Self-Preservation Mode Warnings

**Message:** "EMERGENCY! EUREKA MAY BE INCORRECTLY CONFIGURED"

**Explanation:** Eureka hasn't received expected heartbeats. This can occur in:
- Development environments with slow heartbeats
- Network connectivity issues
- Service startup delays

**Solution for Development:**
```yaml
eureka:
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 5000
```

**Solution for Production:**
```yaml
eureka:
  server:
    enable-self-preservation: true
    renewal-percent-threshold: 0.85
```

---

## ⚡ Performance Tuning

### Optimization Strategies

#### 1. Cache Configuration

```yaml
eureka:
  server:
    response-cache-update-interval-ms: 30000        # Cache update frequency
    response-cache-auto-expiration-in-seconds: 180  # Cache expiry
    use-read-only-response-cache: true              # Use read-only cache
```

#### 2. Connection Pool Tuning

```yaml
eureka:
  server:
    max-threads-for-peer-replication: 20
    min-available-instances-for-peer-replication: 0
    peer-node-connect-timeout-ms: 200
    peer-node-read-timeout-ms: 200
```

#### 3. JVM Tuning

```bash
java -server \
  -Xmx1g \
  -Xms1g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ParallelRefProcEnabled \
  -jar eureka-server.jar
```

#### 4. Database Optimization

For production with persistent registry:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

---

## 🤝 Contributing

### Development Workflow

1. **Create Feature Branch**
   ```bash
   git checkout -b feature/your-feature
   ```

2. **Make Changes**
   ```bash
   # Edit source files
   # Run tests
   ./mvnw test
   ```

3. **Build & Test**
   ```bash
   ./mvnw clean package
   ```

4. **Commit Changes**
   ```bash
   git commit -m "feat: add your feature description"
   ```

5. **Push & Create Pull Request**
   ```bash
   git push origin feature/your-feature
   ```

### Testing

```bash
# Run unit tests
./mvnw test

# Run integration tests
./mvnw verify

# Run specific test class
./mvnw test -Dtest=EurekaServerApplicationTests
```

### Code Style

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use meaningful variable and method names
- Add Javadoc for public APIs
- Keep methods focused and concise

---

## 📚 Resources

### Official Documentation

- [Spring Cloud Netflix Eureka](https://docs.spring.io/spring-cloud-netflix/reference/spring-cloud-netflix.html)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Netflix Eureka GitHub](https://github.com/Netflix/eureka)

### Guides & Tutorials

- [Service Registration and Discovery](https://spring.io/guides/gs/service-registration-and-discovery/)
- [Microservices with Spring Cloud](https://www.baeldung.com/spring-cloud)
- [Eureka Server Setup Guide](https://www.baeldung.com/spring-cloud-netflix-eureka)

### Related Services in ShopFlow

- **API Gateway** - Routes requests to discovered services
- **Config Server** - Centralized configuration management
- **User Service** - User management microservice
- **Order Service** - Order processing microservice
- **Payment Service** - Payment processing microservice
- **Admin Service** - Administrative operations

---

## 📝 Version History

| Version | Date | Changes |
|---------|------|---------|
| 0.0.1 | 2026-04-13 | Initial release |

---

## 📞 Support & Contact

For issues, questions, or suggestions:

1. **Check the [Troubleshooting](#troubleshooting) section**
2. **Review [Resources](#resources) for documentation**
3. **Contact the development team**
4. **Open an issue in the repository**

---

## 📄 License

This project is licensed under the MIT License. See the LICENSE file for details.

---

## 🙏 Acknowledgments

- Spring Cloud team for Netflix Eureka integration
- ShopFlow development team
- Microservices community for best practices

---

**Last Updated:** April 13, 2026

**Maintained By:** ShopFlow Development Team

---

*Happy Service Discovery! 🎉*

