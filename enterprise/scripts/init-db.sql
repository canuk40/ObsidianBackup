-- ===================================
-- ObsidianBackup Enterprise Database
-- Initialization Script
-- ===================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create application schema (optional - using public for simplicity)
-- CREATE SCHEMA IF NOT EXISTS obsidian;

-- Set search path
-- SET search_path TO obsidian, public;

-- Grant permissions to application user
GRANT ALL PRIVILEGES ON DATABASE obsidian_enterprise TO obsidian_ent;

-- Create audit function for updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'ObsidianBackup Enterprise database initialized successfully';
END $$;
