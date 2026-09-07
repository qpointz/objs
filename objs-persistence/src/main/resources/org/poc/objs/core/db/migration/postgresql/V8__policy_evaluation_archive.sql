-- Evaluation result archives (C-33). PostgreSQL JSONB. No FKs to catalog tables (G-P46r).
-- Persist options live in extensible persist_profile JSONB (not fixed columns).

CREATE TABLE objs_policy_evaluation (
    evaluation_id UUID NOT NULL PRIMARY KEY,
    kind VARCHAR(64) NOT NULL,
    evaluated_at TIMESTAMP NOT NULL,
    execution_strategy_kind VARCHAR(64),
    rollup_strategy_kind VARCHAR(64),
    overall_status VARCHAR(32),
    overall_severity VARCHAR(64),
    suite_id UUID,
    suite_name VARCHAR(512),
    tags JSONB NOT NULL DEFAULT '[]',
    annotations JSONB NOT NULL DEFAULT '{}',
    persist_profile JSONB NOT NULL DEFAULT '{}',
    suite_tree JSONB,
    execution_context JSONB,
    input_fragment JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE objs_policy_outcome (
    outcome_id UUID NOT NULL PRIMARY KEY,
    evaluation_id UUID NOT NULL,
    ordinal INT NOT NULL,
    policy_name VARCHAR(512) NOT NULL,
    policy_serial BIGINT NOT NULL,
    engine_kind VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    not_applicable_reason TEXT,
    message TEXT,
    CONSTRAINT fk_objs_policy_outcome_evaluation
        FOREIGN KEY (evaluation_id) REFERENCES objs_policy_evaluation (evaluation_id) ON DELETE CASCADE
);

CREATE INDEX idx_objs_policy_outcome_evaluation ON objs_policy_outcome (evaluation_id);

CREATE TABLE objs_policy_finding (
    finding_id UUID NOT NULL PRIMARY KEY,
    outcome_id UUID NOT NULL,
    idx INT NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(64),
    code VARCHAR(128),
    entity_ids JSONB NOT NULL DEFAULT '[]',
    edge_ids JSONB NOT NULL DEFAULT '[]',
    extras JSONB NOT NULL DEFAULT '{}',
    CONSTRAINT fk_objs_policy_finding_outcome
        FOREIGN KEY (outcome_id) REFERENCES objs_policy_outcome (outcome_id) ON DELETE CASCADE
);

CREATE INDEX idx_objs_policy_finding_outcome ON objs_policy_finding (outcome_id);
