package org.poc.objs.autoconfigure

import jakarta.persistence.EntityManager
import org.hibernate.engine.spi.SessionImplementor
import org.poc.objs.core.persistence.tx.UnitOfWork
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Connection

/**
 * Boot [UnitOfWork] adapter (G-A3): drives TX via Spring [TransactionTemplate] and uses the
 * shared Spring [EntityManager]. Nested [read]/[write] join (`PROPAGATION_REQUIRED`);
 * [writeNew] uses `PROPAGATION_REQUIRES_NEW`.
 */
class TransactionTemplateUnitOfWork(
    private val em: EntityManager,
    private val readTemplate: TransactionTemplate,
    private val writeTemplate: TransactionTemplate,
    private val writeNewTemplate: TransactionTemplate,
) : UnitOfWork {

    private val depth = ThreadLocal.withInitial { 0 }

    override fun isActive(): Boolean =
        depth.get() > 0 || TransactionSynchronizationManager.isActualTransactionActive()

    override fun entityManager(): EntityManager {
        check(isActive()) { "No active UnitOfWork — call read/write/writeNew first" }
        return em
    }

    override fun connection(): Connection {
        val session = entityManager().unwrap(SessionImplementor::class.java)
        return session.jdbcCoordinator.logicalConnection.physicalConnection
    }

    override fun afterRollback(action: () -> Unit) {
        check(TransactionSynchronizationManager.isSynchronizationActive()) {
            "afterRollback requires an active Spring transaction"
        }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        action()
                    }
                }
            },
        )
    }

    override fun <T> read(block: () -> T): T = run(readTemplate, block)

    override fun <T> write(block: () -> T): T = run(writeTemplate, block)

    override fun <T> writeNew(block: () -> T): T = run(writeNewTemplate, block)

    private fun <T> run(template: TransactionTemplate, block: () -> T): T {
        if (depth.get() > 0 && template !== writeNewTemplate) {
            return block()
        }
        // TransactionTemplate.execute returns null for Kotlin Unit / Java void — do not use !!.
        @Suppress("UNCHECKED_CAST")
        return template.execute {
            depth.set(depth.get() + 1)
            try {
                block()
            } finally {
                val next = depth.get() - 1
                if (next <= 0) depth.remove() else depth.set(next)
            }
        } as T
    }

    companion object {
        fun create(em: EntityManager, transactionManager: PlatformTransactionManager): TransactionTemplateUnitOfWork {
            fun template(propagation: Int, readOnly: Boolean) =
                TransactionTemplate(transactionManager).apply {
                    propagationBehavior = propagation
                    isReadOnly = readOnly
                }
            return TransactionTemplateUnitOfWork(
                em = em,
                readTemplate = template(TransactionDefinition.PROPAGATION_REQUIRED, readOnly = true),
                writeTemplate = template(TransactionDefinition.PROPAGATION_REQUIRED, readOnly = false),
                writeNewTemplate = template(TransactionDefinition.PROPAGATION_REQUIRES_NEW, readOnly = false),
            )
        }
    }
}
