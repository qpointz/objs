-- Policy / Category / PolicySuite catalog (C-28). H2 JSON (no GIN). Greenfield only.
-- Identity: ["key"] is the human-managed logical identity (G-P36seed); [name] is display.
-- "key" is quoted throughout — it is a reserved word in H2.

CREATE TABLE objs_policy_category (
    id UUID NOT NULL PRIMARY KEY,
    "key" VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_objs_policy_category_key UNIQUE ("key")
);

CREATE TABLE objs_policy (
    id UUID NOT NULL PRIMARY KEY,
    "key" VARCHAR(255) NOT NULL,
    serial BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    engine_kind VARCHAR(64) NOT NULL,
    body TEXT NOT NULL,
    content_type VARCHAR(255),
    applicability_kind VARCHAR(64),
    applicability_body TEXT,
    category_id UUID NOT NULL,
    tags JSON NOT NULL DEFAULT '[]',
    annotations JSON NOT NULL DEFAULT '{}',
    version VARCHAR(32) NOT NULL DEFAULT '0.1',
    description VARCHAR NOT NULL DEFAULT '',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_objs_policy_key_serial UNIQUE ("key", serial),
    CONSTRAINT fk_objs_policy_category
        FOREIGN KEY (category_id) REFERENCES objs_policy_category (id)
);

CREATE INDEX idx_objs_policy_key ON objs_policy ("key");
CREATE INDEX idx_objs_policy_category_id ON objs_policy (category_id);

CREATE TABLE objs_policy_suite (
    id UUID NOT NULL PRIMARY KEY,
    "key" VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    roll_up_strategy_kind VARCHAR(64) NOT NULL DEFAULT 'BUILTIN',
    execution_strategy_kind VARCHAR(64) NOT NULL DEFAULT 'DEDUPE',
    tags JSON NOT NULL DEFAULT '[]',
    annotations JSON NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_objs_policy_suite_key UNIQUE ("key")
);

CREATE TABLE objs_policy_suite_folder (
    id UUID NOT NULL PRIMARY KEY,
    suite_id UUID NOT NULL,
    "key" VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    parent_id UUID,
    sort_order INT NOT NULL DEFAULT 0,
    participation VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    roll_up_mode VARCHAR(32) NOT NULL DEFAULT 'ALL_PASS',
    matchers JSON NOT NULL DEFAULT '[]',
    tags JSON NOT NULL DEFAULT '[]',
    annotations JSON NOT NULL DEFAULT '{}',
    severity_config VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_objs_policy_suite_folder_suite_key UNIQUE (suite_id, "key"),
    CONSTRAINT fk_objs_policy_suite_folder_suite
        FOREIGN KEY (suite_id) REFERENCES objs_policy_suite (id) ON DELETE CASCADE,
    CONSTRAINT fk_objs_policy_suite_folder_parent
        FOREIGN KEY (parent_id) REFERENCES objs_policy_suite_folder (id) ON DELETE CASCADE
);

CREATE INDEX idx_objs_policy_suite_folder_suite ON objs_policy_suite_folder (suite_id);
CREATE INDEX idx_objs_policy_suite_folder_parent ON objs_policy_suite_folder (parent_id);
