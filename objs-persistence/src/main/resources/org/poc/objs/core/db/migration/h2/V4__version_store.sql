-- HEAD + history. head_version NULL until first capture. Greenfield only.

CREATE TABLE bom_entity_version (
    entity_id UUID NOT NULL,
    version BIGINT NOT NULL,
    type VARCHAR(255) NOT NULL,
    schema_version VARCHAR(64) NOT NULL,
    payload JSON NOT NULL,
    annotations JSON NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    head_deleted_at TIMESTAMP,
    CONSTRAINT pk_bom_entity_version PRIMARY KEY (entity_id, version)
);

CREATE TABLE bom_graph_version (
    graph_id UUID NOT NULL,
    version BIGINT NOT NULL,
    graph_annotations JSON NOT NULL,
    annotations JSON NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    head_deleted_at TIMESTAMP,
    CONSTRAINT pk_bom_graph_version PRIMARY KEY (graph_id, version)
);

CREATE TABLE bom_graph_edge_version (
    edge_id UUID NOT NULL,
    version BIGINT NOT NULL,
    graph_id UUID NOT NULL,
    source_id UUID NOT NULL,
    target_id UUID NOT NULL,
    role VARCHAR(255) NOT NULL,
    type VARCHAR(255),
    schema_version VARCHAR(64),
    properties JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    head_deleted_at TIMESTAMP,
    CONSTRAINT pk_bom_graph_edge_version PRIMARY KEY (edge_id, version)
);

CREATE TABLE bom_graph_version_member (
    graph_id UUID NOT NULL,
    graph_version BIGINT NOT NULL,
    entity_id UUID NOT NULL,
    entity_version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bom_graph_version_member PRIMARY KEY (graph_id, graph_version, entity_id),
    CONSTRAINT fk_bom_graph_version_member_graph
        FOREIGN KEY (graph_id, graph_version) REFERENCES bom_graph_version (graph_id, version),
    CONSTRAINT fk_bom_graph_version_member_entity
        FOREIGN KEY (entity_id, entity_version) REFERENCES bom_entity_version (entity_id, version)
);

CREATE TABLE bom_graph_version_edge (
    graph_id UUID NOT NULL,
    graph_version BIGINT NOT NULL,
    edge_id UUID NOT NULL,
    edge_version BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bom_graph_version_edge PRIMARY KEY (graph_id, graph_version, edge_id),
    CONSTRAINT fk_bom_graph_version_edge_graph
        FOREIGN KEY (graph_id, graph_version) REFERENCES bom_graph_version (graph_id, version),
    CONSTRAINT fk_bom_graph_version_edge_edge
        FOREIGN KEY (edge_id, edge_version) REFERENCES bom_graph_edge_version (edge_id, version)
);

ALTER TABLE bom_entity ADD COLUMN head_version BIGINT;
ALTER TABLE bom_graph ADD COLUMN head_version BIGINT;
ALTER TABLE bom_graph_edge ADD COLUMN head_version BIGINT;

ALTER TABLE bom_entity ADD CONSTRAINT fk_bom_entity_head_version
    FOREIGN KEY (id, head_version) REFERENCES bom_entity_version (entity_id, version);
ALTER TABLE bom_graph ADD CONSTRAINT fk_bom_graph_head_version
    FOREIGN KEY (id, head_version) REFERENCES bom_graph_version (graph_id, version);
ALTER TABLE bom_graph_edge ADD CONSTRAINT fk_bom_graph_edge_head_version
    FOREIGN KEY (id, head_version) REFERENCES bom_graph_edge_version (edge_id, version);

ALTER TABLE bom_graph_edge DROP CONSTRAINT fk_bom_graph_edge_source;
ALTER TABLE bom_graph_edge DROP CONSTRAINT fk_bom_graph_edge_target;
