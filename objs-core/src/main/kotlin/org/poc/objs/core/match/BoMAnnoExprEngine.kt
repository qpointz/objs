package org.poc.objs.core.match

import org.apache.commons.jexl3.JexlBuilder
import org.apache.commons.jexl3.JexlEngine
import org.apache.commons.jexl3.JexlFeatures
import org.apache.commons.jexl3.introspection.JexlPermissions

/**
 * Shared, immutable, default-deny JEXL engine backing both `obj-expr` ([BoMObjExprCompile]) and
 * `graph-expr` ([BoMGraphExprCompile]) compilation.
 */
object BoMAnnoExprEngine {
    const val MAX_EXPRESSION_LENGTH = 4 * 1024
    const val CACHE_SIZE = 256

    val engine: JexlEngine = JexlBuilder()
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
