# Config Server 🏗️

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.0-blue.svg)](https://spring.io/projects/spring-cloud)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://opensource.org/licenses/Apache-2.0)

## 🚀 Overview

The **Config Server** is the backbone of configuration management in the ShopFlow Backend microservices ecosystem. As a centralized configuration service built on Spring Cloud Config, it provides dynamic, environment-aware configuration to all microservices, ensuring consistency, scalability, and ease of maintenance across the entire platform.

Imagine a bustling e-commerce empire where every service needs its settings—database connections, API keys, feature flags—without hardcoding them. That's where we shine! This service eliminates configuration drift, enables seamless deployments across dev, staging, and production, and integrates flawlessly with Netflix Eureka for service discovery.

## 🎯 Key Features

- **Centralized Configuration**: All microservice configs in one place, version-controlled and auditable.
- **Environment-Specific Profiles**: Tailor configs for `dev`, `staging`, `prod` without code changes.
- **Native File System Backend**: Configurations stored locally for simplicity and speed (easily switchable to Git, SVN, or cloud storage).
- **Eureka Integration**: Auto-registers with the service registry for high availability.
- **Security-Ready**: Extensible for encryption, authentication, and access control.
- **Hot Reloading**: Services can refresh configs without restarts (with actuator endpoints).
- **Multi-Format Support**: YAML, Properties, JSON—whatever your services prefer.

## 🏛️ Architecture

### How It Works

1. **Configuration Storage**: Configs reside in `src/main/resources/configurations/` as YAML files per service.
2. **Service Requests**: Microservices query the Config Server at startup via `http://config-server:8888/{service-name}/{profile}`.
3. **Dynamic Resolution**: Server merges default (`application.yml`) with service-specific and profile-specific configs.
4. **Eureka Discovery**: Services discover the Config Server via Eureka, enabling load balancing and failover.

### Integration Flow

```
[Microservice] → [Eureka] → [Config Server] → [Configuration Files]
     ↓                ↓            ↓
  Fetches Config   Discovers     Serves Configs
```

### Supported Services

This Config Server manages configurations for:

- **Admin Service**: Administrative operations and dashboards.
- **API Gateway**: Entry point for all client requests.
- **Auth Service**: User authentication and authorization.
- **Notification Service**: Email, SMS, and push notifications.
- **Order Service**: Order processing and management.
- **Payment Service**: Secure payment integrations.
- **Product Service**: Product catalog and inventory.
- **Review Service**: Customer reviews and ratings.
- **Seller Service**: Seller account management.
- **User Service**: User profiles and preferences.

## 🛠️ Setup & Installation

### Prerequisites

- **Java 21** or higher
- **Maven 3.6+**
- **Docker** (for containerized deployment)
- Access to Eureka Server (default: `http://localhost:8761/eureka`)

### Local Development

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-org/shopflow-backend.git
   cd shopflow-backend/config-server
   ```

2. **Build the Project**:
   ```bash
   ./mvnw clean compile
   ```

3. **Run Locally**:
   ```bash
   ./mvnw spring-boot:run
   ```
   The server starts on `http://localhost:8888`.

4. **Verify**:
   - Health Check: `curl http://localhost:8888/actuator/health`
   - Config Endpoint: `curl http://localhost:8888/admin-service/default`

### Docker Deployment

1. **Build Image**:
   ```bash
   docker build -t shopflow/config-server:latest .
   ```

2. **Run with Docker Compose** (from project root):
   ```bash
   docker-compose up config-server
   ```

### Configuration

Key settings in `application.yml`:

- **Port**: 8888
- **Profile**: `native` (local file system)
- **Search Locations**: `classpath:/configurations`
- **Eureka Registration**: Enabled for service discovery

For production, switch to Git backend by changing `spring.profiles.active` to `git` and adding `spring.cloud.config.server.git.uri`.

## 📖 Usage

### For Microservices

To consume configs, add to your service's `bootstrap.yml`:

```yaml
spring:
  application:
    name: your-service-name
  cloud:
    config:
      uri: http://config-server:8888
      fail-fast: true
```

### API Endpoints

- **GET** `/{application}/{profile}[/{label}]`: Retrieve config for an application and profile.
- **GET** `/{application}-{profile}.yml`: YAML format.
- **GET** `/{application}-{profile}.properties`: Properties format.
- **POST** `/actuator/refresh`: Refresh configs (requires actuator).

Example: `http://localhost:8888/auth-service/prod` returns merged config for auth-service in production.

### Environment Profiles

- **default**: Base configurations.
- **dev**: Development overrides.
- **staging**: Staging environment.
- **prod**: Production settings.

Create profile-specific files like `auth-service-prod.yaml` for environment-specific configs.

## 🔧 Configuration Management

### Adding New Configs

1. Create or edit YAML files in `src/main/resources/configurations/`.
2. Follow naming: `{service-name}.yaml` for defaults, `{service-name}-{profile}.yaml` for profiles.
3. Restart Config Server or use `/actuator/refresh`.

### Best Practices

- **Version Control**: Keep configs in Git for history and collaboration.
- **Encryption**: Use Spring Cloud Config's encryption for sensitive data (e.g., passwords).
- **Validation**: Implement config validation in consuming services.
- **Monitoring**: Monitor config changes via logs and actuator endpoints.

## 🧪 Testing

Run tests with:
```bash
./mvnw test
```

Integration tests verify config serving and Eureka registration.

## 🤝 Contributing

We welcome contributions! Please:

1. Fork the repo.
2. Create a feature branch.
3. Add tests for new configs.
4. Submit a PR with detailed description.

Follow our [Contributing Guidelines](../CONTRIBUTING.md) for code standards.

## 📄 License

Licensed under the Apache License 2.0. See [LICENSE](../LICENSE) for details.

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/your-org/shopflow-backend/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-org/shopflow-backend/discussions)
- **Docs**: [ShopFlow Wiki](https://github.com/your-org/shopflow-backend/wiki)

---

**Built with ❤️ for scalable microservices. Keep configs centralized, deployments smooth!**
