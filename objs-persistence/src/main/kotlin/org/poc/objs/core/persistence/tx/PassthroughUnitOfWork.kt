package org.poc.objs.core.persistence.tx

import jakarta.persistence.EntityManager
import java.sql.Connection

/** No-op UoW for in-memory seed apply and unit tests that never touch JPA. */
class PassthroughUnitOfWork : UnitOfWork {
    override fun <T> read(block: () -> T): T = block()
    override fun <T> write(block: () -> T): T = block()
    override fun <T> writeNew(block: () -> T): T = block()
    override fun afterRollback(action: () -> Unit) {}
    override fun isActive(): Boolean = true
    override fun connection(): Connection = error("PassthroughUnitOfWork has no JDBC connection")
    override fun entityManager(): EntityManager = error("PassthroughUnitOfWork has no EntityManager")
}
