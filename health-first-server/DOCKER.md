# 🐳 Docker Setup Guide

This guide will help you run the Healthcare Provider Registration API using Docker.

## 📋 Prerequisites

- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **Git**: To clone the repository

## 🚀 Quick Start

### Option 1: Using the Docker Management Script (Recommended)

```bash
# Make the script executable (if not already done)
chmod +x docker-run.sh

# Start all services
./docker-run.sh start

# Check service health
./docker-run.sh health

# View logs
./docker-run.sh logs

# Stop all services
./docker-run.sh stop
```

### Option 2: Using Docker Compose Directly

```bash
# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## 🏗️ Architecture

The Docker setup includes the following services:

| Service | Port | Description |
|---------|------|-------------|
| **healthcare-api** | 8080 | Spring Boot Application |
| **postgres** | 5433 | PostgreSQL Database |
| **redis** | 6379 | Redis Cache/Rate Limiting |
| **mailhog** | 1025/8025 | Email Testing Service |

## 📁 File Structure

```
├── Dockerfile                 # Multi-stage Docker build
├── docker-compose.yml         # Production services
├── docker-compose.dev.yml     # Development overrides
├── .dockerignore             # Docker build exclusions
└── docker-run.sh             # Management script
```

## 🔧 Configuration

### Environment Variables

The application uses the following environment variables:

```env
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/healthcare_management
SPRING_DATASOURCE_USERNAME=healthcare_user
SPRING_DATASOURCE_PASSWORD=healthcare_password

# Redis Configuration
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379

# Email Configuration
SPRING_MAIL_HOST=mailhog
SPRING_MAIL_PORT=1025
SPRING_MAIL_USERNAME=test@example.com
SPRING_MAIL_PASSWORD=test

# Application URLs
APP_FRONTEND_URL=http://localhost:3000
APP_BACKEND_URL=http://localhost:8080
```

### Development Mode

To run in development mode with debug capabilities:

```bash
# Start with development overrides
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Or use the script
./docker-run.sh start dev
```

## 🛠️ Docker Management Script

The `docker-run.sh` script provides easy management of Docker services:

### Available Commands

```bash
./docker-run.sh start          # Build and start all services
./docker-run.sh stop           # Stop all services
./docker-run.sh restart        # Restart all services
./docker-run.sh logs           # Show all service logs
./docker-run.sh logs [service] # Show logs for specific service
./docker-run.sh health         # Check service health
./docker-run.sh clean          # Remove all containers and volumes
./docker-run.sh help           # Show help message
```

### Service-Specific Logs

```bash
./docker-run.sh logs healthcare-api  # Application logs
./docker-run.sh logs postgres        # Database logs
./docker-run.sh logs redis           # Redis logs
./docker-run.sh logs mailhog         # Email service logs
```

## 🌐 Access Points

Once the services are running, you can access:

- **🌐 API Base**: http://localhost:8080
- **📖 Swagger UI**: http://localhost:8080/swagger-ui.html
- **📧 MailHog UI**: http://localhost:8025
- **🔍 API Docs**: http://localhost:8080/api-docs

## 🧪 Testing the API

### 1. Health Check

```bash
curl http://localhost:8080/actuator/health
```

### 2. Register a Provider

```bash
curl -X POST http://localhost:8080/api/v1/provider/register \
  -H "Content-Type: application/json" \
  -d '{
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
  }'
```

### 3. Check Rate Limit Status

```bash
curl http://localhost:8080/api/v1/provider/rate-limit-status
```

## 🔍 Troubleshooting

### Common Issues

#### 1. Port Already in Use

```bash
# Check what's using the port
sudo lsof -i :8080

# Kill the process or change the port in docker-compose.yml
```

#### 2. Database Connection Issues

```bash
# Check PostgreSQL logs
./docker-run.sh logs postgres

# Test database connection
docker exec -it healthcare-postgres psql -U healthcare_user -d healthcare_management
```

#### 3. Redis Connection Issues

```bash
# Check Redis logs
./docker-run.sh logs redis

# Test Redis connection
docker exec -it healthcare-redis redis-cli ping
```

#### 4. Application Won't Start

```bash
# Check application logs
./docker-run.sh logs healthcare-api

# Check if all dependencies are healthy
./docker-run.sh health
```

### Debug Mode

To run the application in debug mode:

```bash
# Start with debug port exposed
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Connect your IDE to localhost:5005 for debugging
```

### Reset Everything

```bash
# Complete cleanup
./docker-run.sh clean

# Start fresh
./docker-run.sh start
```

## 📊 Monitoring

### Service Status

```bash
# Check all service status
docker-compose ps

# Check service health
./docker-run.sh health
```

### Resource Usage

```bash
# Monitor resource usage
docker stats

# Check disk usage
docker system df
```

### Logs Analysis

```bash
# Follow all logs
docker-compose logs -f

# Follow specific service
docker-compose logs -f healthcare-api

# Search logs
docker-compose logs healthcare-api | grep "ERROR"
```

## 🔒 Security Considerations

### Production Deployment

For production deployment, consider:

1. **Environment Variables**: Use `.env` files or secrets management
2. **Network Security**: Use Docker networks and firewalls
3. **Image Security**: Regularly update base images
4. **Resource Limits**: Set memory and CPU limits
5. **Health Checks**: Monitor service health

### Example Production Environment

```bash
# Create production environment file
cat > .env.prod << EOF
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/healthcare_management
SPRING_DATA_REDIS_HOST=prod-redis
SPRING_MAIL_HOST=smtp.sendgrid.net
BCRYPT_SALT_ROUNDS=14
RATE_LIMIT_MAX_REQUESTS=3
EOF

# Run with production config
docker-compose --env-file .env.prod up -d
```

## 📝 Development Workflow

### 1. Start Development Environment

```bash
./docker-run.sh start
```

### 2. Make Code Changes

Edit your code in your IDE.

### 3. Rebuild and Restart

```bash
# Rebuild the application
docker-compose build healthcare-api

# Restart the service
docker-compose restart healthcare-api
```

### 4. View Changes

```bash
# Check logs for any errors
./docker-run.sh logs healthcare-api

# Test the API
curl http://localhost:8080/actuator/health
```

## 🎯 Best Practices

1. **Always use the management script** for consistency
2. **Check service health** before testing
3. **Monitor logs** during development
4. **Use development overrides** for debugging
5. **Clean up regularly** to free disk space
6. **Backup data** before major changes

## 📞 Support

If you encounter issues:

1. Check the troubleshooting section above
2. Review service logs: `./docker-run.sh logs`
3. Verify service health: `./docker-run.sh health`
4. Check Docker status: `docker system info`

---

**🐳 Happy Docker Development!** 