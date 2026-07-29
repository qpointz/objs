package org.poc.objs.core.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface BoMSchemaCatalogRepository :
    JpaRepository<BoMSchemaCatalogRecord, BoMSchemaCatalogId>

interface BoMAllowedEdgeRuleRepository :
    JpaRepository<BoMAllowedEdgeRuleRecord, BoMAllowedEdgeRuleId>
