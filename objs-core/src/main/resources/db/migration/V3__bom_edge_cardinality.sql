-- Allowed-edge cardinality metadata (UNSPECIFIED / 1:1 / 1:*).
ALTER TABLE bom_graph_edge_schema
    ADD COLUMN cardinality VARCHAR(32) NOT NULL DEFAULT 'UNSPECIFIED';
