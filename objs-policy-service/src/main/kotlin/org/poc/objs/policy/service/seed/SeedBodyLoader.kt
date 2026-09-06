package org.poc.objs.policy.service.seed

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource

/**
 * Resolves `bodyRef` / `bodyFile` seed references (G-P37seed) to file content. Reference
 * strings use a `classpath:` or `file:` prefix (Spring resource conventions); [SpringSeedBodyLoader]
 * is the default Spring-backed implementation. Tests can supply a simple fake.
 */
interface SeedBodyLoader {
    fun load(reference: String): String
}

/** Default [SeedBodyLoader] using [ClassPathResource] / [FileSystemResource]. */
class SpringSeedBodyLoader : SeedBodyLoader {
    override fun load(reference: String): String {
        val resource = resolve(reference)
        require(resource.exists()) { "Seed body resource not found: $reference" }
        return resource.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private fun resolve(reference: String): Resource = when {
        reference.startsWith(CLASSPATH_PREFIX) ->
            ClassPathResource(reference.removePrefix(CLASSPATH_PREFIX))
        reference.startsWith(FILE_PREFIX) ->
            FileSystemResource(reference.removePrefix(FILE_PREFIX))
        else -> throw IllegalArgumentException(
            "bodyRef/bodyFile must start with '$CLASSPATH_PREFIX' or '$FILE_PREFIX' (got: '$reference')",
        )
    }

    companion object {
        const val CLASSPATH_PREFIX: String = "classpath:"
        const val FILE_PREFIX: String = "file:"
    }
}
