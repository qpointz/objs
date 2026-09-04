package org.poc.objs.policy.drools

/**
 * Drools [org.kie.api.builder.Message] text often appends generated Java source dumps
 * ("Java source of … in error:") that drown the useful diagnostic. Strip those for UI/API.
 */
internal fun sanitizeDroolsMessage(raw: String, maxLen: Int = 480): String {
    var s = raw.trim()
    for (marker in CUT_MARKERS) {
        val i = s.indexOf(marker)
        if (i >= 0) {
            s = s.substring(0, i).trimEnd(' ', ';', '\n', '\r', '\t')
        }
    }
    // Drop trailing stack-frame lines if any leaked through.
    val firstStack = Regex("""\R\s+at\s+\S+""").find(s)
    if (firstStack != null) {
        s = s.substring(0, firstStack.range.first).trimEnd()
    }
    if (s.length > maxLen) {
        s = s.take(maxLen - 1).trimEnd() + "…"
    }
    return s.ifBlank { "Drools error" }
}

private val CUT_MARKERS = listOf(
    "Java source of ",
    "\nJava source of ",
    "\r\nJava source of ",
)
