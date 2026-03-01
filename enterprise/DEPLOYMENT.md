# Production Deployment Guide

**ObsidianBackup Enterprise Backend**  
Version 1.0.0

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Cloud Provider Deployment](#cloud-provider-deployment)
   - [AWS Deployment](#aws-deployment)
   - [Google Cloud Platform](#google-cloud-platform)
   - [Microsoft Azure](#microsoft-azure)
3. [Database Management](#database-management)
4. [SSL/TLS Configuration](#ssltls-configuration)
5. [Monitoring & Observability](#monitoring--observability)
6. [Scaling Strategies](#scaling-strategies)
7. [Security Hardening](#security-hardening)
8. [Disaster Recovery](#disaster-recovery)
9. [Performance Tuning](#performance-tuning)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Tools
- Docker 24.0+ and Docker Compose 2.20+
- `kubectl` (for Kubernetes deployments)
- Cloud CLI tools:
  - AWS CLI 2.x (`aws`)
  - Google Cloud SDK (`gcloud`)
  - Azure CLI (`az`)

### Infrastructure Requirements
- **Compute:** 2+ vCPUs, 4GB+ RAM (minimum per instance)
- **Database:** PostgreSQL 16 (managed service recommended)
- **Cache:** Redis 7 (managed service recommended)
- **Storage:** 20GB+ for application, 100GB+ for database
- **Network:** Static IP or load balancer with SSL/TLS

### Environment Variables
Copy `.env.example` to `.env` and configure:
```bash
cp .env.example .env
# Edit .env with production values
```

**Critical Secrets to Generate:**
```bash
# JWT secret (256-bit)
openssl rand -base64 32

# Database password
openssl rand -base64 24

# Redis password
openssl rand -base64 24
```

---

## Cloud Provider Deployment

### AWS Deployment

#### Architecture
- **Compute:** ECS Fargate or EC2 with Auto Scaling
- **Database:** RDS for PostgreSQL 16
- **Cache:** ElastiCache for Redis 7
- **Storage:** EFS for shared volumes (optional)
- **Load Balancer:** Application Load Balancer (ALB)
- **DNS:** Route 53
- **Secrets:** AWS Secrets Manager
- **Monitoring:** CloudWatch + CloudWatch Logs

#### Step 1: Create RDS PostgreSQL Database

```bash
# Create DB subnet group
aws rds create-db-subnet-group \
  --db-subnet-group-name obsidian-db-subnet \
  --db-subnet-group-description "ObsidianBackup DB Subnet" \
  --subnet-ids subnet-abc123 subnet-def456 \
  --tags Key=Project,Value=ObsidianBackup

# Create RDS PostgreSQL instance
aws rds create-db-instance \
  --db-instance-identifier obsidian-enterprise-db \
  --db-instance-class db.t4g.medium \
  --engine postgres \
  --engine-version 16.1 \
  --master-username obsidian_admin \
  --master-user-password "$(openssl rand -base64 24)" \
  --allocated-storage 100 \
  --storage-type gp3 \
  --storage-encrypted \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --db-subnet-group-name obsidian-db-subnet \
  --vpc-security-group-ids sg-abc123 \
  --multi-az \
  --publicly-accessible false \
  --enable-performance-insights \
  --tags Key=Project,Value=ObsidianBackup
```

#### Step 2: Create ElastiCache Redis Cluster

```bash
# Create cache subnet group
aws elasticache create-cache-subnet-group \
  --cache-subnet-group-name obsidian-cache-subnet \
  --cache-subnet-group-description "ObsidianBackup Cache Subnet" \
  --subnet-ids subnet-abc123 subnet-def456

# Create Redis cluster
aws elasticache create-replication-group \
  --replication-group-id obsidian-redis \
  --replication-group-description "ObsidianBackup Redis" \
  --engine redis \
  --engine-version 7.0 \
  --cache-node-type cache.t4g.medium \
  --num-cache-clusters 2 \
  --automatic-failover-enabled \
  --at-rest-encryption-enabled \
  --transit-encryption-enabled \
  --auth-token "$(openssl rand -base64 24)" \
  --cache-subnet-group-name obsidian-cache-subnet \
  --security-group-ids sg-def456 \
  --tags Key=Project,Value=ObsidianBackup
```

#### Step 3: Store Secrets in AWS Secrets Manager

```bash
# Store database password
aws secretsmanager create-secret \
  --name obsidian/database/password \
  --secret-string "your-generated-password"

# Store Redis password
aws secretsmanager create-secret \
  --name obsidian/redis/password \
  --secret-string "your-generated-password"

# Store JWT secret
aws secretsmanager create-secret \
  --name obsidian/jwt/secret \
  --secret-string "$(openssl rand -base64 32)"
```

#### Step 4: Deploy to ECS Fargate

**Create ECS Task Definition:**
```json
{
  "family": "obsidian-enterprise",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "2048",
  "memory": "4096",
  "containerDefinitions": [
    {
      "name": "backend",
      "image": "your-ecr-repo/obsidian-enterprise:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "secrets": [
        {
          "name": "DATABASE_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:obsidian/database/password"
        },
        {
          "name": "REDIS_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:obsidian/redis/password"
        },
        {
          "name": "JWT_SECRET",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:obsidian/jwt/secret"
        }
      ],
      "environment": [
        {
          "name": "DATABASE_HOST",
          "value": "obsidian-enterprise-db.abc123.us-east-1.rds.amazonaws.com"
        },
        {
          "name": "REDIS_HOST",
          "value": "obsidian-redis.abc123.cache.amazonaws.com"
        },
        {
          "name": "ENVIRONMENT",
          "value": "production"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/obsidian-enterprise",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "backend"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

**Create ECS Service:**
```bash
aws ecs create-service \
  --cluster obsidian-cluster \
  --service-name obsidian-backend \
  --task-definition obsidian-enterprise:1 \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-abc123,subnet-def456],securityGroups=[sg-ghi789],assignPublicIp=DISABLED}" \
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:region:account:targetgroup/obsidian/abc123,containerName=backend,containerPort=8080" \
  --health-check-grace-period-seconds 60
```

---

### Google Cloud Platform

#### Architecture
- **Compute:** Cloud Run or GKE
- **Database:** Cloud SQL for PostgreSQL 16
- **Cache:** Memorystore for Redis 7
- **Load Balancer:** Cloud Load Balancing
- **DNS:** Cloud DNS
- **Secrets:** Secret Manager
- **Monitoring:** Cloud Monitoring + Cloud Logging

#### Step 1: Create Cloud SQL Instance

```bash
# Create PostgreSQL instance
gcloud sql instances create obsidian-enterprise-db \
  --database-version=POSTGRES_16 \
  --tier=db-custom-2-7680 \
  --region=us-central1 \
  --network=projects/PROJECT_ID/global/networks/default \
  --no-assign-ip \
  --database-flags=cloudsql.iam_authentication=on \
  --backup-start-time=03:00 \
  --enable-bin-log \
  --retained-backups-count=7 \
  --storage-size=100GB \
  --storage-type=SSD \
  --storage-auto-increase

# Create database
gcloud sql databases create obsidian_enterprise \
  --instance=obsidian-enterprise-db

# Create user
gcloud sql users create obsidian_ent \
  --instance=obsidian-enterprise-db \
  --password="$(openssl rand -base64 24)"
```

#### Step 2: Create Memorystore Redis

```bash
gcloud redis instances create obsidian-redis \
  --size=5 \
  --region=us-central1 \
  --redis-version=redis_7_0 \
  --tier=standard \
  --auth-enabled \
  --transit-encryption-mode=SERVER_AUTHENTICATION \
  --network=projects/PROJECT_ID/global/networks/default
```

#### Step 3: Store Secrets

```bash
# Create secrets
echo -n "$(openssl rand -base64 32)" | gcloud secrets create jwt-secret --data-file=-
echo -n "your-db-password" | gcloud secrets create database-password --data-file=-
echo -n "your-redis-password" | gcloud secrets create redis-password --data-file=-

# Grant Cloud Run access
gcloud secrets add-iam-policy-binding jwt-secret \
  --member="serviceAccount:PROJECT_NUMBER-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

#### Step 4: Deploy to Cloud Run

```bash
# Build and push Docker image
gcloud builds submit --tag gcr.io/PROJECT_ID/obsidian-enterprise:latest

# Deploy to Cloud Run
gcloud run deploy obsidian-backend \
  --image gcr.io/PROJECT_ID/obsidian-enterprise:latest \
  --platform managed \
  --region us-central1 \
  --min-instances 1 \
  --max-instances 10 \
  --cpu 2 \
  --memory 4Gi \
  --timeout 300 \
  --concurrency 80 \
  --set-env-vars="DATABASE_HOST=/cloudsql/PROJECT_ID:us-central1:obsidian-enterprise-db,REDIS_HOST=REDIS_IP,ENVIRONMENT=production" \
  --set-secrets="JWT_SECRET=jwt-secret:latest,DATABASE_PASSWORD=database-password:latest,REDIS_PASSWORD=redis-password:latest" \
  --add-cloudsql-instances PROJECT_ID:us-central1:obsidian-enterprise-db \
  --vpc-connector obsidian-connector \
  --allow-unauthenticated
```

---

### Microsoft Azure

#### Architecture
- **Compute:** Azure Container Instances or AKS
- **Database:** Azure Database for PostgreSQL Flexible Server
- **Cache:** Azure Cache for Redis
- **Load Balancer:** Azure Application Gateway
- **DNS:** Azure DNS
- **Secrets:** Azure Key Vault
- **Monitoring:** Azure Monitor + Log Analytics

#### Step 1: Create PostgreSQL Server

```bash
# Create resource group
az group create --name obsidian-rg --location eastus

# Create PostgreSQL server
az postgres flexible-server create \
  --resource-group obsidian-rg \
  --name obsidian-enterprise-db \
  --location eastus \
  --admin-user obsidian_admin \
  --admin-password "$(openssl rand -base64 24)" \
  --sku-name Standard_D2s_v3 \
  --tier GeneralPurpose \
  --version 16 \
  --storage-size 128 \
  --backup-retention 7 \
  --high-availability Enabled \
  --zone 1

# Create database
az postgres flexible-server db create \
  --resource-group obsidian-rg \
  --server-name obsidian-enterprise-db \
  --database-name obsidian_enterprise
```

#### Step 2: Create Azure Cache for Redis

```bash
az redis create \
  --resource-group obsidian-rg \
  --name obsidian-redis \
  --location eastus \
  --sku Premium \
  --vm-size P1 \
  --enable-non-ssl-port false \
  --redis-version 6 \
  --minimum-tls-version 1.2
```

#### Step 3: Create Key Vault and Store Secrets

```bash
# Create Key Vault
az keyvault create \
  --resource-group obsidian-rg \
  --name obsidian-vault \
  --location eastus

# Store secrets
az keyvault secret set --vault-name obsidian-vault --name jwt-secret --value "$(openssl rand -base64 32)"
az keyvault secret set --vault-name obsidian-vault --name database-password --value "your-password"
az keyvault secret set --vault-name obsidian-vault --name redis-password --value "your-password"
```

#### Step 4: Deploy to Azure Container Instances

```bash
# Create container instance with Key Vault integration
az container create \
  --resource-group obsidian-rg \
  --name obsidian-backend \
  --image your-acr.azurecr.io/obsidian-enterprise:latest \
  --cpu 2 \
  --memory 4 \
  --dns-name-label obsidian-enterprise \
  --ports 8080 \
  --environment-variables \
    DATABASE_HOST=obsidian-enterprise-db.postgres.database.azure.com \
    REDIS_HOST=obsidian-redis.redis.cache.windows.net \
    ENVIRONMENT=production \
  --secure-environment-variables \
    DATABASE_PASSWORD="$(az keyvault secret show --vault-name obsidian-vault --name database-password --query value -o tsv)" \
    REDIS_PASSWORD="$(az keyvault secret show --vault-name obsidian-vault --name redis-password --query value -o tsv)" \
    JWT_SECRET="$(az keyvault secret show --vault-name obsidian-vault --name jwt-secret --query value -o tsv)"
```

---

## Database Management

### Automated Backups

#### PostgreSQL WAL Archiving (Point-in-Time Recovery)

**Configure `postgresql.conf`:**
```ini
wal_level = replica
archive_mode = on
archive_command = 'aws s3 cp %p s3://obsidian-backups/wal/%f'
archive_timeout = 300
```

#### Daily Full Backups

**Backup Script (`/opt/scripts/backup-database.sh`):**
```bash
#!/bin/bash
set -euo pipefail

BACKUP_DIR="/backups/postgresql"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="obsidian_enterprise_${TIMESTAMP}.sql.gz"
S3_BUCKET="s3://obsidian-backups/daily"

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Perform backup
PGPASSWORD="$DATABASE_PASSWORD" pg_dump \
  -h "$DATABASE_HOST" \
  -U "$DATABASE_USER" \
  -d obsidian_enterprise \
  --format=custom \
  --verbose \
  | gzip > "${BACKUP_DIR}/${BACKUP_FILE}"

# Upload to S3
aws s3 cp "${BACKUP_DIR}/${BACKUP_FILE}" "${S3_BUCKET}/${BACKUP_FILE}"

# Cleanup old backups (keep last 30 days)
find "$BACKUP_DIR" -name "*.sql.gz" -mtime +30 -delete

# Verify backup integrity
aws s3api head-object --bucket obsidian-backups --key "daily/${BACKUP_FILE}" > /dev/null

echo "Backup completed: ${BACKUP_FILE}"
```

**Cron Job:**
```cron
# Run daily at 2 AM
0 2 * * * /opt/scripts/backup-database.sh >> /var/log/backup.log 2>&1
```

### Database Restore

**From Full Backup:**
```bash
# Download backup
aws s3 cp s3://obsidian-backups/daily/obsidian_enterprise_20260220_020000.sql.gz .

# Restore
gunzip obsidian_enterprise_20260220_020000.sql.gz
PGPASSWORD="$DATABASE_PASSWORD" pg_restore \
  -h "$DATABASE_HOST" \
  -U "$DATABASE_USER" \
  -d obsidian_enterprise \
  --clean \
  --if-exists \
  --verbose \
  obsidian_enterprise_20260220_020000.sql
```

**Point-in-Time Recovery (PITR):**
```bash
# Restore base backup
pg_restore -d obsidian_enterprise base_backup.dump

# Apply WAL files up to specific timestamp
recovery_target_time = '2026-02-20 14:30:00'
```

---

## SSL/TLS Configuration

### Let's Encrypt with Certbot

**Install Certbot:**
```bash
sudo apt-get update
sudo apt-get install -y certbot python3-certbot-nginx
```

**Obtain Certificate:**
```bash
sudo certbot --nginx -d enterprise.obsidianbackup.com -d api.obsidianbackup.com
```

### Nginx Reverse Proxy

**Configuration (`/etc/nginx/sites-available/obsidian-enterprise`):**
```nginx
upstream backend {
    least_conn;
    server 10.0.1.10:8080 max_fails=3 fail_timeout=30s;
    server 10.0.1.11:8080 max_fails=3 fail_timeout=30s;
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    listen [::]:80;
    server_name enterprise.obsidianbackup.com;
    return 301 https://$host$request_uri;
}

# HTTPS Configuration
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name enterprise.obsidianbackup.com;

    # SSL Certificates
    ssl_certificate /etc/letsencrypt/live/enterprise.obsidianbackup.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/enterprise.obsidianbackup.com/privkey.pem;
    ssl_trusted_certificate /etc/letsencrypt/live/enterprise.obsidianbackup.com/chain.pem;

    # SSL Configuration (Mozilla Modern)
    ssl_protocols TLSv1.3 TLSv1.2;
    ssl_ciphers 'ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    ssl_stapling on;
    ssl_stapling_verify on;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Proxy Settings
    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Health Check Endpoint
    location /actuator/health {
        proxy_pass http://backend;
        access_log off;
    }

    # Metrics Endpoint (restrict to internal network)
    location /actuator/prometheus {
        allow 10.0.0.0/8;
        deny all;
        proxy_pass http://backend;
    }
}
```

**Auto-Renewal:**
```bash
# Add to crontab
0 3 * * * certbot renew --quiet --post-hook "systemctl reload nginx"
```

---

## Monitoring & Observability

### Prometheus Metrics

**Prometheus Configuration (`prometheus.yml`):**
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'obsidian-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend-1:8080', 'backend-2:8080']
        labels:
          environment: 'production'
          service: 'obsidian-enterprise'
```

### Grafana Dashboards

**Import Spring Boot Dashboard:**
```bash
# Import JVM (Micrometer) Dashboard ID: 4701
# Import Spring Boot Statistics Dashboard ID: 12900
```

**Custom Metrics to Monitor:**
- Request rate (RPS)
- Request latency (p50, p95, p99)
- Error rate (4xx, 5xx)
- JVM heap/non-heap memory
- Database connection pool usage
- Redis cache hit/miss ratio
- Audit log write rate
- Device enrollment rate
- Policy evaluation time

### Log Aggregation (ELK Stack)

**Filebeat Configuration:**
```yaml
filebeat.inputs:
  - type: container
    paths:
      - '/var/lib/docker/containers/*/*.log'
    processors:
      - add_docker_metadata: ~

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "obsidian-backend-%{+yyyy.MM.dd}"
```

### Alerting Rules

**Prometheus Alerts:**
```yaml
groups:
  - name: obsidian_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} for {{ $labels.instance }}"

      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage"

      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active >= hikaricp_connections_max
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool exhausted"
```

---

## Scaling Strategies

### Horizontal Scaling

**Load Balancer Configuration:**
- Session affinity: Not required (stateless JWT)
- Health checks: `/actuator/health/liveness`
- Connection draining: 30 seconds
- SSL termination at load balancer

**Auto-Scaling Policies:**
```yaml
# AWS Auto Scaling Policy
TargetTrackingScaling:
  TargetValue: 70.0
  PredefinedMetricType: ECSServiceAverageCPUUtilization
  ScaleInCooldown: 300
  ScaleOutCooldown: 60
```

**Kubernetes HPA:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: obsidian-backend-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: obsidian-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
```

### Database Read Replicas

**PostgreSQL Read Replicas:**
```bash
# AWS RDS
aws rds create-db-instance-read-replica \
  --db-instance-identifier obsidian-db-replica-1 \
  --source-db-instance-identifier obsidian-enterprise-db \
  --db-instance-class db.t4g.medium \
  --availability-zone us-east-1b
```

**Application Configuration:**
```yaml
spring:
  datasource:
    hikari:
      data-source-properties:
        readOnly: true
    read-replica:
      jdbc-url: jdbc:postgresql://replica-1.amazonaws.com:5432/obsidian_enterprise
      username: obsidian_ent
      password: ${DATABASE_PASSWORD}
```

### Redis Clustering

**Redis Sentinel Configuration:**
```bash
# /etc/redis/sentinel.conf
sentinel monitor obsidian-redis redis-master 6379 2
sentinel down-after-milliseconds obsidian-redis 5000
sentinel parallel-syncs obsidian-redis 1
sentinel failover-timeout obsidian-redis 10000
```

---

## Security Hardening

### Network Security

**Firewall Rules (AWS Security Groups):**
```bash
# Backend security group
Allow Inbound:
  - Port 8080 from Load Balancer Security Group
  - Port 22 from Bastion Host (SSH)
  
Allow Outbound:
  - Port 5432 to Database Security Group (PostgreSQL)
  - Port 6379 to Redis Security Group
  - Port 443 to Internet (HTTPS)

# Database security group
Allow Inbound:
  - Port 5432 from Backend Security Group

# Redis security group
Allow Inbound:
  - Port 6379 from Backend Security Group
```

### Secrets Rotation

**AWS Secrets Manager Rotation Lambda:**
```python
def lambda_handler(event, context):
    arn = event['SecretId']
    token = event['ClientRequestToken']
    step = event['Step']
    
    if step == "createSecret":
        # Generate new password
        new_password = generate_password(32)
        service_client.put_secret_value(
            SecretId=arn,
            ClientRequestToken=token,
            SecretString=new_password,
            VersionStages=['AWSPENDING']
        )
    
    elif step == "setSecret":
        # Update database password
        update_database_password(new_password)
    
    elif step == "testSecret":
        # Test new password
        test_database_connection(new_password)
    
    elif step == "finishSecret":
        # Promote AWSPENDING to AWSCURRENT
        service_client.update_secret_version_stage(
            SecretId=arn,
            VersionStage='AWSCURRENT',
            MoveToVersionId=token
        )
```

### Regular Security Audits

**Vulnerability Scanning:**
```bash
# Docker image scanning
docker scan your-image:latest

# Dependency scanning
./gradlew dependencyCheckAnalyze

# OWASP ZAP API scan
zap-cli quick-scan https://api.obsidianbackup.com
```

---

## Disaster Recovery

### Backup Verification

**Monthly Restore Test:**
```bash
#!/bin/bash
# Test restore to temporary database

# Download latest backup
aws s3 cp s3://obsidian-backups/daily/latest.sql.gz /tmp/

# Restore to test database
createdb obsidian_test
gunzip < /tmp/latest.sql.gz | psql obsidian_test

# Run integrity checks
psql obsidian_test -c "SELECT COUNT(*) FROM organizations;"
psql obsidian_test -c "SELECT COUNT(*) FROM devices;"
psql obsidian_test -c "SELECT COUNT(*) FROM audit_logs;"

# Cleanup
dropdb obsidian_test
```

### Recovery Objectives

- **RTO (Recovery Time Objective):** 1 hour
- **RPO (Recovery Point Objective):** 15 minutes (WAL archiving)

### Failover Procedure

1. **Detect Failure:** Monitoring alerts trigger
2. **Verify Scope:** Check health checks, logs, metrics
3. **Initiate Failover:** 
   - Database: Promote read replica
   - Application: Route traffic to standby region
4. **Validate:** Test critical endpoints
5. **Monitor:** Watch for cascading failures
6. **Root Cause Analysis:** Post-incident review

---

## Performance Tuning

### JVM Optimization

**Production JVM Flags:**
```bash
java -XX:+UseContainerSupport \
     -XX:MaxRAMPercentage=75.0 \
     -XX:InitialRAMPercentage=50.0 \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:ParallelGCThreads=4 \
     -XX:ConcGCThreads=2 \
     -XX:+UseStringDeduplication \
     -XX:+ExitOnOutOfMemoryError \
     -Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=5,filesize=10m \
     -jar app.jar
```

### Database Tuning

**PostgreSQL Configuration:**
```ini
# /etc/postgresql/16/main/postgresql.conf

# Memory
shared_buffers = 2GB
effective_cache_size = 6GB
work_mem = 16MB
maintenance_work_mem = 512MB

# Connections
max_connections = 200

# Checkpoints
checkpoint_completion_target = 0.9
wal_buffers = 16MB

# Query Planner
random_page_cost = 1.1  # For SSD
effective_io_concurrency = 200

# Autovacuum
autovacuum_max_workers = 4
autovacuum_naptime = 10s
```

### Connection Pooling

**HikariCP Tuning:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

---

## Troubleshooting

### Common Issues

#### 1. Database Connection Failures

**Symptoms:** `Connection refused` or `Connection timeout`

**Diagnosis:**
```bash
# Test database connectivity
psql -h $DATABASE_HOST -U $DATABASE_USER -d obsidian_enterprise

# Check firewall rules
telnet $DATABASE_HOST 5432

# Verify credentials
echo $DATABASE_PASSWORD
```

**Resolution:**
- Verify security group allows port 5432
- Check database instance status
- Validate credentials in `.env`

#### 2. High Memory Usage

**Symptoms:** OutOfMemoryError, container restarts

**Diagnosis:**
```bash
# Check JVM heap dump
jmap -heap <pid>

# Analyze memory leaks
jcmd <pid> GC.heap_dump /tmp/heap.hprof
# Analyze with VisualVM or Eclipse MAT
```

**Resolution:**
- Increase container memory limits
- Tune JVM heap size (`-XX:MaxRAMPercentage`)
- Identify and fix memory leaks

#### 3. Slow API Responses

**Symptoms:** High latency, timeouts

**Diagnosis:**
```bash
# Check active database connections
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';

# Slow query log
SELECT query, calls, mean_exec_time 
FROM pg_stat_statements 
ORDER BY mean_exec_time DESC 
LIMIT 10;

# Application metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
```

**Resolution:**
- Add database indexes
- Optimize N+1 queries
- Enable database connection pooling
- Scale horizontally

#### 4. JWT Token Issues

**Symptoms:** `401 Unauthorized`, token validation failures

**Diagnosis:**
```bash
# Decode JWT token
echo "eyJhbGc..." | base64 -d

# Check Redis blacklist
redis-cli GET "jwt:blacklist:token-hash"

# Verify JWT secret matches
echo $JWT_SECRET
```

**Resolution:**
- Verify `JWT_SECRET` is consistent across instances
- Check token expiration times
- Clear Redis blacklist if needed

---

## Support & Resources

### Documentation
- API Documentation: `https://api.obsidianbackup.com/swagger-ui`
- Runbooks: `https://docs.obsidianbackup.com/runbooks`
- Architecture Diagrams: `/docs/architecture/`

### Monitoring Dashboards
- Grafana: `https://grafana.obsidianbackup.com`
- Kibana: `https://kibana.obsidianbackup.com`
- Prometheus: `https://prometheus.obsidianbackup.com`

### Emergency Contacts
- On-Call Engineer: PagerDuty
- Database Team: dba@obsidianbackup.com
- Security Team: security@obsidianbackup.com

---

**Document Version:** 1.0.0  
**Last Updated:** 2026-02-20  
**Maintained By:** DevOps Team
