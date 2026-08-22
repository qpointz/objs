-- Reverse lookup: graphs that pin an entity version (C-19).

CREATE INDEX idx_bom_graph_version_member_entity_id ON bom_graph_version_member (entity_id);
