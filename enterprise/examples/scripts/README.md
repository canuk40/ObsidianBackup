# Enterprise Automation Scripts

Collection of scripts for automating enterprise operations.

## Scripts

### bulk-register-devices.sh
Bulk register devices from CSV file.

**Usage:**
```bash
./bulk-register-devices.sh devices.csv
```

**CSV Format:**
```csv
name,platform,osVersion,appVersion,userId
John's Phone,Android,14.0,1.0.0,user-uuid-1
Jane's Tablet,Android,13.0,1.0.0,user-uuid-2
```

### deploy-policies.sh
Deploy standard enterprise policies.

**Usage:**
```bash
./deploy-policies.sh
```

Deploys:
- Daily backup schedule
- 90-day retention policy
- AES-256 encryption policy

### analyze-audit-logs.py
Analyze audit logs for security insights.

**Requirements:**
```bash
pip install requests
```

**Usage:**
```bash
python3 analyze-audit-logs.py
```

Generates:
- Action frequency analysis
- Top users report
- Failed operations summary
- Security insights

## Configuration

Update these variables in each script:
- `API_URL` - Your backend URL
- `TOKEN` - Your JWT authentication token

## Getting API Token

```bash
# Login to get token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'

# Extract token from response
TOKEN="eyJhbGciOiJIUzI1NiIs..."
```

## Best Practices

1. Store API tokens securely (use environment variables)
2. Test scripts in staging before production
3. Keep audit logs of bulk operations
4. Schedule regular policy deployments
5. Monitor script execution logs

## Support

See parent directory documentation for more information.
