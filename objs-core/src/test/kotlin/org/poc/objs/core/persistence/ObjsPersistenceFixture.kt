package org.poc.objs.core.persistence

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/** Opens a unique H2 objs store for each test method. */
abstract class ObjsPersistenceFixture {
    protected lateinit var db: ObjsPersistenceTestSupport

    @BeforeEach
    fun openPersistence() {
        db = ObjsPersistenceTestSupport.h2()
    }

    @AfterEach
    fun closePersistence() {
        if (::db.isInitialized) db.close()
    }

    protected val store get() = db.graphStore
    protected val graphStore get() = db.graphStore
    protected val namedGraphs get() = db.namedGraphs
    protected val schemas get() = db.schemaCatalog
    protected val allowed get() = db.edgeCatalog
    protected val rules get() = db.edgeCatalog
    protected val graphDao get() = db.graphDao
    protected val graphs get() = db.graphDao
    protected val graphRepository get() = db.graphDao
    protected val entityDao get() = db.entityDao
    protected val entityRepository get() = db.entityDao
    protected val entities get() = db.entityDao
    protected val edgeDao get() = db.edgeDao
    protected val edges get() = db.edgeDao
    protected val membershipDao get() = db.membershipDao
    protected val memberships get() = db.membershipDao
    protected val membershipRepository get() = db.membershipDao
    protected val poolReader get() = db.poolReader
    protected val schemaCatalog get() = db.schemaCatalog
    protected val edgeCatalog get() = db.edgeCatalog
    protected val schemaRepo get() = db.schemaCatalogDao
    protected val edgeRuleRepo get() = db.allowedEdgeRuleDao
    protected val importer get() = db.seedImporter
    protected val ledger get() = db.seedLedger
    protected val ledgerRepo get() = db.seedLedgerDao
    protected val uow get() = db.uow
}
