-- Entities and edges: generic columns + JSON (H2-compatible; PostgreSQL runtime uses JSON/JSONB via dialect)
CREATE TABLE bo_entity (
    id UUID NOT NULL PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    version VARCHAR(64) NOT NULL,
    payload JSON NOT NULL,
    annotations JSON NOT NULL
);

CREATE TABLE bo_edge (
    id UUID NOT NULL PRIMARY KEY,
    source_id UUID NOT NULL,
    target_id UUID NOT NULL,
    role VARCHAR(255) NOT NULL,
    type VARCHAR(255),
    version VARCHAR(64),
    properties JSON,
    CONSTRAINT fk_bo_edge_source FOREIGN KEY (source_id) REFERENCES bo_entity (id),
    CONSTRAINT fk_bo_edge_target FOREIGN KEY (target_id) REFERENCES bo_entity (id)
);

CREATE INDEX idx_bo_entity_type_version ON bo_entity (type, version);
CREATE INDEX idx_bo_edge_source ON bo_edge (source_id);
CREATE INDEX idx_bo_edge_target ON bo_edge (target_id);
CREATE INDEX idx_bo_edge_role ON bo_edge (role);
