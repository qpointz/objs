package org.poc.objs.core.seed

import tools.jackson.databind.DeserializationFeature
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.kotlinModule
import java.io.InputStream
import java.io.Reader

/** Shared Jackson 3 YAML mapper for seed parse/serialize. */
object SeedYaml {
    val mapper: YAMLMapper = YAMLMapper.builder()
        .addModule(kotlinModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    @Suppress("UNCHECKED_CAST")
    fun parseDocuments(yaml: String): List<SeedRawDocument> {
        if (yaml.isBlank()) return emptyList()
        val parts = yaml.split(Regex("(?m)^---\\s*$"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val docs = mutableListOf<SeedRawDocument>()
        parts.forEachIndexed { index, part ->
            val raw = mapper.readValue(part, MutableMap::class.java) as Map<String, Any?>
            docs += toRawDocument(index, raw)
        }
        return docs
    }

    fun parseDocuments(stream: InputStream): List<SeedRawDocument> =
        parseDocuments(stream.reader().use(Reader::readText))

    fun writeDocuments(documents: List<Map<String, Any?>>): String {
        if (documents.isEmpty()) return ""
        return documents.joinToString("\n---\n") { doc ->
            mapper.writeValueAsString(doc).trimEnd()
        } + "\n"
    }

    @Suppress("UNCHECKED_CAST")
    private fun toRawDocument(index: Int, raw: Map<String, Any?>): SeedRawDocument {
        val metadata = (raw["metadata"] as? Map<*, *>)?.entries
            ?.associate { it.key.toString() to it.value } ?: emptyMap()
        val spec = (raw["spec"] as? Map<*, *>)?.entries
            ?.associate { it.key.toString() to it.value } ?: emptyMap()
        return SeedRawDocument(
            index = index,
            apiVersion = raw["apiVersion"]?.toString(),
            kind = raw["kind"]?.toString(),
            metadata = metadata,
            spec = spec,
            raw = raw,
        )
    }
}
