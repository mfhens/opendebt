CREATE TABLE IF NOT EXISTS rentegodt_rate_entry (
    id uuid PRIMARY KEY,
    publication_date date NOT NULL,
    effective_date date NOT NULL,
    reference_rate_percent numeric(6,4) NOT NULL,
    godtgoerelse_rate_percent numeric(6,4) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_rre_publication UNIQUE (publication_date)
);
CREATE INDEX IF NOT EXISTS idx_rgre_effective_date ON rentegodt_rate_entry (effective_date);
