# Healthcare Provider Registration Backend

This project is a comprehensive Spring Boot application designed for healthcare provider registration and management with enterprise-level security and scalability.

## 🏥 Overview

The Healthcare Provider Registration Backend is a secure, scalable system that handles healthcare provider onboarding with enterprise-level security, comprehensive validation, and email verification. It follows industry best practices for healthcare data management and security compliance.

## ✨ Features

- **🔐 Secure Provider Registration**: Complete registration process with comprehensive validation
- **📧 Email Verification**: Email-based account verification with secure token generation
- **🛡️ Rate Limiting**: IP-based rate limiting (5 attempts/hour) to prevent abuse
- **🔒 Advanced Security**: BCrypt password hashing with complexity requirements
- **✅ Input Validation**: Comprehensive input sanitization and field validation
- **📱 Phone Validation**: International phone number format validation using libphonenumber
- **🗄️ Database Management**: PostgreSQL with JPA/Hibernate and optimized indexes
- **📚 API Documentation**: Complete Swagger/OpenAPI documentation
- **🧪 Testing**: Comprehensive unit and integration tests with high coverage
- **📊 Monitoring**: Built-in health checks and audit logging

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.5.3, Spring Security, Spring Data JPA
- **Database**: PostgreSQL 15 with optimized indexes
- **Cache/Rate Limiting**: Redis 7 with Lettuce client
- **Security**: BCrypt password hashing, JWT tokens
- **Email**: Spring Mail with HTML templates
- **Validation**: Bean Validation (JSR-303), libphonenumber
- **Documentation**: SpringDoc OpenAPI 3
- **Testing**: JUnit 5, Mockito, MockMvc
- **Build Tool**: Maven 3.8+
- **Java Version**: 17 LTS

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- Git

### 1. Clone and Setup

```bash
git clone <repository-url>
cd java-ai-suit-9
```

### 2. Start Infrastructure Services

```bash
# Start PostgreSQL, Redis, and MailHog
docker-compose up -d
```

This starts:
- PostgreSQL on port 5433
- Redis on port 6379  
- MailHog SMTP on port 1025 (UI on 8025)

### 3. Configure Environment

Create `.env` file or set environment variables:

```env
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5433/healthcare_management
DATABASE_USERNAME=healthcare_user
DATABASE_PASSWORD=healthcare_password

# Email Configuration
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_USER=test@example.com
SMTP_PASS=test

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# Security Configuration
BCRYPT_SALT_ROUNDS=12
JWT_SECRET=your-secure-jwt-secret-key

# Rate Limiting Configuration
RATE_LIMIT_WINDOW_MS=3600000
RATE_LIMIT_MAX_REQUESTS=5

# Application URLs
FRONTEND_URL=http://localhost:3000
BACKEND_URL=http://localhost:8080
```

### 4. Run the Application

```bash
# Option 1: Using Maven
mvn spring-boot:run

# Option 2: Using compiled JAR
mvn clean package -DskipTests
java -jar target/session-demo-0.0.1-SNAPSHOT.jar
```

### 5. Access the Application

- **🌐 API Base**: http://localhost:8080
- **📖 Swagger UI**: http://localhost:8080/swagger-ui.html
- **📧 MailHog UI**: http://localhost:8025
- **🔍 API Docs**: http://localhost:8080/api-docs

## 📋 API Documentation

### Provider Registration Endpoint

#### 🔄 Register New Provider
```http
POST /api/v1/provider/register
Content-Type: application/json

{
  "first_name": "John",
  "last_name": "Doe",
  "email": "john.doe@clinic.com",
  "phone_number": "+1234567890",
  "password": "SecurePassword123!",
  "confirm_password": "SecurePassword123!",
  "specialization": "CARDIOLOGY",
  "license_number": "MD123456789",
  "years_of_experience": 10,
  "clinic_address": {
    "street": "123 Medical Center Dr",
    "city": "New York",
    "state": "NY",
    "zip": "10001"
  }
}
```

**✅ Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Provider registered successfully. Verification email sent.",
  "data": {
    "provider_id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "john.doe@clinic.com",
    "verification_status": "PENDING",
    "created_at": "2024-01-24T10:30:00Z"
  }
}
```

#### 📧 Verify Email Address
```http
GET /api/v1/provider/verify-email?token={verification-token}
```

#### 📊 Check Rate Limit Status
```http
GET /api/v1/provider/rate-limit-status
```

## 🔒 Security Implementation

### Password Security Requirements
- **Minimum Length**: 8 characters
- **Complexity**: Must contain uppercase, lowercase, number, and special character
- **Hashing**: BCrypt with 12 salt rounds
- **Storage**: Never store or log plain text passwords

### Rate Limiting Strategy
- **Limit**: 5 registration attempts per IP address per hour
- **Implementation**: Redis-based sliding window
- **Response**: HTTP 429 with retry information

### Input Validation & Sanitization
- **Email**: RFC-compliant format validation, uniqueness check
- **Phone**: libphonenumber validation, E.164 normalization
- **License**: Alphanumeric format, uniqueness validation
- **Text Fields**: XSS prevention, SQL injection protection

## 🧪 Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run with coverage report
mvn clean test jacoco:report

# Run integration tests only
mvn test -Dtest=*IntegrationTest
```

## 🚀 Deployment

### Docker Deployment

```bash
# Build and run
mvn clean package -DskipTests
docker-compose up -d
```

### Environment Variables for Production

```env
DATABASE_URL=jdbc:postgresql://prod-db:5432/healthcare_management
REDIS_HOST=prod-redis
SMTP_HOST=smtp.sendgrid.net
BCRYPT_SALT_ROUNDS=14
RATE_LIMIT_MAX_REQUESTS=3
```

## 📊 API Endpoints Summary

| Endpoint | Method | Description | Rate Limited |
|----------|--------|-------------|--------------|
| `/api/v1/provider/register` | POST | Register new provider | ✅ Yes |
| `/api/v1/provider/verify-email` | GET | Verify email address | ❌ No |
| `/api/v1/provider/rate-limit-status` | GET | Check rate limit status | ❌ No |
| `/swagger-ui.html` | GET | API Documentation | ❌ No |

## 🔧 Troubleshooting

### Common Issues

```bash
# Check services status
docker-compose ps

# View logs
docker-compose logs [service-name]

# Reset rate limiting
redis-cli FLUSHALL

# View emails in development
open http://localhost:8025
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit pull request

## 📄 License

This project is licensed under the MIT License.

---

**🏥 Healthcare Provider Registration Backend** - Built with Spring Boot, secured with enterprise-grade practices, tested comprehensively.