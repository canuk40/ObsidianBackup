#!/bin/bash

# ObsidianBackup Enterprise - Installation Script
# This script sets up the enterprise environment

set -e

echo "==================================="
echo "ObsidianBackup Enterprise Installer"
echo "==================================="
echo ""

# Check prerequisites
echo "Checking prerequisites..."

if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose not found. Please install Docker Compose first."
    exit 1
fi

echo "✅ Docker found: $(docker --version)"
echo "✅ Docker Compose found: $(docker-compose --version)"
echo ""

# Generate JWT secret
echo "Generating JWT secret..."
JWT_SECRET=$(openssl rand -base64 64)
echo "✅ JWT secret generated"
echo ""

# Create .env file
echo "Creating environment configuration..."
cat > .env << EOF
# ObsidianBackup Enterprise Configuration
# Generated on $(date)

# Database Configuration
DATABASE_URL=jdbc:postgresql://postgres:5432/obsidian_enterprise
DATABASE_USER=postgres
DATABASE_PASSWORD=$(openssl rand -base64 32)

# JWT Configuration
JWT_SECRET=${JWT_SECRET}

# API Configuration
VITE_API_URL=http://localhost:8080
EOF
echo "✅ Environment configuration created"
echo ""

# Create docker-compose override for production
echo "Creating production configuration..."
cat > docker-compose.override.yml << EOF
version: '3.8'

services:
  backend:
    environment:
      - JWT_SECRET=\${JWT_SECRET}
      - DATABASE_PASSWORD=\${DATABASE_PASSWORD}
    restart: always

  admin-console:
    restart: always

  postgres:
    environment:
      - POSTGRES_PASSWORD=\${DATABASE_PASSWORD}
    restart: always
    volumes:
      - ./backups:/backups
EOF
echo "✅ Production configuration created"
echo ""

# Start services
echo "Starting services..."
docker-compose up -d

echo ""
echo "Waiting for services to be ready..."
sleep 10

# Check service health
echo "Checking service health..."
if curl -s http://localhost:8080/health > /dev/null; then
    echo "✅ Backend API is healthy"
else
    echo "⚠️  Backend API may need more time to start"
fi

if curl -s http://localhost:3000 > /dev/null; then
    echo "✅ Admin Console is accessible"
else
    echo "⚠️  Admin Console may need more time to start"
fi

echo ""
echo "==================================="
echo "Installation Complete!"
echo "==================================="
echo ""
echo "Access Points:"
echo "  Admin Console: http://localhost:3000"
echo "  Backend API:   http://localhost:8080"
echo ""
echo "Default Credentials:"
echo "  Email:    admin@example.com"
echo "  Password: admin123"
echo ""
echo "⚠️  IMPORTANT: Change default password immediately!"
echo ""
echo "Configuration saved to .env file"
echo "Keep this file secure and never commit it to version control."
echo ""
echo "Next steps:"
echo "1. Open http://localhost:3000 in your browser"
echo "2. Login with default credentials"
echo "3. Change admin password in Settings"
echo "4. Configure your organization settings"
echo "5. Set up SAML/SSO if needed"
echo ""
echo "For more information, see:"
echo "  - ENTERPRISE_EDITION.md"
echo "  - QUICKSTART.md"
echo ""
echo "To view logs: docker-compose logs -f"
echo "To stop services: docker-compose down"
echo ""
