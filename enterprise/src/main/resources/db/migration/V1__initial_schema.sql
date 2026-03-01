-- ObsidianBackup Enterprise Backend - Initial Schema
-- Finding 5: PostgreSQL Multi-Tenant with Row-Level Security (RLS)
-- Finding 8: SOC 2 Audit Logging with Immutability

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ORGANIZATIONS (Tenants)
-- ============================================================================

CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    plan_type VARCHAR(50) NOT NULL DEFAULT 'FREE',
    max_devices INTEGER NOT NULL DEFAULT 100,
    max_users INTEGER NOT NULL DEFAULT 10,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_plan_type CHECK (plan_type IN ('FREE', 'PROFESSIONAL', 'ENTERPRISE'))
);

CREATE INDEX idx_organizations_slug ON organizations(slug);
CREATE INDEX idx_organizations_enabled ON organizations(enabled);

-- ============================================================================
-- USERS
-- ============================================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255),  -- NULL for SAML-only users
    role VARCHAR(50) NOT NULL DEFAULT 'VIEWER',
    enabled BOOLEAN NOT NULL DEFAULT true,
    mfa_enabled BOOLEAN NOT NULL DEFAULT false,
    mfa_secret VARCHAR(255),
    last_login_at TIMESTAMP WITH TIME ZONE,
    last_login_ip INET,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_role CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'DEVICE_MANAGER', 'VIEWER')),
    CONSTRAINT unq_user_email_org UNIQUE (organization_id, email)
);

CREATE INDEX idx_users_organization ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_enabled ON users(enabled);

-- ============================================================================
-- REFRESH TOKENS (Finding 6: JWT Token Rotation)
-- ============================================================================

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    device_info JSONB,
    ip_address INET
);

CREATE INDEX idx_refresh_tokens_org ON refresh_tokens(organization_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked) WHERE NOT revoked;

-- Auto-cleanup expired tokens
CREATE INDEX idx_refresh_tokens_cleanup ON refresh_tokens(expires_at) WHERE NOT revoked;

-- ============================================================================
-- DEVICES (Finding 2: Device Management)
-- ============================================================================

CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    device_id VARCHAR(255) NOT NULL,
    device_name VARCHAR(255) NOT NULL,
    os_type VARCHAR(50) NOT NULL,
    os_version VARCHAR(100),
    app_version VARCHAR(100),
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    enrolled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enrolled_by UUID REFERENCES users(id) ON DELETE SET NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE,
    last_seen_ip INET,
    fcm_token VARCHAR(500),
    compliance_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    locked BOOLEAN NOT NULL DEFAULT false,
    wiped BOOLEAN NOT NULL DEFAULT false,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_os_type CHECK (os_type IN ('ANDROID', 'IOS', 'WINDOWS', 'MACOS', 'LINUX')),
    CONSTRAINT chk_compliance_status CHECK (compliance_status IN ('PENDING', 'COMPLIANT', 'NON_COMPLIANT', 'EXEMPTED')),
    CONSTRAINT unq_device_org UNIQUE (organization_id, device_id)
);

CREATE INDEX idx_devices_organization ON devices(organization_id);
CREATE INDEX idx_devices_device_id ON devices(device_id);
CREATE INDEX idx_devices_compliance ON devices(compliance_status);
CREATE INDEX idx_devices_last_seen ON devices(last_seen_at);
CREATE INDEX idx_devices_fcm_token ON devices(fcm_token) WHERE fcm_token IS NOT NULL;

-- ============================================================================
-- POLICIES (Finding 4: Policy Engine)
-- ============================================================================

CREATE TABLE policies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    policy_type VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    priority INTEGER NOT NULL DEFAULT 0,
    rules JSONB NOT NULL,
    compliance_frameworks VARCHAR(100)[],
    auto_remediation BOOLEAN NOT NULL DEFAULT false,
    remediation_actions JSONB,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_policy_type CHECK (policy_type IN ('SECURITY', 'COMPLIANCE', 'OPERATIONAL', 'CUSTOM')),
    CONSTRAINT unq_policy_name_org UNIQUE (organization_id, name)
);

CREATE INDEX idx_policies_organization ON policies(organization_id);
CREATE INDEX idx_policies_enabled ON policies(enabled);
CREATE INDEX idx_policies_type ON policies(policy_type);
CREATE INDEX idx_policies_priority ON policies(priority DESC);
CREATE INDEX idx_policies_frameworks ON policies USING GIN(compliance_frameworks);

-- ============================================================================
-- DEVICE POLICIES (Many-to-Many)
-- ============================================================================

CREATE TABLE device_policies (
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    policy_id UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    
    PRIMARY KEY (device_id, policy_id)
);

CREATE INDEX idx_device_policies_device ON device_policies(device_id);
CREATE INDEX idx_device_policies_policy ON device_policies(policy_id);

-- ============================================================================
-- POLICY EVALUATIONS
-- ============================================================================

CREATE TABLE policy_evaluations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    policy_id UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    evaluation_result VARCHAR(50) NOT NULL,
    violations JSONB,
    remediation_applied BOOLEAN NOT NULL DEFAULT false,
    remediation_details JSONB,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_evaluation_result CHECK (evaluation_result IN ('PASS', 'FAIL', 'ERROR', 'SKIPPED'))
);

CREATE INDEX idx_policy_eval_org ON policy_evaluations(organization_id);
CREATE INDEX idx_policy_eval_device ON policy_evaluations(device_id);
CREATE INDEX idx_policy_eval_policy ON policy_evaluations(policy_id);
CREATE INDEX idx_policy_eval_result ON policy_evaluations(evaluation_result);
CREATE INDEX idx_policy_eval_time ON policy_evaluations(evaluated_at DESC);

-- ============================================================================
-- DEVICE COMMANDS (Finding 2, 3: FCM Push Commands)
-- ============================================================================

CREATE TABLE device_commands (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    command_type VARCHAR(50) NOT NULL,
    command_payload JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    issued_by UUID REFERENCES users(id) ON DELETE SET NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    
    CONSTRAINT chk_command_type CHECK (command_type IN ('LOCK', 'UNLOCK', 'WIPE', 'SYNC_POLICY', 'UPDATE_APP', 'REBOOT', 'CUSTOM')),
    CONSTRAINT chk_command_status CHECK (status IN ('PENDING', 'QUEUED', 'DELIVERED', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_commands_org ON device_commands(organization_id);
CREATE INDEX idx_commands_device ON device_commands(device_id);
CREATE INDEX idx_commands_status ON device_commands(status);
CREATE INDEX idx_commands_issued ON device_commands(issued_at DESC);
CREATE INDEX idx_commands_pending ON device_commands(status, issued_at) WHERE status IN ('PENDING', 'QUEUED');

-- ============================================================================
-- AUDIT LOGS (Finding 8: SOC 2 Compliance)
-- Append-only, immutable, hash-chained
-- ============================================================================

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    sequence_number BIGSERIAL NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- SOC 2 Required Fields
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(255),
    outcome VARCHAR(20) NOT NULL,
    
    -- Context
    ip_address INET,
    user_agent TEXT,
    session_id VARCHAR(255),
    
    -- Details
    details JSONB,
    previous_state JSONB,
    new_state JSONB,
    
    -- Compliance
    compliance_frameworks VARCHAR(100)[],
    data_classification VARCHAR(50),
    
    -- Tamper Detection (Finding 8)
    previous_hash VARCHAR(64),
    entry_hash VARCHAR(64) NOT NULL,
    
    CONSTRAINT chk_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE', 'PARTIAL')),
    CONSTRAINT chk_data_classification CHECK (data_classification IN ('PUBLIC', 'INTERNAL', 'CONFIDENTIAL', 'PHI', 'PII'))
);

CREATE INDEX idx_audit_logs_org ON audit_logs(organization_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_outcome ON audit_logs(outcome);
CREATE INDEX idx_audit_logs_sequence ON audit_logs(sequence_number);
CREATE INDEX idx_audit_logs_frameworks ON audit_logs USING GIN(compliance_frameworks);

-- Make audit_logs append-only (no updates or deletes allowed)
CREATE RULE audit_logs_no_update AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
CREATE RULE audit_logs_no_delete AS ON DELETE TO audit_logs DO INSTEAD NOTHING;

-- ============================================================================
-- ROW-LEVEL SECURITY (Finding 5: Multi-Tenant Isolation)
-- ============================================================================

-- Enable RLS on all tenant tables
ALTER TABLE organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE policies ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_policies ENABLE ROW LEVEL SECURITY;
ALTER TABLE policy_evaluations ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_commands ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- RLS Policy: Users can only access data from their organization
-- Set current_organization via SET LOCAL app.current_organization = '<uuid>';

CREATE POLICY tenant_isolation_organizations ON organizations
    USING (id = current_setting('app.current_organization', true)::UUID);

CREATE POLICY tenant_isolation_users ON users
    USING (organization_id = current_setting('app.current_organization', true)::UUID);

CREATE POLICY tenant_isolation_refresh_tokens ON refresh_tokens
    USING (organization_id = current_setting('app.current_organization', true)::UUID);

CREATE POLICY tenant_isolation_devices ON devices
    USING (organization_id = current_setting('app.current_organization', true)::UUID);

CREATE POLICY tenant_isolation_policies ON policies
    USING (organization_id = current_setting('app.current_organization', true)::UUID);

CREATE POLICY tenant_isolation_policy_evaluations ON policy_evaluations
    USING (organization_id = current_setting('app.current_organization', true)::UUID);

CREATE POLICY tenant_isolation_device_commands ON device_commands
    USING (organization_id = current_setting('app.current_organization', true)::UUID);

CREATE POLICY tenant_isolation_audit_logs ON audit_logs
    USING (organization_id = current_setting('app.current_organization', true)::UUID);

-- ============================================================================
-- FUNCTIONS & TRIGGERS
-- ============================================================================

-- Function: Update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to relevant tables
CREATE TRIGGER update_organizations_updated_at BEFORE UPDATE ON organizations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_devices_updated_at BEFORE UPDATE ON devices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_policies_updated_at BEFORE UPDATE ON policies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function: Generate hash for audit log entry
CREATE OR REPLACE FUNCTION generate_audit_log_hash()
RETURNS TRIGGER AS $$
BEGIN
    -- Get previous hash from last entry
    SELECT entry_hash INTO NEW.previous_hash
    FROM audit_logs
    WHERE organization_id = NEW.organization_id
    ORDER BY sequence_number DESC
    LIMIT 1;
    
    -- Generate hash for current entry
    NEW.entry_hash = encode(
        digest(
            COALESCE(NEW.previous_hash, '') ||
            NEW.timestamp::TEXT ||
            NEW.action ||
            COALESCE(NEW.resource_id, '') ||
            NEW.outcome ||
            COALESCE(NEW.details::TEXT, ''),
            'sha256'
        ),
        'hex'
    );
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_log_hash_chain BEFORE INSERT ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION generate_audit_log_hash();

-- ============================================================================
-- INITIAL DATA
-- ============================================================================

-- Create system organization (for super admin operations)
INSERT INTO organizations (id, name, slug, plan_type, max_devices, max_users)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    'System',
    'system',
    'ENTERPRISE',
    999999,
    999999
);

-- Create default admin user (password: Admin123! - CHANGE IN PRODUCTION)
-- Password hash generated with bcrypt, cost 12
INSERT INTO users (organization_id, email, username, password_hash, role)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    'admin@obsidianbackup.com',
    'admin',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYKfPXP.Zmy',
    'SUPER_ADMIN'
);

-- Create demo organization
INSERT INTO organizations (name, slug, plan_type, max_devices, max_users)
VALUES (
    'Demo Organization',
    'demo',
    'PROFESSIONAL',
    500,
    50
);
