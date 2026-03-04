-- OpenDebt Letter Service - Initial Schema
-- Database: PostgreSQL

CREATE TABLE letter_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_code VARCHAR(50) NOT NULL,
    version VARCHAR(20) NOT NULL,
    letter_type VARCHAR(30) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    content TEXT NOT NULL,
    variables JSONB,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    approved_at TIMESTAMP,
    approved_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_template_code_version UNIQUE (template_code, version),
    CONSTRAINT chk_letter_type CHECK (letter_type IN (
        'DEBT_NOTIFICATION', 'PAYMENT_REMINDER', 'WAGE_GARNISHMENT_NOTICE',
        'OFFSETTING_NOTICE', 'PAYMENT_CONFIRMATION', 'PAYMENT_PLAN_PROPOSAL',
        'PAYMENT_PLAN_CONFIRMATION', 'CASE_CLOSURE', 'APPEAL_ACKNOWLEDGEMENT'
    ))
);

CREATE TABLE letters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id UUID,
    debtor_id VARCHAR(11) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    template_version VARCHAR(20) NOT NULL,
    letter_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    delivery_channel VARCHAR(20) NOT NULL,
    recipient_address TEXT,
    recipient_email VARCHAR(200),
    template_variables JSONB,
    generated_content TEXT,
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    delivery_tracking_id VARCHAR(100),
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    
    CONSTRAINT chk_letter_status CHECK (status IN (
        'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'SCHEDULED',
        'GENERATING', 'SENT', 'DELIVERED', 'FAILED', 'CANCELLED'
    )),
    CONSTRAINT chk_delivery_channel CHECK (delivery_channel IN (
        'DIGITAL_POST', 'EMAIL', 'PHYSICAL_MAIL', 'E_BOKS'
    ))
);

-- Indexes
CREATE INDEX idx_letter_case_id ON letters(case_id);
CREATE INDEX idx_letter_debtor_id ON letters(debtor_id);
CREATE INDEX idx_letter_status ON letters(status);
CREATE INDEX idx_letter_scheduled_at ON letters(scheduled_at);
CREATE INDEX idx_template_active ON letter_templates(template_code, active);

-- Audit trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_letters_updated_at
    BEFORE UPDATE ON letters
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_letter_templates_updated_at
    BEFORE UPDATE ON letter_templates
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE letters IS 'Generated and sent letters';
COMMENT ON TABLE letter_templates IS 'Letter templates with versioning';
