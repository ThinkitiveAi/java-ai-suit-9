#!/bin/bash

# Healthcare Provider Registration API - Docker Management Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
}

# Function to build and start all services
start() {
    print_status "Starting Healthcare Provider Registration API with Docker..."
    
    check_docker
    
    print_status "Building and starting all services..."
    docker-compose up -d --build
    
    print_success "All services started successfully!"
    print_status "Waiting for services to be ready..."
    
    # Wait for services to be healthy
    sleep 30
    
    print_status "Checking service status..."
    docker-compose ps
    
    print_success "Healthcare Provider Registration API is ready!"
    echo ""
    print_status "Access URLs:"
    echo "  🌐 API Base: http://localhost:8080"
    echo "  📖 Swagger UI: http://localhost:8080/swagger-ui.html"
    echo "  📧 MailHog UI: http://localhost:8025"
    echo "  🔍 API Docs: http://localhost:8080/api-docs"
    echo ""
    print_status "To view logs: ./docker-run.sh logs"
    print_status "To stop services: ./docker-run.sh stop"
}

# Function to stop all services
stop() {
    print_status "Stopping all services..."
    docker-compose down
    print_success "All services stopped successfully!"
}

# Function to restart all services
restart() {
    print_status "Restarting all services..."
    docker-compose down
    docker-compose up -d --build
    print_success "All services restarted successfully!"
}

# Function to view logs
logs() {
    print_status "Showing logs for all services..."
    docker-compose logs -f
}

# Function to view logs for specific service
logs_service() {
    if [ -z "$1" ]; then
        print_error "Please specify a service name (postgres, redis, mailhog, healthcare-api)"
        exit 1
    fi
    print_status "Showing logs for service: $1"
    docker-compose logs -f "$1"
}

# Function to clean up everything
clean() {
    print_warning "This will remove all containers, volumes, and images. Are you sure? (y/N)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
        print_status "Cleaning up all Docker resources..."
        docker-compose down -v --rmi all
        docker system prune -f
        print_success "Cleanup completed!"
    else
        print_status "Cleanup cancelled."
    fi
}

# Function to check service health
health() {
    print_status "Checking service health..."
    docker-compose ps
    
    echo ""
    print_status "Testing API health endpoint..."
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        print_success "API is healthy!"
    else
        print_error "API health check failed!"
    fi
}

# Function to show help
help() {
    echo "Healthcare Provider Registration API - Docker Management Script"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  start     Build and start all services"
    echo "  stop      Stop all services"
    echo "  restart   Restart all services"
    echo "  logs      Show logs for all services"
    echo "  logs [service]  Show logs for specific service"
    echo "  health    Check service health and status"
    echo "  clean     Remove all containers, volumes, and images"
    echo "  help      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 start                    # Start all services"
    echo "  $0 logs healthcare-api      # Show API logs"
    echo "  $0 health                   # Check service health"
}

# Main script logic
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    logs)
        if [ -n "$2" ]; then
            logs_service "$2"
        else
            logs
        fi
        ;;
    health)
        health
        ;;
    clean)
        clean
        ;;
    help|--help|-h)
        help
        ;;
    *)
        print_error "Unknown command: $1"
        echo ""
        help
        exit 1
        ;;
esac 