# Enhanced Healthcare Provider Authentication System

## Overview

This document describes the enterprise-grade authentication system implemented for healthcare providers, featuring comprehensive security measures, audit logging, and session management.

## 🚀 Features Implemented

### ✅ Core Authentication Features
- **JWT-based Authentication** with access and refresh tokens
- **Multi-factor Login** (email or phone number)
- **Remember Me** functionality with extended token expiration
- **Token Rotation** for enhanced security
- **Session Management** with concurrent session limits

### ✅ Security Features
- **Brute Force Protection** with progressive account lockouts
- **Rate Limiting** by IP address and identifier
- **Account Lockout** after failed attempts
- **Password Security** with bcrypt hashing
- **Device Fingerprinting** for session tracking
- **IP Address Tracking** for security monitoring

### ✅ Audit & Compliance
- **Comprehensive Audit Logging** of all authentication events
- **Login Attempt Tracking** with failure reasons
- **Session History** for compliance requirements
- **Security Event Monitoring** for suspicious activity

### ✅ Session Management
- **Concurrent Session Limits** (configurable)
- **Session Cleanup** with automated maintenance
- **Device-based Session Tracking**
- **Manual Session Revocation**

## 📋 API Endpoints

### Authentication Endpoints

#### 1. Enhanced Login
```http
POST /api/v1/provider/login
```

**Request Body:**
```json
{
  "identifier": "john.doe@clinic.com",
  "password": "SecurePassword123!",
  "remember_me": false,
  "device_info": "Chrome on Windows 10"
}
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 3600,
    "refresh_expires_in": 604800,
    "token_type": "Bearer",
    "provider": {
      "id": 1,
      "uuid": "uuid-provider-id",
      "first_name": "John",
      "last_name": "Doe",
      "email": "john.doe@clinic.com",
      "phone_number": "+1234567890",
      "specialization": "CARDIOLOGY",
      "verification_status": "VERIFIED",
      "is_active": true,
      "last_login": "2025-01-24T10:30:00Z",
      "login_count": 15
    }
  }
}
```

#### 2. Token Refresh
```http
POST /api/v1/provider/refresh
```

**Request Body:**
```json
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### 3. Logout (Single Session)
```http
POST /api/v1/provider/logout?refresh_token=token_here
```

#### 4. Logout All Sessions
```http
POST /api/v1/provider/logout-all?providerUuid=uuid_here
```

## 🔧 Configuration

### Environment Variables

```bash
# JWT Configuration
JWT_SECRET=your-super-secure-jwt-secret-key
JWT_ACCESS_EXPIRES_IN=3600000
JWT_REFRESH_EXPIRES_IN=604800000
JWT_REFRESH_REMEMBER_ME_EXPIRES_IN=2592000000

# Security Settings
MAX_LOGIN_ATTEMPTS=5
ACCOUNT_LOCKOUT_DURATION=1800000
RATE_LIMIT_WINDOW=900000
RATE_LIMIT_MAX_ATTEMPTS=5
MAX_CONCURRENT_SESSIONS=5

# Session Cleanup
LOGIN_ATTEMPTS_RETENTION_DAYS=90
REFRESH_TOKENS_RETENTION_DAYS=30
```

### Application Properties

```properties
# Enhanced JWT Configuration
jwt.access.expiration=${JWT_ACCESS_EXPIRES_IN:3600000}
jwt.refresh.expiration=${JWT_REFRESH_EXPIRES_IN:604800000}
jwt.refresh.remember-me.expiration=${JWT_REFRESH_REMEMBER_ME_EXPIRES_IN:2592000000}

# Security Settings
security.max-login-attempts=${MAX_LOGIN_ATTEMPTS:5}
security.account-lockout-duration=${ACCOUNT_LOCKOUT_DURATION:1800000}
security.rate-limit-window=${RATE_LIMIT_WINDOW:900000}
security.rate-limit-max-attempts=${RATE_LIMIT_MAX_ATTEMPTS:5}
security.max-concurrent-sessions=${MAX_CONCURRENT_SESSIONS:5}

# Session Cleanup Configuration
session.cleanup.login-attempts-retention-days=${LOGIN_ATTEMPTS_RETENTION_DAYS:90}
session.cleanup.refresh-tokens-retention-days=${REFRESH_TOKENS_RETENTION_DAYS:30}
```

## 🗄️ Database Schema

### Enhanced Provider Entity
```sql
-- Additional fields added to Provider table
ALTER TABLE providers ADD COLUMN last_login TIMESTAMP;
ALTER TABLE providers ADD COLUMN failed_login_attempts INTEGER DEFAULT 0;
ALTER TABLE providers ADD COLUMN locked_until TIMESTAMP;
ALTER TABLE providers ADD COLUMN login_count INTEGER DEFAULT 0;
ALTER TABLE providers ADD COLUMN password_changed_at TIMESTAMP;
ALTER TABLE providers ADD COLUMN concurrent_sessions INTEGER DEFAULT 0;
```

### RefreshToken Entity
```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    provider_id BIGINT REFERENCES providers(id),
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_token_provider ON refresh_tokens(provider_id);
CREATE INDEX idx_refresh_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_token_expires ON refresh_tokens(expires_at);
```

### LoginAttempt Entity (Audit Trail)
```sql
CREATE TABLE login_attempts (
    id UUID PRIMARY KEY,
    provider_id BIGINT REFERENCES providers(id),
    identifier VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent TEXT,
    attempt_type VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_login_attempt_provider ON login_attempts(provider_id);
CREATE INDEX idx_login_attempt_identifier ON login_attempts(identifier);
CREATE INDEX idx_login_attempt_ip ON login_attempts(ip_address);
CREATE INDEX idx_login_attempt_created ON login_attempts(created_at);
CREATE INDEX idx_login_attempt_type ON login_attempts(attempt_type);
```

## 🔒 Security Features

### Brute Force Protection
- **Progressive Lockouts**: 5 minutes → 15 minutes → 30 minutes → 1 hour → 24 hours
- **Failed Attempt Tracking**: Per identifier and IP address
- **Account Lockout**: Automatic after 5 consecutive failed attempts
- **Rate Limiting**: Maximum 5 attempts per 15 minutes per identifier

### Rate Limiting Strategy
```java
// Lockout escalation strategy
const lockoutDurations = {
  1: 5 * 60 * 1000,    // 5 minutes
  2: 15 * 60 * 1000,   // 15 minutes
  3: 30 * 60 * 1000,   // 30 minutes
  4: 60 * 60 * 1000,   // 1 hour
  5: 24 * 60 * 60 * 1000 // 24 hours
};
```

### JWT Token Security
- **Access Token**: 1 hour expiration (24 hours with remember_me)
- **Refresh Token**: 7 days expiration (30 days with remember_me)
- **Token Rotation**: New refresh token on each use
- **Token Revocation**: Support for manual and automatic revocation

## 📊 Error Handling

### Standardized Error Responses

#### Invalid Credentials (401)
```json
{
  "success": false,
  "message": "Invalid email/phone or password",
  "error_code": "AUTHENTICATION_FAILED"
}
```

#### Account Locked (423)
```json
{
  "success": false,
  "message": "Account temporarily locked due to multiple failed attempts",
  "error_code": "ACCOUNT_LOCKED"
}
```

#### Rate Limited (429)
```json
{
  "success": false,
  "message": "Too many login attempts. Please try again later",
  "error_code": "RATE_LIMITED"
}
```

#### Email Not Verified (403)
```json
{
  "success": false,
  "message": "Please verify your email before logging in",
  "error_code": "EMAIL_NOT_VERIFIED"
}
```

## 🧪 Testing

### Unit Tests
- `EnhancedAuthServiceTest`: Comprehensive service layer testing
- JWT token generation and validation
- Password verification logic
- Rate limiting algorithms
- Account lockout mechanisms

### Integration Tests
- `EnhancedAuthControllerIntegrationTest`: End-to-end API testing
- Complete login flow testing
- Token refresh workflow
- Error response validation
- Security feature testing

### Test Coverage
- Authentication success and failure scenarios
- Rate limiting and brute force protection
- Session management and cleanup
- Audit logging verification

## 🔄 Session Management

### Automated Cleanup
- **Hourly**: Cleanup expired refresh tokens
- **Daily (2 AM)**: Cleanup old login attempts (90 days retention)
- **Daily (3 AM)**: Cleanup old refresh tokens (30 days retention)

### Manual Cleanup
```java
@Autowired
private SessionCleanupService sessionCleanupService;

// Perform immediate cleanup
sessionCleanupService.performManualCleanup();
```

## 📈 Monitoring & Alerting

### Security Monitoring
- Failed login attempt spikes
- Account lockout frequency
- Suspicious IP activity
- Token refresh patterns
- Concurrent session anomalies

### Performance Monitoring
- Login endpoint response times
- Database query performance
- JWT token generation speed
- Rate limiting overhead
- Session cleanup efficiency

### Audit Requirements
- All authentication events logged
- Account lockouts and unlocks tracked
- Token generation and revocation recorded
- Password change events monitored
- Administrative actions audited

## 🚀 Deployment

### Prerequisites
- Java 17+
- Spring Boot 3.x
- PostgreSQL 12+
- Redis (for rate limiting)

### Build & Run
```bash
# Build the application
./mvnw clean package

# Run with Docker
docker-compose up -d

# Run locally
./mvnw spring-boot:run
```

### Database Migration
```bash
# Run database migrations
./mvnw flyway:migrate

# Or use the provided SQL scripts
psql -d your_database -f schema.sql
```

## 🔐 Security Best Practices

### Production Deployment
1. **Change Default Secrets**: Update all JWT secrets and passwords
2. **Enable HTTPS**: Use SSL/TLS for all communications
3. **Configure Firewall**: Restrict access to necessary ports only
4. **Monitor Logs**: Set up centralized logging and alerting
5. **Regular Updates**: Keep dependencies updated
6. **Backup Strategy**: Implement regular database backups

### Compliance Considerations
- **HIPAA Compliance**: All audit logs maintained for required retention
- **Data Encryption**: Sensitive data encrypted at rest and in transit
- **Access Controls**: Role-based access control implemented
- **Audit Trails**: Comprehensive logging for compliance requirements

## 📚 Additional Resources

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [JWT.io](https://jwt.io/) - JWT token debugging and validation
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [Healthcare Security Standards](https://www.hhs.gov/hipaa/index.html)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Implement your changes
4. Add comprehensive tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details. 