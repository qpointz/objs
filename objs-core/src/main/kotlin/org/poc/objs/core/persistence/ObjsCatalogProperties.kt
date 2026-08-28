package org.poc.objs.core.persistence

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Catalog read-cache settings for [JpaSchemaCatalog] / [JpaAllowedEdgeCatalog].
 *
 * @property cacheTtl Max age of a hydrated snapshot before the next read reloads from the store.
 *   [Duration.ZERO] disables TTL expiry (startup hydrate + write-through + explicit refresh only).
 */
@ConfigurationProperties(prefix = "objs.catalogs")
data class ObjsCatalogProperties(
    var cacheTtl: Duration = Duration.ofSeconds(30),
)
