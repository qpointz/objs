package org.poc.objs.policy.drools

import org.kie.api.builder.Message

/**
 * One Drools builder diagnostic. [line] / [column] come from [Message.getLine] /
 * [Message.getColumn] when positive (1-based); never parsed from [message] text.
 */
data class DroolsCompileIssue(
    val message: String,
    val line: Int? = null,
    val column: Int? = null,
    val path: String? = null,
) {
    fun display(): String =
        when {
            line != null && column != null -> "line $line:$column: $message"
            line != null -> "line $line: $message"
            else -> message
        }
}

internal fun Message.toCompileIssue(): DroolsCompileIssue =
    DroolsCompileIssue(
        message = sanitizeDroolsMessage(text),
        line = line.takeIf { it > 0 },
        column = column.takeIf { it > 0 },
        path = path?.takeIf { it.isNotBlank() },
    )
