-- Canonical objs schema (pool + graphs). PostgreSQL JSONB + GIN. Greenfield only.

CREATE TABLE bom_entity (
    id UUID NOT NULL PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    schema_version VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    annotations JSONB NOT NULL
);

CREATE INDEX idx_bom_entity_type_schema_version ON bom_entity (type, schema_version);

CREATE INDEX idx_bom_entity_annotations_gin
    ON bom_entity USING GIN (annotations jsonb_path_ops);

CREATE TABLE bom_graph (
    id UUID NOT NULL PRIMARY KEY,
    annotations JSONB NOT NULL
);

CREATE INDEX idx_bom_graph_annotations_gin
    ON bom_graph USING GIN (annotations jsonb_path_ops);

CREATE TABLE bom_graph_entity (
    graph_id UUID NOT NULL,
    entity_id UUID NOT NULL,
    PRIMARY KEY (graph_id, entity_id),
    CONSTRAINT fk_bom_graph_entity_graph
        FOREIGN KEY (graph_id) REFERENCES bom_graph (id) ON DELETE CASCADE,
    CONSTRAINT fk_bom_graph_entity_entity
        FOREIGN KEY (entity_id) REFERENCES bom_entity (id) ON DELETE CASCADE
);

CREATE INDEX idx_bom_graph_entity_entity ON bom_graph_entity (entity_id);

CREATE TABLE bom_graph_edge (
    id UUID NOT NULL PRIMARY KEY,
    graph_id UUID NOT NULL,
    source_id UUID NOT NULL,
    target_id UUID NOT NULL,
    role VARCHAR(255) NOT NULL,
    type VARCHAR(255),
    schema_version VARCHAR(64),
    properties JSONB,
    CONSTRAINT fk_bom_graph_edge_graph
        FOREIGN KEY (graph_id) REFERENCES bom_graph (id) ON DELETE CASCADE,
    CONSTRAINT fk_bom_graph_edge_source
        FOREIGN KEY (source_id) REFERENCES bom_entity (id),
    CONSTRAINT fk_bom_graph_edge_target
        FOREIGN KEY (target_id) REFERENCES bom_entity (id)
);

CREATE INDEX idx_bom_graph_edge_graph ON bom_graph_edge (graph_id);
CREATE INDEX idx_bom_graph_edge_source ON bom_graph_edge (source_id);
CREATE INDEX idx_bom_graph_edge_target ON bom_graph_edge (target_id);
CREATE INDEX idx_bom_graph_edge_role ON bom_graph_edge (role);
CREATE INDEX idx_bom_graph_edge_graph_source ON bom_graph_edge (graph_id, source_id);
CREATE INDEX idx_bom_graph_edge_graph_target ON bom_graph_edge (graph_id, target_id);

CREATE TABLE bom_entity_schema (
    type VARCHAR(255) NOT NULL,
    version VARCHAR(64) NOT NULL,
    definition_doc JSONB NOT NULL,
    usage VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bom_entity_schema PRIMARY KEY (type, version)
);

CREATE TABLE bom_edge_schema (
    source_type VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    target_type VARCHAR(255) NOT NULL,
    properties_policy VARCHAR(32) NOT NULL DEFAULT 'NONE',
    empty_properties_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    properties_schema_type VARCHAR(255),
    properties_schema_version VARCHAR(64),
    cardinality VARCHAR(32) NOT NULL DEFAULT 'UNSPECIFIED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bom_edge_schema PRIMARY KEY (source_type, role, target_type)
);

CREATE TABLE bom_seed_ledger (
    seed_key VARCHAR(512) NOT NULL,
    last_success_fingerprint VARCHAR(128),
    last_success_at TIMESTAMP,
    last_attempt_fingerprint VARCHAR(128),
    last_attempt_status VARCHAR(32) NOT NULL,
    last_attempt_at TIMESTAMP NOT NULL,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bom_seed_ledger PRIMARY KEY (seed_key)
);

CREATE INDEX idx_bom_seed_ledger_status ON bom_seed_ledger (last_attempt_status);
