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
import org.apache.commons.jexl3.parser.ASTReference
import org.apache.commons.jexl3.parser.ASTReferenceExpression
import org.apache.commons.jexl3.parser.ASTStringLiteral
import org.apache.commons.jexl3.parser.JexlNode
import java.util.UUID

/**
 * Lowers `graph-expr` AST to [BoMGraphExprPushdown] when the tree is only equality (`==` / `===`)
 * of `id` / `a.*` combined with `&&`. Any `||` or unsupported shape → null (local eval).
 */
object BoMGraphExprLowerer : ScriptVisitor() {

    fun toPushdown(compiled: JexlExpression): BoMGraphExprPushdown? {
        @Suppress("UNCHECKED_CAST")
        val clauses = visitExpression(compiled, null) as? List<GraphEqClause> ?: return null
        if (clauses.isEmpty()) return null
        var idEquals: UUID? = null
        val annotations = linkedMapOf<String, String>()
        for (clause in clauses) {
            when (clause) {
                is GraphEqClause.Id -> {
                    if (idEquals != null && idEquals != clause.value) return null
                    idEquals = clause.value
                }
                is GraphEqClause.Annotation -> annotations[clause.key] = clause.value
            }
        }
        return try {
            BoMGraphExprPushdown(idEquals = idEquals, annotationEquals = annotations)
        } catch (_: IllegalArgumentException) {
            null
        }
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
        val parts = ArrayList<GraphEqClause>()
        for (i in 0 until node.jjtGetNumChildren()) {
            when (val child = node.jjtGetChild(i).jjtAccept(this, data)) {
                is GraphEqClause -> parts.add(child)
                is List<*> -> {
                    for (item in child) {
                        parts.add(item as? GraphEqClause ?: return null)
                    }
                }
                else -> return null
            }
        }
        return parts
    }

    override fun visit(node: ASTEQNode, data: Any?): Any? = parseEquality(node)?.let { listOf(it) }

    override fun visit(node: ASTEQSNode, data: Any?): Any? = parseEquality(node)?.let { listOf(it) }

    override fun visitNode(node: JexlNode, data: Any?): Any? = null

    private fun parseEquality(node: JexlNode): GraphEqClause? {
        if (node.jjtGetNumChildren() != 2) return null
        val left = node.jjtGetChild(0)
        val right = node.jjtGetChild(1)
        val (path, value) = when {
            right is ASTStringLiteral -> pathFrom(left) to right.literal
            left is ASTStringLiteral -> pathFrom(right) to left.literal
            else -> return null
        }
        return clauseFromPath(path ?: return null, value)
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

    private fun clauseFromPath(path: List<String>, value: String): GraphEqClause? =
        when {
            path.size == 1 && path[0] == "id" -> {
                val uuid = try {
                    UUID.fromString(value)
                } catch (_: IllegalArgumentException) {
                    return null
                }
                GraphEqClause.Id(uuid)
            }
            path.size == 2 && path[0] == "a" -> GraphEqClause.Annotation(path[1], value)
            else -> null
        }

    private sealed interface GraphEqClause {
        data class Id(val value: UUID) : GraphEqClause
        data class Annotation(val key: String, val value: String) : GraphEqClause
    }
}

/** Equality/`&&` pushdown plan for [BoMGraphExprMatcher] (Postgres `annotations @>` / PK). */
data class BoMGraphExprPushdown(
    val idEquals: UUID? = null,
    val annotationEquals: Map<String, String> = emptyMap(),
) {
    init {
        require(idEquals != null || annotationEquals.isNotEmpty()) {
            "graph-expr pushdown must constrain id and/or annotations"
        }
    }
}
