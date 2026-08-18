ALTER TABLE bom_entity_schema ADD COLUMN tags JSON DEFAULT '[]';
ALTER TABLE bom_entity_schema ADD COLUMN attributes JSON DEFAULT '{}';

ALTER TABLE bom_edge_schema ADD COLUMN description VARCHAR;
ALTER TABLE bom_edge_schema ADD COLUMN source_verb VARCHAR(255);
ALTER TABLE bom_edge_schema ADD COLUMN target_verb VARCHAR(255);
ALTER TABLE bom_edge_schema ADD COLUMN tags JSON DEFAULT '[]';
ALTER TABLE bom_edge_schema ADD COLUMN attributes JSON DEFAULT '{}';
