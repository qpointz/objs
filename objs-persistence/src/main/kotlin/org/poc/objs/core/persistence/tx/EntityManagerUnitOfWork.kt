package org.poc.objs.core.persistence.tx

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.EntityTransaction
import org.hibernate.engine.spi.SessionImplementor
import java.sql.Connection

/**
 * EM-backed [UnitOfWork]: join the active context when present; otherwise open a short-lived EM
 * from [emf]. [writeNew] always uses a separate EM/TX (REQUIRES_NEW analogue).
 */
class EntityManagerUnitOfWork(
    private val emf: EntityManagerFactory,
) : UnitOfWork {

    private val stack = ThreadLocal.withInitial { ArrayDeque<Frame>() }

    override fun isActive(): Boolean = stack.get().isNotEmpty()

    override fun entityManager(): EntityManager {
        val frame = stack.get().lastOrNull()
            ?: error("No active UnitOfWork — call read/write/writeNew first")
        return frame.em
    }

    override fun connection(): Connection {
        val session = entityManager().unwrap(SessionImplementor::class.java)
        return session.jdbcCoordinator.logicalConnection.physicalConnection
    }

    override fun afterRollback(action: () -> Unit) {
        val frame = stack.get().lastOrNull()
            ?: error("afterRollback requires an active UnitOfWork")
        frame.afterRollback += action
    }

    override fun <T> read(block: () -> T): T = withContext(join = true, readOnly = true, block)

    override fun <T> write(block: () -> T): T = withContext(join = true, readOnly = false, block)

    override fun <T> writeNew(block: () -> T): T = withContext(join = false, readOnly = false, block)

    private fun <T> withContext(join: Boolean, readOnly: Boolean, block: () -> T): T {
        if (join && isActive()) {
            return block()
        }
        val em = emf.createEntityManager()
        val tx: EntityTransaction = em.transaction
        val frame = Frame(em = em, readOnly = readOnly)
        stack.get().addLast(frame)
        try {
            tx.begin()
            val result = block()
            if (readOnly) {
                if (tx.isActive) tx.rollback()
            } else {
                tx.commit()
            }
            return result
        } catch (ex: Exception) {
            if (tx.isActive) {
                try {
                    tx.rollback()
                } catch (_: Exception) {
                    // keep original
                }
            }
            frame.afterRollback.toList().forEach { action ->
                try {
                    action()
                } catch (_: Exception) {
                    // best-effort rehydrate
                }
            }
            throw ex
        } finally {
            stack.get().removeLast()
            if (stack.get().isEmpty()) {
                stack.remove()
            }
            if (em.isOpen) {
                em.close()
            }
        }
    }

    private class Frame(
        val em: EntityManager,
        val readOnly: Boolean,
        val afterRollback: MutableList<() -> Unit> = mutableListOf(),
    )
}
