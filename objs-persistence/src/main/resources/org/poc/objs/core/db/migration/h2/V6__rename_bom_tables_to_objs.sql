-- Move the Objs persistence tables into the objs_* namespace.
-- This forward migration preserves existing installations and upgrades greenfield schemas
-- created by V1-V5.

ALTER TABLE bom_entity RENAME TO objs_entity;
ALTER TABLE bom_graph RENAME TO objs_graph;
ALTER TABLE bom_graph_entity RENAME TO objs_graph_entity;
ALTER TABLE bom_graph_edge RENAME TO objs_graph_edge;
ALTER TABLE bom_entity_schema RENAME TO objs_entity_schema;
ALTER TABLE bom_edge_schema RENAME TO objs_edge_schema;
ALTER TABLE bom_seed_ledger RENAME TO objs_seed_ledger;
ALTER TABLE bom_entity_version RENAME TO objs_entity_version;
ALTER TABLE bom_graph_version RENAME TO objs_graph_version;
ALTER TABLE bom_graph_edge_version RENAME TO objs_graph_edge_version;
ALTER TABLE bom_graph_version_member RENAME TO objs_graph_version_member;
ALTER TABLE bom_graph_version_edge RENAME TO objs_graph_version_edge;

ALTER INDEX idx_bom_entity_type_schema_version RENAME TO idx_objs_entity_type_schema_version;
ALTER INDEX idx_bom_graph_entity_entity RENAME TO idx_objs_graph_entity_entity;
ALTER INDEX idx_bom_graph_edge_graph RENAME TO idx_objs_graph_edge_graph;
ALTER INDEX idx_bom_graph_edge_source RENAME TO idx_objs_graph_edge_source;
ALTER INDEX idx_bom_graph_edge_target RENAME TO idx_objs_graph_edge_target;
ALTER INDEX idx_bom_graph_edge_role RENAME TO idx_objs_graph_edge_role;
ALTER INDEX idx_bom_graph_edge_graph_source RENAME TO idx_objs_graph_edge_graph_source;
ALTER INDEX idx_bom_graph_edge_graph_target RENAME TO idx_objs_graph_edge_graph_target;
ALTER INDEX idx_bom_seed_ledger_status RENAME TO idx_objs_seed_ledger_status;
ALTER INDEX idx_bom_graph_version_member_entity_id RENAME TO idx_objs_graph_version_member_entity_id;
