package org.poc.objs.core.persistence.policy

import org.poc.objs.core.persistence.tx.UnitOfWork
import java.util.UUID

class PolicyEvaluationDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()

    fun findById(id: UUID): PolicyEvaluationRecord? =
        em.find(PolicyEvaluationRecord::class.java, id)

    fun save(record: PolicyEvaluationRecord): PolicyEvaluationRecord =
        if (em.find(PolicyEvaluationRecord::class.java, record.evaluationId) == null) {
            em.persist(record)
            record
        } else {
            em.merge(record)
        }

    fun deleteById(id: UUID) {
        findById(id)?.let { em.remove(it) }
    }

    /**
     * Ordered evaluation rows for archive list. Optional [kind] filter in JPQL.
     * Optional [tag] containment is applied in-memory (JSON tags; same pattern as policy query).
     */
    fun list(
        limit: Int,
        offset: Int,
        kind: String? = null,
        tag: String? = null,
    ): List<PolicyEvaluationRecord> {
        val jpql =
            buildString {
                append("select e from PolicyEvaluationRecord e")
                if (kind != null) append(" where e.kind = :kind")
                append(" order by e.evaluatedAt desc, e.evaluationId desc")
            }
        val query = em.createQuery(jpql, PolicyEvaluationRecord::class.java)
        if (kind != null) {
            query.setParameter("kind", kind)
        }
        if (tag == null) {
            query.firstResult = offset.coerceAtLeast(0)
            query.maxResults = limit
            return query.resultList
        }
        val filtered = query.resultList.filter { tag in it.tags }
        val from = offset.coerceAtLeast(0)
        if (from >= filtered.size) return emptyList()
        return filtered.drop(from).take(limit)
    }
}

class PolicyOutcomeDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()

    fun deleteByEvaluationId(evaluationId: UUID) {
        em.createQuery("delete from PolicyOutcomeRecord o where o.evaluationId = :id")
            .setParameter("id", evaluationId)
            .executeUpdate()
    }

    fun save(record: PolicyOutcomeRecord): PolicyOutcomeRecord {
        em.persist(record)
        return record
    }

    fun findByEvaluationId(evaluationId: UUID): List<PolicyOutcomeRecord> =
        em.createQuery(
            "select o from PolicyOutcomeRecord o where o.evaluationId = :id order by o.ordinal asc",
            PolicyOutcomeRecord::class.java,
        ).setParameter("id", evaluationId).resultList
}

class PolicyFindingDao(private val uow: UnitOfWork) {
    private val em get() = uow.entityManager()

    fun deleteByOutcomeIds(outcomeIds: Collection<UUID>) {
        if (outcomeIds.isEmpty()) return
        em.createQuery("delete from PolicyFindingRecord f where f.outcomeId in :ids")
            .setParameter("ids", outcomeIds)
            .executeUpdate()
    }

    fun save(record: PolicyFindingRecord): PolicyFindingRecord {
        em.persist(record)
        return record
    }

    fun findByOutcomeIds(outcomeIds: Collection<UUID>): List<PolicyFindingRecord> {
        if (outcomeIds.isEmpty()) return emptyList()
        return em.createQuery(
            "select f from PolicyFindingRecord f where f.outcomeId in :ids order by f.idx asc",
            PolicyFindingRecord::class.java,
        ).setParameter("ids", outcomeIds).resultList
    }
}
