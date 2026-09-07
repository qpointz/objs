-- Evaluation result archives (C-33). H2 JSON. No FKs to catalog tables (G-P46r).
-- Persist options live in extensible persist_profile JSON (not fixed columns).

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
    tags JSON NOT NULL DEFAULT '[]',
    annotations JSON NOT NULL DEFAULT '{}',
    persist_profile JSON NOT NULL DEFAULT '{}',
    suite_tree JSON,
    execution_context JSON,
    input_fragment JSON,
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
    not_applicable_reason VARCHAR,
    message VARCHAR,
    CONSTRAINT fk_objs_policy_outcome_evaluation
        FOREIGN KEY (evaluation_id) REFERENCES objs_policy_evaluation (evaluation_id) ON DELETE CASCADE
);

CREATE INDEX idx_objs_policy_outcome_evaluation ON objs_policy_outcome (evaluation_id);

CREATE TABLE objs_policy_finding (
    finding_id UUID NOT NULL PRIMARY KEY,
    outcome_id UUID NOT NULL,
    idx INT NOT NULL,
    message VARCHAR NOT NULL,
    severity VARCHAR(64),
    code VARCHAR(128),
    entity_ids JSON NOT NULL DEFAULT '[]',
    edge_ids JSON NOT NULL DEFAULT '[]',
    extras JSON NOT NULL DEFAULT '{}',
    CONSTRAINT fk_objs_policy_finding_outcome
        FOREIGN KEY (outcome_id) REFERENCES objs_policy_outcome (outcome_id) ON DELETE CASCADE
);

CREATE INDEX idx_objs_policy_finding_outcome ON objs_policy_finding (outcome_id);
