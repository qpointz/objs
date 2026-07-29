-- Entities and edges: generic columns + JSON (H2-compatible; PostgreSQL runtime uses JSON/JSONB via dialect)
CREATE TABLE bom_graph_entity (
    id UUID NOT NULL PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    schema_version VARCHAR(64) NOT NULL,
    payload JSON NOT NULL,
    annotations JSON NOT NULL
);

CREATE TABLE bom_graph_edge (
    id UUID NOT NULL PRIMARY KEY,
    source_id UUID NOT NULL,
    target_id UUID NOT NULL,
    role VARCHAR(255) NOT NULL,
    type VARCHAR(255),
    schema_version VARCHAR(64),
    properties JSON,
    CONSTRAINT fk_bom_graph_edge_source FOREIGN KEY (source_id) REFERENCES bom_graph_entity (id),
    CONSTRAINT fk_bom_graph_edge_target FOREIGN KEY (target_id) REFERENCES bom_graph_entity (id)
);

CREATE INDEX idx_bom_graph_entity_type_schema_version ON bom_graph_entity (type, schema_version);
CREATE INDEX idx_bom_graph_edge_source ON bom_graph_edge (source_id);
CREATE INDEX idx_bom_graph_edge_target ON bom_graph_edge (target_id);
CREATE INDEX idx_bom_graph_edge_role ON bom_graph_edge (role);

-- Authoritative entity/property schema definitions.
CREATE TABLE bom_graph_entity_schema (
    type           VARCHAR(255)  NOT NULL,
    version        VARCHAR(64)   NOT NULL,
    definition_doc JSON          NOT NULL,
    usages         JSON          NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bom_graph_entity_schema PRIMARY KEY (type, version)
);

-- Directed edge allow-list and edge-properties policy.
CREATE TABLE bom_graph_edge_schema (
    source_type              VARCHAR(255)  NOT NULL,
    role                     VARCHAR(255)  NOT NULL,
    target_type              VARCHAR(255)  NOT NULL,
    properties_policy        VARCHAR(32)   NOT NULL DEFAULT 'NONE',
    empty_properties_allowed BOOLEAN       NOT NULL DEFAULT TRUE,
    properties_schema_type   VARCHAR(255),
    properties_schema_version VARCHAR(64),
    created_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bom_graph_edge_schema PRIMARY KEY (source_type, role, target_type)
);
