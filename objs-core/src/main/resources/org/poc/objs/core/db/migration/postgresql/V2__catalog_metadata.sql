ALTER TABLE bom_entity_schema
    ADD COLUMN tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN attributes JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE bom_edge_schema
    ADD COLUMN description TEXT,
    ADD COLUMN source_verb VARCHAR(255),
    ADD COLUMN target_verb VARCHAR(255),
    ADD COLUMN tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN attributes JSONB NOT NULL DEFAULT '{}'::jsonb;
