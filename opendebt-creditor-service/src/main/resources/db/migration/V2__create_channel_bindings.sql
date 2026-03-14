-- OpenDebt Creditor Service - Channel Binding Model (petition010, W1-ACC-01)
-- Maps external channel identities (M2M certificates, portal users) to fordringshavere.

-- ============================================================================
-- CHANNEL BINDINGS TABLE
-- ============================================================================

CREATE TABLE channel_bindings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- The external identity string presented by the channel
    channel_identity VARCHAR(255) NOT NULL UNIQUE,

    -- Channel type: M2M or PORTAL
    channel_type VARCHAR(20) NOT NULL CHECK (channel_type IN ('M2M', 'PORTAL')),

    -- FK to the creditor this identity is bound to
    creditor_id UUID NOT NULL REFERENCES creditors(id),

    -- Whether this binding is currently active
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Optional description
    description VARCHAR(500),

    -- Audit fields
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    sys_period TSTZRANGE NOT NULL DEFAULT tstzrange(CURRENT_TIMESTAMP, NULL)
);

-- History table for channel_bindings
CREATE TABLE channel_bindings_history (
    LIKE channel_bindings,
    PRIMARY KEY (id, sys_period)
);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_cb_creditor_id ON channel_bindings(creditor_id);
CREATE INDEX idx_cb_channel_type ON channel_bindings(channel_type);
CREATE INDEX idx_cb_active ON channel_bindings(active) WHERE active = TRUE;

CREATE INDEX idx_cb_history_id ON channel_bindings_history(id);
CREATE INDEX idx_cb_history_period ON channel_bindings_history USING GIST (sys_period);

-- ============================================================================
-- TEMPORAL TRIGGERS
-- ============================================================================

CREATE OR REPLACE FUNCTION channel_bindings_versioning_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        INSERT INTO channel_bindings_history SELECT OLD.*;
        UPDATE channel_bindings_history SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        NEW.sys_period := tstzrange(CURRENT_TIMESTAMP, NULL);
        NEW.updated_at := CURRENT_TIMESTAMP;
        NEW.version := OLD.version + 1;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO channel_bindings_history SELECT OLD.*;
        UPDATE channel_bindings_history SET sys_period = tstzrange(lower(sys_period), CURRENT_TIMESTAMP)
        WHERE id = OLD.id AND upper(sys_period) IS NULL;
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER channel_bindings_versioning BEFORE UPDATE OR DELETE ON channel_bindings
    FOR EACH ROW EXECUTE FUNCTION channel_bindings_versioning_trigger();

CREATE TRIGGER channel_bindings_audit AFTER INSERT OR UPDATE OR DELETE ON channel_bindings
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE channel_bindings IS 'Maps external channel identities (M2M/portal) to fordringshavere';
COMMENT ON COLUMN channel_bindings.channel_identity IS 'External identity string (certificate subject or portal user ID)';
COMMENT ON COLUMN channel_bindings.channel_type IS 'Channel type: M2M (system-to-system) or PORTAL (human interaction)';
COMMENT ON COLUMN channel_bindings.creditor_id IS 'FK to creditors table — the fordringshaver this identity is bound to';
