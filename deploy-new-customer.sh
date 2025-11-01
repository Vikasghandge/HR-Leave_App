#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_DIR="/home/ubuntu/HR-Leave_App"
TEMPLATE_DIR="$BASE_DIR/Cloudblitz-leave-app"

# Logging function
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"
}

info() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] INFO:${NC} $1"
}

# Function to get next available ports
get_next_ports() {
    local last_nginx_port=8081
    local last_mysql_port=3307
    local last_app_port=8091

    # Find the highest currently used ports by checking running containers
    for dir in $BASE_DIR/*-leave-app; do
        if [ -d "$dir" ] && [ -f "$dir/docker-compose.yml" ]; then
            local nginx_port=$(grep -oP 'ports:\s*-\s*"\K[0-9]+(?=:80)' "$dir/docker-compose.yml" 2>/dev/null || echo "0")
            local mysql_port=$(grep -oP 'ports:\s*-\s*"\K[0-9]+(?=:3306)' "$dir/docker-compose.yml" 2>/dev/null || echo "0")
            local app_port=$(grep -oP 'SERVER_PORT:\s*\K[0-9]+' "$dir/docker-compose.yml" 2>/dev/null || echo "0")

            [ "$nginx_port" -gt "$last_nginx_port" ] && last_nginx_port=$nginx_port
            [ "$mysql_port" -gt "$last_mysql_port" ] && last_mysql_port=$mysql_port
            [ "$app_port" -gt "$last_app_port" ] && last_app_port=$app_port
        fi
    done

    # Check if ports are actually available, if not find next available
    while nc -z localhost $last_nginx_port 2>/dev/null; do
        warn "Port $last_nginx_port is already in use, trying next..."
        last_nginx_port=$((last_nginx_port + 1))
    done

    while nc -z localhost $last_mysql_port 2>/dev/null; do
        warn "Port $last_mysql_port is already in use, trying next..."
        last_mysql_port=$((last_mysql_port + 1))
    done

    # For app port, we don't need to check since it's not exposed to host
    last_app_port=$((last_app_port + 1))

    NEXT_NGINX_PORT=$last_nginx_port
    NEXT_MYSQL_PORT=$last_mysql_port
    NEXT_APP_PORT=$last_app_port

    info "Next available ports: Nginx=$NEXT_NGINX_PORT, MySQL=$NEXT_MYSQL_PORT, App=$NEXT_APP_PORT"
}

# Function to validate customer name
validate_customer_name() {
    local name="$1"
    # Remove any invalid characters and convert to lowercase
    CLEAN_NAME=$(echo "$name" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9-]//g' | sed 's/--*/-/g' | sed 's/^-//' | sed 's/-$//')

    if [ -z "$CLEAN_NAME" ]; then
        error "Invalid customer name. Use only letters, numbers, and hyphens."
        return 1
    fi

    # Check if customer directory already exists
    if [ -d "$BASE_DIR/${CLEAN_NAME}-leave-app" ]; then
        error "Customer directory already exists: $BASE_DIR/${CLEAN_NAME}-leave-app"
        return 1
    fi

    return 0
}

# Function to validate domain name
validate_domain_name() {
    local domain="$1"

    if [ -z "$domain" ]; then
        error "Domain name cannot be empty"
        return 1
    fi

    # Basic domain validation
    if [[ ! "$domain" =~ ^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$ ]]; then
        error "Invalid domain name format. Please provide a valid domain (e.g., example.com)"
        return 1
    fi

    return 0
}

# Function to copy and customize template
setup_customer_directory() {
    local customer_name="$1"
    local domain="$2"
    local customer_dir="$BASE_DIR/${customer_name}-leave-app"

    info "Creating customer directory: $customer_dir"

    # Copy template
    if ! cp -r "$TEMPLATE_DIR" "$customer_dir"; then
        error "Failed to copy template directory"
        return 1
    fi

    log "✓ Customer directory created"

    # Update docker-compose.yml
    info "Customizing docker-compose.yml..."

    cat > "$customer_dir/docker-compose.yml" << EOF
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: ${customer_name}-leave-mysql
    restart: always
    ports:
      - "${NEXT_MYSQL_PORT}:3306"
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: ${customer_name}_leavemanagement
      MYSQL_USER: ${customer_name}_user
      MYSQL_PASSWORD: password
    volumes:
      - ${customer_name}-mysql-data:/var/lib/mysql
    networks:
      - ${customer_name}-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 5s
      timeout: 10s
      retries: 10

  app:
    image: ghandgevikas/leave-management:\${DOCKER_TAG:-latest}
    container_name: ${customer_name}-leave-app
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${customer_name}_leavemanagement?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: password
      SERVER_PORT: ${NEXT_APP_PORT}
    networks:
      - ${customer_name}-network
    restart: unless-stopped

  nginx:
    build: ./nginx
    container_name: ${customer_name}-leave-nginx
    restart: unless-stopped
    ports:
      - "${NEXT_NGINX_PORT}:80"
    depends_on:
      - app
    networks:
      - ${customer_name}-network

networks:
  ${customer_name}-network:
    driver: bridge

volumes:
  ${customer_name}-mysql-data:
EOF

    log "✓ docker-compose.yml customized"

    # Update nginx configuration
    info "Customizing nginx configuration..."

    cat > "$customer_dir/nginx/nginx.conf" << EOF
events {
    worker_connections 1024;
    use epoll;
    multi_accept on;
}

http {
    # Basic Settings
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    client_max_body_size 100M;

    # MIME types
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    # Logging
    access_log /var/log/nginx/access.log;
    error_log /var/log/nginx/error.log;

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;

    # Upstream backend
    upstream app_backend {
        server app:${NEXT_APP_PORT} max_fails=3 fail_timeout=30s;
        keepalive 32;
    }

    # Main server block
    server {
        listen 80;
        server_name ${domain};

        # Security headers
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-XSS-Protection "1; mode=block" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;

        # Proxy settings
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;

        # Timeouts
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 60s;

        # Main application location
        location / {
            proxy_pass http://app_backend;

            # Essential headers
            proxy_set_header Host \$host:${NEXT_NGINX_PORT};
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto \$scheme;
            proxy_set_header X-Forwarded-Host \$host:${NEXT_NGINX_PORT};
            proxy_set_header X-Forwarded-Port ${NEXT_NGINX_PORT};
            proxy_set_header X-Forwarded-Server \$host;

            # Handle redirects properly
            proxy_redirect ~^(http://[^:]+)(?::80)?/(.*)$ http://\$1:${NEXT_NGINX_PORT}/\$2;
            proxy_redirect http://app:${NEXT_APP_PORT}/ http://\$host:${NEXT_NGINX_PORT}/;
            proxy_redirect http://localhost:${NEXT_APP_PORT}/ http://\$host:${NEXT_NGINX_PORT}/;
            proxy_redirect default;

            # Error handling
            proxy_intercept_errors on;
            error_page 500 502 503 504 /50x.html;
            error_page 404 /404.html;
        }

        # Health check endpoint
        location /health {
            access_log off;
            return 200 "${customer_name} Leave App - ${domain}:${NEXT_NGINX_PORT}\\n";
            add_header Content-Type text/plain;
            add_header X-Status "Healthy";
        }

        # Custom error pages
        location = /50x.html {
            return 500 "Internal Server Error\\n";
            add_header Content-Type text/plain;
        }

        location = /404.html {
            return 404 "Resource not found. Please check the URL.\\n";
            add_header Content-Type text/plain;
        }
    }
}
EOF

    log "✓ nginx.conf customized"
    return 0
}

# Function to deploy the application
deploy_application() {
    local customer_dir="$1"
    local customer_name="$2"
    local domain="$3"

    info "Deploying application for $customer_name..."

    cd "$customer_dir" || {
        error "Failed to enter customer directory: $customer_dir"
        return 1
    }

    # Build and start containers
    if docker-compose up --build -d; then
        log "✓ Docker containers started successfully"

        # Wait for services to be ready
        info "Waiting for services to be ready..."
        sleep 15

        # Test the deployment
        local health_check=$(curl -s "http://${domain}:${NEXT_NGINX_PORT}/health")
        if echo "$health_check" | grep -q "$customer_name"; then
            log "✓ Application deployed and responding correctly"
            return 0
        else
            warn "Application deployed but health check failed. Manual verification recommended."
            return 0
        fi
    else
        error "Failed to start Docker containers"
        return 1
    fi
}

# Function to display deployment summary
show_summary() {
    local customer_name="$1"
    local domain="$2"

    echo
    echo "=================================================="
    echo "🚀 DEPLOYMENT SUCCESSFUL!"
    echo "=================================================="
    echo "Customer Name: $customer_name"
    echo "Access URL: http://${domain}:${NEXT_NGINX_PORT}"
    echo "Health Check: http://${domain}:${NEXT_NGINX_PORT}/health"
    echo "MySQL Port: $NEXT_MYSQL_PORT"
    echo "App Port: $NEXT_APP_PORT"
    echo "Nginx Port: $NEXT_NGINX_PORT"
    echo "=================================================="
    echo
    echo "📋 Quick Test Commands:"
    echo "curl http://${domain}:${NEXT_NGINX_PORT}/health"
    echo "docker-compose -f $BASE_DIR/${customer_name}-leave-app/docker-compose.yml ps"
    echo
    echo "🌍 Share this URL with your customer:"
    echo "http://${domain}:${NEXT_NGINX_PORT}"
    echo "=================================================="
}

# Main execution
main() {
    echo
    echo "=================================================="
    echo "🚀 ADVANCED CUSTOMER DEPLOYMENT SCRIPT"
    echo "=================================================="
    echo

    # Check if template exists
    if [ ! -d "$TEMPLATE_DIR" ]; then
        error "Template directory not found: $TEMPLATE_DIR"
        exit 1
    fi

    # Get customer name
    read -p "Enter customer name (e.g., icic-bank): " CUSTOMER_NAME

    if [ -z "$CUSTOMER_NAME" ]; then
        error "Customer name cannot be empty"
        exit 1
    fi

    # Validate and clean customer name
    if ! validate_customer_name "$CUSTOMER_NAME"; then
        exit 1
    fi

    info "Using cleaned customer name: $CLEAN_NAME"

    # Get domain name
    echo
    echo "Please enter your domain name (e.g., company.com)"
    read -p "Enter domain name: " DOMAIN_NAME

    if [ -z "$DOMAIN_NAME" ]; then
        error "Domain name cannot be empty"
        exit 1
    fi

    # Validate domain name
    if ! validate_domain_name "$DOMAIN_NAME"; then
        exit 1
    fi

    info "Using domain: $DOMAIN_NAME"

    # Get next available ports
    get_next_ports

    # Setup customer directory
    if ! setup_customer_directory "$CLEAN_NAME" "$DOMAIN_NAME"; then
        error "Failed to setup customer directory"
        exit 1
    fi

    # Deploy application
    if deploy_application "$BASE_DIR/${CLEAN_NAME}-leave-app" "$CLEAN_NAME" "$DOMAIN_NAME"; then
        show_summary "$CLEAN_NAME" "$DOMAIN_NAME"
    else
        error "Deployment failed"
        exit 1
    fi
}

# Run main function
main "$@"
