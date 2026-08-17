-- Current SBOM inventory schema (end state of former Java V3–V8). Greenfield only.

CREATE TABLE sbom_application (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2048),
    tags TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_sbom_application_name UNIQUE (name)
);

CREATE INDEX idx_sbom_application_name ON sbom_application (name);

CREATE TABLE sbom_application_version (
    id UUID NOT NULL PRIMARY KEY,
    application_id UUID NOT NULL,
    label VARCHAR(255),
    captured_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'RELEASED',
    version VARCHAR(255) NOT NULL,
    version_serial NUMERIC(40, 16) NOT NULL DEFAULT -1,
    promoted_at TIMESTAMPTZ,
    tags TEXT[] NOT NULL DEFAULT '{}',
    based_on_version_id UUID,
    based_on_fingerprint_id UUID,
    CONSTRAINT fk_sbom_application_version_app
        FOREIGN KEY (application_id) REFERENCES sbom_application (id) ON DELETE CASCADE,
    CONSTRAINT fk_sbom_application_version_based_on_version
        FOREIGN KEY (based_on_version_id) REFERENCES sbom_application_version (id) ON DELETE CASCADE
);

CREATE INDEX idx_sbom_application_version_latest
    ON sbom_application_version (application_id, captured_at DESC, id DESC);

CREATE UNIQUE INDEX uq_sbom_application_version_ident
    ON sbom_application_version (application_id, version);

CREATE INDEX idx_sbom_application_version_serial
    ON sbom_application_version (application_id, status, version_serial DESC);

CREATE TABLE sbom_application_sbom (
    id UUID NOT NULL PRIMARY KEY,
    version_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2048),
    tags TEXT[] NOT NULL DEFAULT '{}',
    graph_id UUID NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_sbom_application_sbom_graph UNIQUE (graph_id),
    CONSTRAINT uq_sbom_application_sbom_name UNIQUE (version_id, name),
    CONSTRAINT fk_sbom_application_sbom_version
        FOREIGN KEY (version_id) REFERENCES sbom_application_version (id) ON DELETE CASCADE,
    CONSTRAINT fk_sbom_application_sbom_graph
        FOREIGN KEY (graph_id) REFERENCES bom_graph (id)
);

CREATE INDEX idx_sbom_application_sbom_version ON sbom_application_sbom (version_id, sort_order);

CREATE TABLE sbom_application_fingerprint (
    id UUID NOT NULL PRIMARY KEY,
    version_id UUID NOT NULL,
    graph_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(32) NOT NULL,
    content_sha256 VARCHAR(64) NOT NULL,
    CONSTRAINT uq_sbom_application_fingerprint_graph UNIQUE (graph_id),
    CONSTRAINT fk_sbom_application_fingerprint_version
        FOREIGN KEY (version_id) REFERENCES sbom_application_version (id) ON DELETE CASCADE,
    CONSTRAINT fk_sbom_application_fingerprint_graph
        FOREIGN KEY (graph_id) REFERENCES bom_graph (id),
    CONSTRAINT ck_sbom_application_fingerprint_category
        CHECK (category IN ('approval', 'history', 'unknown'))
);

CREATE INDEX idx_sbom_application_fingerprint_version
    ON sbom_application_fingerprint (version_id, created_at DESC);

ALTER TABLE sbom_application_version
    ADD CONSTRAINT fk_sbom_application_version_based_on_fingerprint
        FOREIGN KEY (based_on_fingerprint_id) REFERENCES sbom_application_fingerprint (id) ON DELETE CASCADE;

CREATE TABLE sbom_portfolio (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2048),
    uniqueness VARCHAR(32) NOT NULL DEFAULT 'UNIQUE_APP',
    origin VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    source VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_sbom_portfolio_name UNIQUE (name)
);

CREATE TABLE sbom_portfolio_node (
    id UUID NOT NULL PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    parent_id UUID,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2048),
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_sbom_portfolio_node_portfolio
        FOREIGN KEY (portfolio_id) REFERENCES sbom_portfolio (id) ON DELETE CASCADE,
    CONSTRAINT fk_sbom_portfolio_node_parent
        FOREIGN KEY (parent_id) REFERENCES sbom_portfolio_node (id) ON DELETE CASCADE
);

CREATE INDEX idx_sbom_portfolio_node_portfolio ON sbom_portfolio_node (portfolio_id, parent_id);

CREATE TABLE sbom_portfolio_membership (
    id UUID NOT NULL PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    node_id UUID,
    application_id UUID NOT NULL,
    version_id UUID,
    CONSTRAINT fk_sbom_portfolio_membership_portfolio
        FOREIGN KEY (portfolio_id) REFERENCES sbom_portfolio (id) ON DELETE CASCADE,
    CONSTRAINT fk_sbom_portfolio_membership_node
        FOREIGN KEY (node_id) REFERENCES sbom_portfolio_node (id) ON DELETE CASCADE,
    CONSTRAINT fk_sbom_portfolio_membership_app
        FOREIGN KEY (application_id) REFERENCES sbom_application (id) ON DELETE CASCADE,
    CONSTRAINT fk_sbom_portfolio_membership_version
        FOREIGN KEY (version_id) REFERENCES sbom_application_version (id) ON DELETE SET NULL
);

CREATE INDEX idx_sbom_portfolio_membership_node ON sbom_portfolio_membership (node_id);
