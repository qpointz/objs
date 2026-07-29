-- Registry catalog tables for schema and allowed-edge-rule persistence (C-3).
-- H2 (MODE=PostgreSQL) compatible; production uses PostgreSQL with JSONB.

CREATE TABLE bom_schema_catalog (
    type           VARCHAR(255)  NOT NULL,
    version        VARCHAR(64)   NOT NULL,
    schema_doc     JSON          NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bom_schema_catalog PRIMARY KEY (type, version)
);

CREATE TABLE bom_allowed_edge (
    source_type              VARCHAR(255)  NOT NULL,
    role                     VARCHAR(255)  NOT NULL,
    target_type              VARCHAR(255)  NOT NULL,
    properties_policy        VARCHAR(32)   NOT NULL DEFAULT 'NONE',
    empty_properties_allowed BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bom_allowed_edge PRIMARY KEY (source_type, role, target_type)
);
