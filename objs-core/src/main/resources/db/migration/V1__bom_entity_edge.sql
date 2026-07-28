-- Entities and edges: generic columns + JSON (H2-compatible; PostgreSQL runtime uses JSON/JSONB via dialect)
CREATE TABLE bom_entity (
    id UUID NOT NULL PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    schema_version VARCHAR(64) NOT NULL,
    payload JSON NOT NULL,
    annotations JSON NOT NULL
);

CREATE TABLE bom_edge (
    id UUID NOT NULL PRIMARY KEY,
    source_id UUID NOT NULL,
    target_id UUID NOT NULL,
    role VARCHAR(255) NOT NULL,
    type VARCHAR(255),
    schema_version VARCHAR(64),
    properties JSON,
    CONSTRAINT fk_bom_edge_source FOREIGN KEY (source_id) REFERENCES bom_entity (id),
    CONSTRAINT fk_bom_edge_target FOREIGN KEY (target_id) REFERENCES bom_entity (id)
);

CREATE INDEX idx_bom_entity_type_schema_version ON bom_entity (type, schema_version);
CREATE INDEX idx_bom_edge_source ON bom_edge (source_id);
CREATE INDEX idx_bom_edge_target ON bom_edge (target_id);
CREATE INDEX idx_bom_edge_role ON bom_edge (role);
