package org.poc.objs.core.persistence.tx

import jakarta.persistence.EntityManager
import java.sql.Connection

/**
 * Internal transaction / persistence context boundary for objs-core (G-A2).
 * Hidden from public API; Boot adapts via Spring `TransactionTemplate` (G-A3).
 */
interface UnitOfWork {
    fun <T> read(block: () -> T): T
    fun <T> write(block: () -> T): T
    fun <T> writeNew(block: () -> T): T
    fun afterRollback(action: () -> Unit)
    fun isActive(): Boolean
    fun connection(): Connection
    fun entityManager(): EntityManager
}
