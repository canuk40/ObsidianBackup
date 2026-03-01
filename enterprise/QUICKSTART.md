# ObsidianBackup Enterprise Edition - Quick Start Guide

## Prerequisites

- Docker & Docker Compose installed
- 4GB+ RAM available
- 10GB+ disk space

## Installation Steps

### 1. Start the Services

```bash
cd /root/workspace/ObsidianBackup/enterprise
docker-compose up -d
```

### 2. Verify Services

```bash
# Check all services are running
docker-compose ps

# Expected output:
# NAME                 STATUS              PORTS
# enterprise-backend   Up 10 seconds       0.0.0.0:8080->8080/tcp
# enterprise-console   Up 10 seconds       0.0.0.0:3000->80/tcp
# enterprise-postgres  Up 15 seconds       0.0.0.0:5432->5432/tcp
```

### 3. Access Admin Console

Open your browser to: http://localhost:3000

**Default Login:**
- Email: `admin@example.com`
- Password: `admin123`

⚠️ **Change immediately after first login!**

### 4. Initial Configuration

1. Navigate to **Settings** → **Organization Settings**
2. Update organization name and domain
3. Configure SAML/SSO (if needed)
4. Set compliance requirements

### 5. Create Your First Policy

1. Go to **Policies** → **Create Policy**
2. Choose policy type (e.g., Backup Schedule)
3. Configure settings
4. Select target devices
5. Click **Create**

### 6. Register Devices

Devices can be registered through:
- Mobile app registration flow
- Bulk import via API
- Manual registration in admin console

## Common Tasks

### View Dashboard

Navigate to **Dashboard** to see:
- Device statistics
- Backup success rates
- Storage usage
- Compliance metrics

### Remote Wipe Device

1. Go to **Devices**
2. Find the device
3. Click **Wipe** button
4. Enter reason
5. Confirm action

### Export Audit Logs

1. Go to **Audit Logs**
2. Set date filters
3. Click **Export CSV**
4. Save file

### Create Custom Role

1. Go to **RBAC** → **Create Role**
2. Enter role name and description
3. Select permissions
4. Click **Create**

## Troubleshooting

### Services Won't Start

```bash
# Check Docker status
docker ps

# View service logs
docker-compose logs -f backend
docker-compose logs -f admin-console
```

### Can't Login

1. Check backend is running: `curl http://localhost:8080/health`
2. Verify credentials
3. Check browser console for errors

### Database Connection Failed

```bash
# Restart PostgreSQL
docker-compose restart postgres

# Check connection
docker-compose exec postgres pg_isready
```

## Next Steps

1. ✅ Configure SAML/SSO
2. ✅ Set up backup policies
3. ✅ Create user roles
4. ✅ Register devices
5. ✅ Review compliance settings

## Support

- Documentation: See ENTERPRISE_EDITION.md
- Issues: Check logs with `docker-compose logs`
- Email: enterprise@obsidianbackup.com

## Security Checklist

- [ ] Change default admin password
- [ ] Configure HTTPS/SSL
- [ ] Set strong JWT secret
- [ ] Enable firewall rules
- [ ] Configure SAML/SSO
- [ ] Review audit log settings
- [ ] Set up regular database backups

---

**Ready to manage your enterprise backup fleet!** 🚀
