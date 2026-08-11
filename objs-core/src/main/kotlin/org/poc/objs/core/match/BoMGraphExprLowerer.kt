package org.poc.objs.core.match

import org.apache.commons.jexl3.JexlExpression
import org.apache.commons.jexl3.internal.ScriptVisitor
import org.apache.commons.jexl3.parser.ASTAndNode
import org.apache.commons.jexl3.parser.ASTArrayAccess
import org.apache.commons.jexl3.parser.ASTEQNode
import org.apache.commons.jexl3.parser.ASTEQSNode
import org.apache.commons.jexl3.parser.ASTIdentifier
import org.apache.commons.jexl3.parser.ASTIdentifierAccess
import org.apache.commons.jexl3.parser.ASTJexlScript
import org.apache.commons.jexl3.parser.ASTNENode
import org.apache.commons.jexl3.parser.ASTNESNode
import org.apache.commons.jexl3.parser.ASTOrNode
import org.apache.commons.jexl3.parser.ASTReference
import org.apache.commons.jexl3.parser.ASTReferenceExpression
import org.apache.commons.jexl3.parser.ASTStringLiteral
import org.apache.commons.jexl3.parser.JexlNode
import java.util.UUID

/**
 * Lowers `graph-expr` AST to [BoMGraphExprPushdown] when the tree is only equality / inequality
 * (`==` / `===` / `!=` / `!==`) of `id` / `a.*` combined with `&&` / `||`.
 * Anything else → null (local eval).
 */
object BoMGraphExprLowerer : ScriptVisitor() {

    fun toPushdown(compiled: JexlExpression): BoMGraphExprPushdown? {
        @Suppress("UNCHECKED_CAST")
        val tree = visitExpression(compiled, null) as? BoolExpr<GraphAtom> ?: return null
        val groups = toDnf(tree).mapNotNull { atoms -> foldAndGroup(atoms) }
        if (groups.isEmpty()) {
            return BoMGraphExprPushdown(dnf = emptyList())
        }
        return BoMGraphExprPushdown(dnf = groups)
    }

    override fun visit(node: ASTJexlScript, data: Any?): Any? {
        if (node.jjtGetNumChildren() != 1) return null
        return node.jjtGetChild(0).jjtAccept(this, data)
    }

    override fun visit(node: ASTReferenceExpression, data: Any?): Any? {
        if (node.jjtGetNumChildren() != 1) return null
        return node.jjtGetChild(0).jjtAccept(this, data)
    }

    override fun visit(node: ASTAndNode, data: Any?): Any? {
        val parts = ArrayList<BoolExpr<GraphAtom>>()
        for (i in 0 until node.jjtGetNumChildren()) {
            val child = node.jjtGetChild(i).jjtAccept(this, data) as? BoolExpr<*> ?: return null
            @Suppress("UNCHECKED_CAST")
            parts.add(child as BoolExpr<GraphAtom>)
        }
        return if (parts.isEmpty()) null else BoolExpr.And(parts)
    }

    override fun visit(node: ASTOrNode, data: Any?): Any? {
        val parts = ArrayList<BoolExpr<GraphAtom>>()
        for (i in 0 until node.jjtGetNumChildren()) {
            val child = node.jjtGetChild(i).jjtAccept(this, data) as? BoolExpr<*> ?: return null
            @Suppress("UNCHECKED_CAST")
            parts.add(child as BoolExpr<GraphAtom>)
        }
        return if (parts.isEmpty()) null else BoolExpr.Or(parts)
    }

    override fun visit(node: ASTEQNode, data: Any?): Any? =
        parseComparison(node, eq = true)?.let { BoolExpr.Atom(it) }

    override fun visit(node: ASTEQSNode, data: Any?): Any? =
        parseComparison(node, eq = true)?.let { BoolExpr.Atom(it) }

    override fun visit(node: ASTNENode, data: Any?): Any? =
        parseComparison(node, eq = false)?.let { BoolExpr.Atom(it) }

    override fun visit(node: ASTNESNode, data: Any?): Any? =
        parseComparison(node, eq = false)?.let { BoolExpr.Atom(it) }

    override fun visitNode(node: JexlNode, data: Any?): Any? = null

    private fun parseComparison(node: JexlNode, eq: Boolean): GraphAtom? {
        if (node.jjtGetNumChildren() != 2) return null
        val left = node.jjtGetChild(0)
        val right = node.jjtGetChild(1)
        val (path, value) = when {
            right is ASTStringLiteral -> pathFrom(left) to right.literal
            left is ASTStringLiteral -> pathFrom(right) to left.literal
            else -> return null
        }
        return atomFromPath(path ?: return null, value, eq)
    }

    private fun pathFrom(node: JexlNode): List<String>? {
        return when (node) {
            is ASTIdentifier -> listOf(node.name)
            is ASTReferenceExpression ->
                if (node.jjtGetNumChildren() == 1) pathFrom(node.jjtGetChild(0)) else null
            is ASTReference -> {
                if (node.jjtGetNumChildren() < 1) return null
                val parts = ArrayList<String>()
                val first = node.jjtGetChild(0)
                if (first !is ASTIdentifier) return null
                parts.add(first.name)
                for (i in 1 until node.jjtGetNumChildren()) {
                    when (val child = node.jjtGetChild(i)) {
                        is ASTIdentifierAccess -> parts.add(child.name)
                        is ASTArrayAccess -> {
                            if (child.jjtGetNumChildren() != 1) return null
                            val keyNode = child.jjtGetChild(0)
                            if (keyNode !is ASTStringLiteral) return null
                            parts.add(keyNode.literal)
                        }
                        else -> return null
                    }
                }
                parts
            }
            else -> null
        }
    }

    private fun atomFromPath(path: List<String>, value: String, eq: Boolean): GraphAtom? =
        when {
            path.size == 1 && path[0] == "id" -> {
                val uuid = try {
                    UUID.fromString(value)
                } catch (_: IllegalArgumentException) {
                    return null
                }
                GraphAtom.Id(uuid, eq)
            }
            path.size == 2 && path[0] == "a" -> GraphAtom.Annotation(path[1], value, eq)
            else -> null
        }

    private fun foldAndGroup(atoms: List<GraphAtom>): BoMGraphExprAndGroup? {
        var idEquals: UUID? = null
        val idNotEquals = linkedSetOf<UUID>()
        val annotationEquals = linkedMapOf<String, String>()
        val annotationNotEquals = linkedMapOf<String, String>()

        for (atom in atoms) {
            when (atom) {
                is GraphAtom.Id -> {
                    if (atom.eq) {
                        if (idEquals != null && idEquals != atom.value) return null
                        idEquals = atom.value
                    } else {
                        idNotEquals += atom.value
                    }
                }
                is GraphAtom.Annotation -> {
                    if (atom.eq) {
                        val prev = annotationEquals.put(atom.key, atom.value)
                        if (prev != null && prev != atom.value) return null
                    } else {
                        val prev = annotationNotEquals.put(atom.key, atom.value)
                        if (prev != null && prev != atom.value) return null
                    }
                }
            }
        }

        if (idEquals != null && idEquals in idNotEquals) return null
        for ((k, v) in annotationEquals) {
            if (annotationNotEquals[k] == v) return null
        }

        val group = BoMGraphExprAndGroup(
            idEquals = idEquals,
            idNotEquals = idNotEquals,
            annotationEquals = annotationEquals,
            annotationNotEquals = annotationNotEquals,
        )
        return if (group.hasConstraint) group else null
    }

    private sealed interface GraphAtom {
        data class Id(val value: UUID, val eq: Boolean) : GraphAtom
        data class Annotation(val key: String, val value: String, val eq: Boolean) : GraphAtom
    }
}

/** DNF pushdown plan for [BoMGraphExprMatcher] (`==`/`!=` with `&&`/`||` over `id` / `a.*`). */
data class BoMGraphExprPushdown(
    val dnf: List<BoMGraphExprAndGroup>,
) {
    val idEquals: UUID? get() = dnf.singleOrNull()?.idEquals
    val annotationEquals: Map<String, String> get() = dnf.singleOrNull()?.annotationEquals.orEmpty()

    val needsJsonbContainment: Boolean
        get() = dnf.any { it.annotationEquals.isNotEmpty() }

    val isUnsatisfiable: Boolean get() = dnf.isEmpty()
}

/** One AND-group inside a [BoMGraphExprPushdown] DNF. */
data class BoMGraphExprAndGroup(
    val idEquals: UUID? = null,
    val idNotEquals: Set<UUID> = emptySet(),
    val annotationEquals: Map<String, String> = emptyMap(),
    val annotationNotEquals: Map<String, String> = emptyMap(),
) {
    val hasConstraint: Boolean
        get() =
            idEquals != null ||
                idNotEquals.isNotEmpty() ||
                annotationEquals.isNotEmpty() ||
                annotationNotEquals.isNotEmpty()
}
