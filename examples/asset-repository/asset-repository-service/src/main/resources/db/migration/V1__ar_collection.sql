-- Asset repository collection metadata (hybrid persistence; objects live in objs).
CREATE TABLE ar_collection (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner VARCHAR(255) NOT NULL,
    owner_email VARCHAR(255),
    support_email VARCHAR(255),
    sla TEXT,
    object_write_mode VARCHAR(32) NOT NULL,
    graph_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_ar_collection_graph_id UNIQUE (graph_id)
);

CREATE INDEX idx_ar_collection_owner ON ar_collection (owner);
CREATE INDEX idx_ar_collection_name ON ar_collection (name);

-- Accepted object types per collection (1-*) with collection-level type metadata.
CREATE TABLE ar_collection_type (
    id UUID NOT NULL PRIMARY KEY,
    collection_id UUID NOT NULL,
    object_type VARCHAR(255) NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ar_collection_type_collection
        FOREIGN KEY (collection_id) REFERENCES ar_collection (id) ON DELETE CASCADE,
    CONSTRAINT uq_ar_collection_type UNIQUE (collection_id, object_type)
);

CREATE INDEX idx_ar_collection_type_collection ON ar_collection_type (collection_id);
CREATE INDEX idx_ar_collection_type_object_type ON ar_collection_type (object_type);
