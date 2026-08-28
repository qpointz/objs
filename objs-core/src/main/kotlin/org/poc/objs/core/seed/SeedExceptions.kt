package org.poc.objs.core.seed

import org.poc.objs.core.validation.ValidationIssue

class SeedDocumentParseException @JvmOverloads constructor(
    val documentIndex: Int,
    message: String,
    cause: Throwable? = null,
) : RuntimeException("document[$documentIndex]: $message", cause)

class SeedDocumentValidationException(
    val documentIndex: Int,
    val issues: List<ValidationIssue>,
    message: String,
) : RuntimeException("document[$documentIndex]: $message")

fun requireText(
    map: Map<String, Any?>,
    key: String,
    documentIndex: Int,
    path: String = key,
): String {
    val value = map[key]?.toString()?.trim()
    if (value.isNullOrEmpty()) {
        throw SeedDocumentParseException(documentIndex, "$path must be a non-blank string")
    }
    return value
}
