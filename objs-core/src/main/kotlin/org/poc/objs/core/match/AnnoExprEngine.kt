package org.poc.objs.core.match

import org.apache.commons.jexl3.JexlArithmetic
import org.apache.commons.jexl3.JexlBuilder
import org.apache.commons.jexl3.JexlEngine
import org.apache.commons.jexl3.JexlFeatures
import org.apache.commons.jexl3.introspection.JexlPermissions
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Shared, immutable, default-deny JEXL engine backing both `obj-expr` ([ObjExprCompile]) and
 * `graph-expr` ([GraphExprCompile]) compilation.
 *
 * `=~` on strings uses regex **find** (substring), not Java [String.matches] (whole string).
 * Anchor with `^` / `$` for a full match, e.g. `p.name =~ '^Apache$'`.
 */
object AnnoExprEngine {
    const val MAX_EXPRESSION_LENGTH = 4 * 1024
    const val CACHE_SIZE = 256

    val engine: JexlEngine = JexlBuilder()
        .arithmetic(JexlArithmetic())
        .features(
            JexlFeatures()
                .script(false)
                .methodCall(false)
                .lambda(false)
                .loops(false)
                .sideEffect(false)
                .sideEffectGlobal(false)
                .newInstance(false)
                .annotation(false)
                .pragma(false)
                .namespacePragma(false)
                .namespaceIdentifier(false)
                .localVar(false)
                .register(false),
        )
        .permissions(JexlPermissions.RESTRICTED)
        .cache(CACHE_SIZE)
        .strict(true)
        .silent(false)
        .create()
}

/** JEXL `left =~ right` is [JexlArithmetic.contains](right, left). */
private class JexlArithmetic : JexlArithmetic(true) {
    override fun contains(container: Any?, value: Any?): Boolean? {
        if (value == null && container == null) return true
        if (value == null || container == null) return false
        if (container is Pattern) {
            return container.matcher(value.toString()).find()
        }
        if (container is CharSequence) {
            return try {
                Pattern.compile(container.toString()).matcher(value.toString()).find()
            } catch (_: PatternSyntaxException) {
                throw IllegalArgumentException("Invalid regular expression: $container")
            }
        }
        return super.contains(container, value)
    }
}
