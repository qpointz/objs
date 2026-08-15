package org.poc.objs.sbom.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SbomQueryAnnotationsTest {

    @Test
    fun shouldDropSwaggerPlaceholdersAndReservedKeys() {
        val cleaned = SbomQueryAnnotations.fromRequestParams(
            mapOf(
                "appId" to "payments-api",
                "version" to "2.3.1",
                "app" to "ignore-me",
                "appVersion" to "ignore-me",
                "additionalProp1" to "string",
                "additionalProp2" to "string",
                "source" to "manual",
                "origin" to "ui",
                "blank" to "  ",
            ),
        )
        assertThat(cleaned).containsExactlyInAnyOrderEntriesOf(
            mapOf("source" to "manual", "origin" to "ui"),
        )
    }
}
