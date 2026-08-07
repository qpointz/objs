-- Soft-link subgraphs: header + M2M membership (entity/edge ids unchanged)
CREATE TABLE bom_subgraph (
    id UUID NOT NULL PRIMARY KEY,
    annotations JSON NOT NULL
);

CREATE TABLE bom_subgraph_entities (
    subgraph_id UUID NOT NULL,
    entity_id UUID NOT NULL,
    PRIMARY KEY (subgraph_id, entity_id),
    CONSTRAINT fk_bse_subgraph FOREIGN KEY (subgraph_id) REFERENCES bom_subgraph (id) ON DELETE CASCADE,
    CONSTRAINT fk_bse_entity FOREIGN KEY (entity_id) REFERENCES bom_graph_entity (id) ON DELETE CASCADE
);

CREATE TABLE bom_subgraph_edges (
    subgraph_id UUID NOT NULL,
    edge_id UUID NOT NULL,
    PRIMARY KEY (subgraph_id, edge_id),
    CONSTRAINT fk_bsg_subgraph FOREIGN KEY (subgraph_id) REFERENCES bom_subgraph (id) ON DELETE CASCADE,
    CONSTRAINT fk_bsg_edge FOREIGN KEY (edge_id) REFERENCES bom_graph_edge (id) ON DELETE CASCADE
);

CREATE INDEX idx_bse_entity ON bom_subgraph_entities (entity_id);
CREATE INDEX idx_bsg_edge ON bom_subgraph_edges (edge_id);
