package org.poc.objs.sbom.web

import org.poc.objs.sbom.annotations.SbomAnnotationKeys

/**
 * Query-string → annotation filter/defaults for SBOM REST.
 *
 * Springdoc documents free-form `Map` params as `additionalProp1`…; those must not become
 * match-all annotation filters (they yield empty subgraphs in Swagger Try-it-out).
 */
object SbomQueryAnnotations {
    private val reservedKeys = setOf(
        "appId",
        "version",
        SbomAnnotationKeys.APP,
        SbomAnnotationKeys.APP_VERSION,
    )

    private val swaggerPlaceholder = Regex("^additionalProp\\d+$")

    fun fromRequestParams(params: Map<String, String>): Map<String, String> =
        params.filter { (key, value) ->
            key.isNotBlank() &&
                value.isNotBlank() &&
                key !in reservedKeys &&
                !swaggerPlaceholder.matches(key)
        }
}
