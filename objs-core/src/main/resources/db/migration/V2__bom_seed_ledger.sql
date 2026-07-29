-- Durable seed resource ledger: latest successful fingerprint retained separately from
-- latest attempt / failure diagnostics so a failed re-import cannot erase a prior success.
CREATE TABLE bom_seed_ledger (
    seed_key                 VARCHAR(512)  NOT NULL,
    last_success_fingerprint VARCHAR(128),
    last_success_at          TIMESTAMP,
    last_attempt_fingerprint VARCHAR(128),
    last_attempt_status      VARCHAR(32)   NOT NULL,
    last_attempt_at          TIMESTAMP     NOT NULL,
    last_error               TEXT,
    created_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bom_seed_ledger PRIMARY KEY (seed_key)
);

CREATE INDEX idx_bom_seed_ledger_status ON bom_seed_ledger (last_attempt_status);
