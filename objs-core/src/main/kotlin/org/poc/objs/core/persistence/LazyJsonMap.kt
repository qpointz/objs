package org.poc.objs.core.persistence

import org.poc.objs.core.typed.PayloadMapper
import tools.jackson.core.type.TypeReference

/**
 * Mutable map backed by a raw JSON string. Parsing happens once on first access so
 * matchers can inspect only the fields they touch.
 */
class LazyJsonMap<V>(
    private val rawJson: String?,
    private val emptyWhenNull: Boolean = true,
    private val parse: (String) -> MutableMap<String, V>,
) : MutableMap<String, V> {
    private var parsed: MutableMap<String, V>? = null
    private var parseCount: Int = 0

    val wasParsed: Boolean get() = parsed != null
    val parseInvocations: Int get() = parseCount

    fun rawJsonOrNull(): String? = rawJson

    private fun materialize(): MutableMap<String, V> {
        parsed?.let { return it }
        parseCount++
        val next = when {
            rawJson.isNullOrBlank() && emptyWhenNull -> mutableMapOf()
            rawJson.isNullOrBlank() -> error("JSON value is null")
            else -> parse(rawJson)
        }
        parsed = next
        return next
    }

    override val size: Int get() = materialize().size
    override fun isEmpty(): Boolean = materialize().isEmpty()
    override fun containsKey(key: String): Boolean = materialize().containsKey(key)
    override fun containsValue(value: V): Boolean = materialize().containsValue(value)
    override fun get(key: String): V? = materialize()[key]
    override val keys: MutableSet<String> get() = materialize().keys
    override val values: MutableCollection<V> get() = materialize().values
    override val entries: MutableSet<MutableMap.MutableEntry<String, V>> get() = materialize().entries
    override fun clear() {
        materialize().clear()
    }

    override fun put(key: String, value: V): V? = materialize().put(key, value)
    override fun putAll(from: Map<out String, V>) {
        materialize().putAll(from)
    }

    override fun remove(key: String): V? = materialize().remove(key)

    companion object {
        private val stringMapType = object : TypeReference<MutableMap<String, String>>() {}
        private val anyMapType = object : TypeReference<MutableMap<String, Any?>>() {}

        fun annotations(rawJson: String?): LazyJsonMap<String> =
            LazyJsonMap(rawJson) { json ->
                PayloadMapper.mapper.readValue(json, stringMapType)
            }

        fun payload(rawJson: String?): LazyJsonMap<Any?> =
            LazyJsonMap(rawJson) { json ->
                PayloadMapper.mapper.readValue(json, anyMapType)
            }

        fun properties(rawJson: String?): LazyJsonMap<Any?>? =
            if (rawJson == null) null else payload(rawJson)
    }
}
