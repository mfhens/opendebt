-- Add review_status to business_config (petition 047)
ALTER TABLE business_config ADD COLUMN IF NOT EXISTS review_status VARCHAR(20);
CREATE INDEX IF NOT EXISTS idx_config_review_status ON business_config(review_status) WHERE review_status IS NOT NULL;

-- Create business_config_audit table (petition 047)
CREATE TABLE IF NOT EXISTS business_config_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_entry_id UUID NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    action VARCHAR(20) NOT NULL,
    old_value VARCHAR(500),
    new_value VARCHAR(500),
    performed_by VARCHAR(100) NOT NULL,
    performed_at TIMESTAMP NOT NULL,
    details TEXT
);
CREATE INDEX IF NOT EXISTS idx_bca_config_entry_id ON business_config_audit(config_entry_id);
CREATE INDEX IF NOT EXISTS idx_bca_config_key ON business_config_audit(config_key);
CREATE INDEX IF NOT EXISTS idx_bca_performed_at ON business_config_audit(performed_at);
