package org.poc.objs.sbom.persistence

import org.poc.objs.api.GraphOperationException
import org.poc.objs.api.store.GraphVersionReferenceGuard
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Fail closed when SBOM rows still reference a graph (BOM) or graph version (fingerprint)
 * about to be deleted / purged / destroyed.
 */
@Primary
@Component
class SbomGraphVersionReferenceGuard(
    private val fingerprints: SbomApplicationFingerprintRepository,
    private val boms: SbomApplicationSbomRepository,
) : GraphVersionReferenceGuard {
    override fun assertRemovable(graphId: UUID, versions: Collection<Long>) {
        if (versions.isEmpty()) {
            val bom = boms.findByGraphId(graphId)
            if (bom != null) {
                throw GraphOperationException(
                    code = "GRAPH_IN_USE",
                    message = "Graph $graphId is referenced by application BOM ${bom.id}",
                )
            }
        }
        val blocking =
            if (versions.isEmpty()) {
                fingerprints.findByGraphId(graphId)
            } else {
                versions.flatMap { fingerprints.findByGraphIdAndGraphVersion(graphId, it) }
            }
        if (blocking.isEmpty()) return
        val sample = blocking.first()
        throw GraphOperationException(
            code = "GRAPH_VERSION_IN_USE",
            message = "Graph $graphId version ${sample.graphVersion} is referenced by fingerprint ${sample.id}",
        )
    }
}
