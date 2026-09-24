-- C-40: align freeze entity pin table name with live objs_graph_entity / edge pin pair.

ALTER TABLE objs_graph_version_member RENAME TO objs_graph_version_entity;
ALTER INDEX idx_objs_graph_version_member_entity_id RENAME TO idx_objs_graph_version_entity_entity_id;
