# ObsidianBackup Enterprise Edition

**Version:** 1.0.0  
**Target:** Business Deployments  
**Date:** February 2026

## Overview

ObsidianBackup Enterprise Edition is a comprehensive backup management platform designed for business deployments. It provides enterprise-grade features including centralized management, SSO authentication, audit logging, policy enforcement, and compliance tools for GDPR and HIPAA.

## Architecture

### Components

1. **Backend API Server** (Kotlin + Ktor)
   - RESTful API for all enterprise operations
   - JWT authentication with SAML/SSO support
   - PostgreSQL database for persistent storage
   - Real-time audit logging

2. **Admin Console** (React + Material-UI)
   - Web-based management interface
   - Real-time dashboards and analytics
   - Device fleet management
   - Policy configuration UI

3. **Database** (PostgreSQL)
   - Multi-tenant architecture
   - Audit log storage
   - User and device management
   - Policy enforcement records

## Core Features

### 1. Admin Console Web UI

A modern, responsive web interface built with React and Material-UI providing:

- **Dashboard**: Real-time statistics and visualizations
  - Total devices, active devices, backup success rates
  - Storage usage charts
  - Compliance metrics
  - Backup success rate trends (last 30 days)

- **Device Management**: Fleet-wide device control
  - Device registration and listing
  - Device status monitoring
  - Remote wipe capability
  - Compliance status per device

- **Policy Management**: Centralized policy configuration
  - Backup schedules
  - Retention policies
  - Encryption requirements
  - Network policies

- **Audit Logs**: Comprehensive activity tracking
  - Searchable audit trail
  - CSV export functionality
  - Date range filtering
  - User and device filtering

- **Reports**: Business intelligence and analytics
  - Storage usage breakdown
  - Compliance reports
  - GDPR/HIPAA compliance summary
  - PDF export capability

- **RBAC**: Role-based access control
  - Custom role creation
  - Permission assignment
  - User role management

### 2. SSO/SAML Integration

**Authentication Methods:**
- Standard email/password authentication
- SAML 2.0 for enterprise SSO
- JWT token-based authorization

**SAML Configuration:**
```kotlin
// Service Provider (SP) Configuration
- Entity ID: obsidian-enterprise
- ACS URL: https://enterprise.obsidianbackup.com/saml/acs
- SLO URL: https://enterprise.obsidianbackup.com/saml/slo

// Identity Provider (IdP) Integration
- Supports major IdPs (Okta, Azure AD, OneLogin, etc.)
- Certificate-based signing
- Attribute mapping for user provisioning
```

**API Endpoints:**
- `POST /api/v1/auth/login` - Standard login
- `POST /api/v1/auth/saml/initiate` - Initiate SAML authentication
- `POST /api/v1/auth/saml/callback` - SAML response handler
- `POST /api/v1/auth/logout` - Logout

### 3. Audit Log System

**Tracked Operations:**
- User authentication (login/logout)
- Device registration/deregistration
- Backup operations (start, complete, fail)
- Restore operations
- Policy changes
- Device wipes
- Configuration changes
- Access control modifications

**Log Entry Structure:**
```json
{
  "id": "uuid",
  "timestamp": "2026-02-08T12:34:56Z",
  "userId": "user-uuid",
  "deviceId": "device-uuid",
  "action": "DEVICE_WIPED",
  "resourceType": "Device",
  "resourceId": "device-uuid",
  "status": "SUCCESS",
  "details": {
    "reason": "Security policy violation"
  },
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0..."
}
```

**Features:**
- Real-time logging
- Tamper-proof storage
- Advanced filtering and search
- CSV export for compliance
- Configurable retention periods
- Automated archival

**API Endpoints:**
- `GET /api/v1/audit` - Query audit logs
- `GET /api/v1/audit/{logId}` - Get specific log entry
- `GET /api/v1/audit/export` - Export logs as CSV

### 4. Multi-Device Fleet Management API

**Device Management:**
```kotlin
// Device Registration
POST /api/v1/devices/register
{
  "name": "John's Pixel 8",
  "platform": "Android",
  "osVersion": "14.0",
  "appVersion": "1.0.0"
}

// Get All Devices
GET /api/v1/devices

// Get Device Details
GET /api/v1/devices/{deviceId}

// Check Device Compliance
GET /api/v1/devices/{deviceId}/compliance
```

**Device Model:**
```kotlin
data class Device(
    val id: String,
    val name: String,
    val userId: String,
    val organizationId: String,
    val platform: String,
    val osVersion: String,
    val appVersion: String,
    val lastSyncAt: String?,
    val status: DeviceStatus, // ACTIVE, INACTIVE, WIPED, SUSPENDED
    val policies: List<String>,
    val complianceStatus: ComplianceStatus,
    val registeredAt: String
)
```

**Bulk Operations:**
- Mass policy deployment
- Bulk status updates
- Fleet-wide configuration changes

### 5. Policy Enforcement

**Policy Types:**

1. **Backup Schedule Policies**
   ```json
   {
     "type": "BACKUP_SCHEDULE",
     "config": {
       "frequency": "daily",
       "time": "02:00",
       "mandatory": "true",
       "networkType": "wifi"
     }
   }
   ```

2. **Retention Policies**
   ```json
   {
     "type": "RETENTION",
     "config": {
       "keepDays": "90",
       "maxBackups": "30",
       "autoDelete": "true"
     }
   }
   ```

3. **Encryption Policies**
   ```json
   {
     "type": "ENCRYPTION",
     "config": {
       "algorithm": "AES-256",
       "mandatory": "true",
       "keyRotationDays": "90"
     }
   }
   ```

4. **Network Policies**
   ```json
   {
     "type": "NETWORK",
     "config": {
       "allowCellular": "false",
       "requireVPN": "true",
       "bandwidthLimit": "10485760"
     }
   }
   ```

5. **Compliance Policies**
   ```json
   {
     "type": "COMPLIANCE",
     "config": {
       "requireBiometric": "true",
       "dataResidency": "EU",
       "auditLevel": "verbose"
     }
   }
   ```

**Policy API:**
- `GET /api/v1/policies` - List all policies
- `POST /api/v1/policies` - Create policy
- `PUT /api/v1/policies/{id}` - Update policy
- `DELETE /api/v1/policies/{id}` - Delete policy
- `POST /api/v1/policies/{id}/enforce` - Force policy enforcement

### 6. Remote Wipe Capability

**Use Cases:**
- Lost or stolen devices
- Employee termination
- Security policy violations
- Data breach response

**Remote Wipe Process:**
1. Admin initiates wipe from console
2. Command queued for device
3. Device receives wipe command on next sync
4. All backup data removed from device
5. Cloud backup data retained per policy
6. Audit log entry created

**API:**
```kotlin
POST /api/v1/devices/{deviceId}/wipe
{
  "deviceId": "device-uuid",
  "reason": "Device lost - reported by employee"
}
```

**Security Features:**
- Requires admin authentication
- Logged in audit trail
- Confirmation required
- Cannot be undone
- Multi-factor authentication option

### 7. GDPR/HIPAA Compliance Features

#### GDPR Compliance

**Right to Access:**
- User data export API
- Complete backup history
- Audit log access

**Right to Erasure:**
- Remote wipe functionality
- Complete data deletion
- Verification and confirmation

**Data Portability:**
- Export data in standard formats (JSON, CSV)
- Machine-readable format
- Includes all user data and metadata

**Consent Management:**
- Explicit consent tracking
- Consent withdrawal support
- Purpose-specific consent

**Data Protection:**
- AES-256 encryption at rest
- TLS 1.3 for data in transit
- Encryption key management
- Data minimization principles

#### HIPAA Compliance

**Access Controls:**
- Role-based access control (RBAC)
- Minimum necessary access
- Unique user identification
- Emergency access procedures

**Audit Controls:**
- Comprehensive audit logging
- Hardware and software activity logging
- Tamper-proof audit trail
- Automated log review

**Integrity Controls:**
- Data integrity verification
- Merkle tree checksums
- Tamper detection
- Version control

**Transmission Security:**
- End-to-end encryption
- TLS 1.3 minimum
- VPN support
- Secure authentication

**Business Associate Agreements:**
- BAA template included
- Terms of service compliance
- Breach notification procedures

### 8. Bulk Deployment Configuration

**Configuration Management:**
```yaml
# enterprise-config.yaml
organization:
  name: "Acme Corporation"
  domain: "acme.com"
  maxDevices: 1000

policies:
  - name: "Corporate Backup Policy"
    type: "BACKUP_SCHEDULE"
    mandatory: true
    targets: "all"
    config:
      frequency: "daily"
      time: "02:00"
      
  - name: "Data Retention"
    type: "RETENTION"
    mandatory: true
    targets: "all"
    config:
      keepDays: 90

saml:
  enabled: true
  entityId: "obsidian-enterprise"
  ssoUrl: "https://sso.acme.com/saml"
  
compliance:
  gdpr: true
  hipaa: true
  dataResidency: "EU"
```

**Deployment Methods:**
1. **REST API** - Programmatic deployment
2. **CLI Tool** - Command-line management
3. **Configuration Files** - YAML/JSON configs
4. **Admin Console** - Web-based deployment

**Bulk Operations:**
- `POST /api/v1/bulk/deploy-policies` - Deploy policies to multiple devices
- `POST /api/v1/bulk/update-config` - Update configuration across fleet
- `POST /api/v1/bulk/register-devices` - Register multiple devices

### 9. Role-Based Access Control (RBAC)

**Predefined Roles:**

1. **Super Admin**
   - Full system access
   - Organization management
   - User management
   - All permissions

2. **Admin**
   - Device management
   - Policy management
   - Audit log access
   - Report generation

3. **Operator**
   - Device monitoring
   - Backup status viewing
   - Basic reporting

4. **Auditor**
   - Read-only access
   - Audit log viewing
   - Report generation
   - Compliance reports

**Permissions:**
```
- devices:read
- devices:write
- devices:delete
- devices:wipe
- policies:read
- policies:write
- policies:delete
- policies:enforce
- audit:read
- audit:export
- reports:read
- reports:export
- users:read
- users:write
- users:delete
- roles:read
- roles:write
- roles:delete
- organization:read
- organization:write
```

**Custom Roles:**
```json
{
  "name": "Compliance Officer",
  "description": "Access to compliance and audit features",
  "permissions": [
    "audit:read",
    "audit:export",
    "reports:read",
    "reports:export",
    "devices:read"
  ]
}
```

**API:**
- `GET /api/v1/rbac/roles` - List roles
- `POST /api/v1/rbac/roles` - Create role
- `GET /api/v1/rbac/roles/{id}` - Get role
- `DELETE /api/v1/rbac/roles/{id}` - Delete role
- `POST /api/v1/rbac/users/{userId}/roles` - Assign roles
- `GET /api/v1/rbac/permissions` - List all permissions

### 10. Reporting Dashboard

**Dashboard Metrics:**
- Total devices
- Active devices
- Total backups
- Successful backups
- Failed backups
- Total storage used
- Average backup size
- Compliance rate

**Charts and Visualizations:**
1. **Backup Success Rate** - Line chart showing success rate over time
2. **Storage Usage** - Bar chart showing usage by device
3. **Compliance Status** - Pie chart showing compliant vs non-compliant devices
4. **Device Health** - Table with device status and last sync times

**Report Types:**
- Dashboard Summary Report
- Backup Success Rate Report
- Storage Usage Report
- Compliance Report
- Device Health Report

**Export Formats:**
- PDF
- CSV
- JSON

**API:**
- `GET /api/v1/reports/dashboard` - Dashboard statistics
- `GET /api/v1/reports/backup-success-rates` - Success rates over time
- `GET /api/v1/reports/storage-usage` - Storage breakdown
- `GET /api/v1/reports/compliance` - Compliance report
- `GET /api/v1/reports/device-health` - Device health status
- `GET /api/v1/reports/export/pdf` - Export report as PDF

## Technical Stack

### Backend
- **Language**: Kotlin 1.9.22
- **Framework**: Ktor 2.3.7
- **Database**: PostgreSQL 15
- **ORM**: Exposed
- **Authentication**: JWT + SAML 2.0
- **Container**: Docker

### Frontend
- **Framework**: React 18
- **UI Library**: Material-UI 5
- **Charts**: Recharts
- **Data Grid**: MUI X Data Grid
- **HTTP Client**: Axios
- **Build Tool**: Vite

### Infrastructure
- **Containerization**: Docker + Docker Compose
- **Database**: PostgreSQL with connection pooling (HikariCP)
- **Reverse Proxy**: Nginx
- **Logging**: Logback with file rotation

## Installation & Deployment

### Prerequisites
- Docker 24.0+
- Docker Compose 2.0+
- PostgreSQL 15+ (or use Docker)
- Node.js 20+ (for local development)
- JDK 17+ (for backend development)

### Quick Start with Docker

```bash
# Clone the repository
cd /root/workspace/ObsidianBackup/enterprise

# Set environment variables
export JWT_SECRET=$(openssl rand -base64 32)

# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f
```

### Access Points
- **Admin Console**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **API Health**: http://localhost:8080/health

### Default Credentials
```
Email: admin@example.com
Password: admin123
```
**⚠️ Change immediately after first login!**

### Manual Installation

#### Backend Setup

```bash
cd backend

# Build
./gradlew build

# Run
java -jar build/libs/enterprise-backend-1.0.0.jar
```

#### Frontend Setup

```bash
cd admin-console

# Install dependencies
npm install

# Development mode
npm run dev

# Production build
npm run build
```

### Database Setup

```sql
-- Create database
CREATE DATABASE obsidian_enterprise;

-- Connect to database
\c obsidian_enterprise

-- Tables are auto-created by Exposed ORM on first run
```

## Configuration

### Backend Configuration

Edit `backend/src/main/resources/application.conf`:

```hocon
ktor {
    deployment {
        port = 8080
        host = "0.0.0.0"
    }
}

jwt {
    secret = "your-secure-secret-key"
    issuer = "obsidian-enterprise"
    audience = "obsidian-clients"
    realm = "ObsidianBackup Enterprise"
}

database {
    url = "jdbc:postgresql://localhost:5432/obsidian_enterprise"
    user = "postgres"
    password = "secure-password"
}
```

### SAML Configuration

```kotlin
// In organization settings or via API
{
  "samlConfig": {
    "entityId": "obsidian-enterprise",
    "ssoUrl": "https://idp.example.com/sso",
    "certificate": "-----BEGIN CERTIFICATE-----...",
    "attributeMapping": {
      "email": "emailAddress",
      "name": "displayName",
      "groups": "memberOf"
    }
  }
}
```

### Environment Variables

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/obsidian_enterprise
DATABASE_USER=postgres
DATABASE_PASSWORD=secure-password

# JWT
JWT_SECRET=your-secure-secret-key

# API URL (for frontend)
VITE_API_URL=http://localhost:8080
```

## API Documentation

### Authentication

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "admin123"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "uuid",
    "email": "admin@example.com",
    "name": "Admin User",
    "roles": ["SUPER_ADMIN"]
  }
}
```

#### SAML Login
```http
POST /api/v1/auth/saml/initiate
Content-Type: application/json

{
  "organizationId": "org-uuid"
}

Response:
{
  "authUrl": "https://idp.example.com/saml/sso?SAMLRequest=..."
}
```

### Devices

#### List Devices
```http
GET /api/v1/devices
Authorization: Bearer {token}

Response:
[
  {
    "id": "device-uuid",
    "name": "John's Pixel 8",
    "platform": "Android",
    "status": "ACTIVE",
    "complianceStatus": {
      "isCompliant": true,
      "violations": [],
      "lastCheckAt": "2026-02-08T12:00:00Z"
    }
  }
]
```

#### Remote Wipe
```http
POST /api/v1/devices/{deviceId}/wipe
Authorization: Bearer {token}
Content-Type: application/json

{
  "deviceId": "device-uuid",
  "reason": "Device lost"
}

Response:
{
  "message": "Device wipe initiated"
}
```

### Policies

#### Create Policy
```http
POST /api/v1/policies
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Daily Backup Policy",
  "type": "BACKUP_SCHEDULE",
  "config": {
    "frequency": "daily",
    "time": "02:00"
  },
  "targetDevices": ["device-uuid-1", "device-uuid-2"]
}

Response:
{
  "id": "policy-uuid",
  "name": "Daily Backup Policy",
  "type": "BACKUP_SCHEDULE",
  "isEnforced": true,
  "createdAt": "2026-02-08T12:00:00Z"
}
```

### Audit Logs

#### Query Logs
```http
GET /api/v1/audit?userId=user-uuid&action=DEVICE_WIPED&page=1&pageSize=50
Authorization: Bearer {token}

Response:
[
  {
    "id": "log-uuid",
    "timestamp": "2026-02-08T12:00:00Z",
    "userId": "user-uuid",
    "action": "DEVICE_WIPED",
    "status": "SUCCESS",
    "details": {
      "reason": "Security violation"
    }
  }
]
```

### Reports

#### Dashboard Stats
```http
GET /api/v1/reports/dashboard
Authorization: Bearer {token}

Response:
{
  "totalDevices": 150,
  "activeDevices": 142,
  "totalBackups": 5420,
  "successfulBackups": 5380,
  "failedBackups": 40,
  "totalStorageUsed": 536870912000,
  "averageBackupSize": 99078169,
  "complianceRate": 0.95
}
```

## Security Best Practices

### Production Deployment

1. **Change Default Credentials**
   - Update admin password immediately
   - Use strong, unique passwords

2. **Secure JWT Secret**
   ```bash
   export JWT_SECRET=$(openssl rand -base64 64)
   ```

3. **Enable HTTPS**
   - Use Let's Encrypt or commercial SSL
   - Configure TLS 1.3 minimum
   - Enable HSTS

4. **Database Security**
   - Use strong passwords
   - Limit network access
   - Enable SSL connections
   - Regular backups

5. **Environment Variables**
   - Never commit secrets to git
   - Use .env files with proper permissions
   - Rotate credentials regularly

6. **Network Security**
   - Use firewall rules
   - Limit API access by IP
   - Enable rate limiting
   - DDoS protection

7. **Monitoring**
   - Enable access logging
   - Monitor failed login attempts
   - Set up alerts for suspicious activity
   - Regular security audits

## Monitoring & Maintenance

### Health Checks

```bash
# Backend health
curl http://localhost:8080/health

# Database connection
docker-compose exec postgres pg_isready

# View logs
docker-compose logs -f backend
docker-compose logs -f admin-console
```

### Backup Database

```bash
# Backup
docker-compose exec postgres pg_dump -U postgres obsidian_enterprise > backup.sql

# Restore
docker-compose exec -T postgres psql -U postgres obsidian_enterprise < backup.sql
```

### Log Rotation

Logs are automatically rotated daily and kept for 30 days. Configure in `logback.xml`:

```xml
<maxHistory>30</maxHistory>
```

## Troubleshooting

### Common Issues

1. **Backend won't start**
   - Check database connection
   - Verify environment variables
   - Check logs: `docker-compose logs backend`

2. **Frontend can't connect to API**
   - Verify VITE_API_URL is set correctly
   - Check CORS configuration
   - Ensure backend is running

3. **Database connection errors**
   - Verify PostgreSQL is running
   - Check credentials
   - Ensure database exists

4. **SAML authentication fails**
   - Verify IdP configuration
   - Check certificate validity
   - Review SAML response in logs

## Support & Licensing

### Enterprise Support

- **Email**: enterprise@obsidianbackup.com
- **Documentation**: https://docs.obsidianbackup.com/enterprise
- **Support Portal**: https://support.obsidianbackup.com

### Licensing

- Enterprise Edition requires a valid license key
- Unlimited devices with enterprise license
- Annual subscription model
- Volume discounts available

## Roadmap

### Upcoming Features

- **Q2 2026**
  - Multi-tenancy support
  - Advanced analytics with ML predictions
  - Mobile admin app (iOS/Android)

- **Q3 2026**
  - Kubernetes deployment support
  - Advanced workflow automation
  - Integration with ticketing systems

- **Q4 2026**
  - AI-powered anomaly detection
  - Predictive maintenance
  - Advanced data lifecycle management

## Appendix

### API Reference

Complete API documentation available at:
- Swagger UI: http://localhost:8080/swagger-ui
- OpenAPI Spec: http://localhost:8080/openapi.json

### Database Schema

See `enterprise/backend/src/main/kotlin/com/obsidianbackup/enterprise/database/Tables.kt` for complete schema.

### Compliance Certifications

- SOC 2 Type II (In Progress)
- ISO 27001 (In Progress)
- GDPR Compliant
- HIPAA Compliant

---

**© 2026 ObsidianBackup Enterprise. All rights reserved.**
