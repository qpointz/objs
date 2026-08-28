package org.poc.objs.core.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface SchemaCatalogRepository :
    JpaRepository<SchemaCatalogRecord, SchemaCatalogId>

interface AllowedEdgeRuleRepository :
    JpaRepository<AllowedEdgeRuleRecord, AllowedEdgeRuleId>

interface SeedLedgerRepository :
    JpaRepository<SeedLedgerRecord, String>
