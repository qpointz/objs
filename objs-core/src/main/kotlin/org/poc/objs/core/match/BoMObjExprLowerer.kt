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
 * Lowers `obj-expr` AST to [BoMObjExprPushdown] when the tree is only equality (`==` / `===`)
 * of `id` / `type` / `schemaVersion` / `a.*` / `p.*` combined with `&&`.
 * Any `||` or unsupported shape → null (local eval).
 */
object BoMObjExprLowerer : ScriptVisitor() {

    fun toPushdown(compiled: JexlExpression): BoMObjExprPushdown? {
        @Suppress("UNCHECKED_CAST")
        val clauses = visitExpression(compiled, null) as? List<ObjEqClause> ?: return null
        if (clauses.isEmpty()) return null
        var typeEquals: String? = null
        var idEquals: UUID? = null
        var schemaVersionEquals: String? = null
        val annotations = linkedMapOf<String, String>()
        val payload = linkedMapOf<String, String>()
        for (clause in clauses) {
            when (clause) {
                is ObjEqClause.Type -> {
                    if (typeEquals != null && typeEquals != clause.value) return null
                    typeEquals = clause.value
                }
                is ObjEqClause.Id -> {
                    if (idEquals != null && idEquals != clause.value) return null
                    idEquals = clause.value
                }
                is ObjEqClause.SchemaVersion -> {
                    if (schemaVersionEquals != null && schemaVersionEquals != clause.value) return null
                    schemaVersionEquals = clause.value
                }
                is ObjEqClause.Annotation -> annotations[clause.key] = clause.value
                is ObjEqClause.Payload -> payload[clause.key] = clause.value
            }
        }
        return try {
            BoMObjExprPushdown(
                typeEquals = typeEquals,
                idEquals = idEquals,
                schemaVersionEquals = schemaVersionEquals,
                annotationEquals = annotations,
                payloadEquals = payload,
            )
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
        val parts = ArrayList<ObjEqClause>()
        for (i in 0 until node.jjtGetNumChildren()) {
            when (val child = node.jjtGetChild(i).jjtAccept(this, data)) {
                is ObjEqClause -> parts.add(child)
                is List<*> -> {
                    for (item in child) {
                        parts.add(item as? ObjEqClause ?: return null)
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

    private fun parseEquality(node: JexlNode): ObjEqClause? {
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

    private fun clauseFromPath(path: List<String>, value: String): ObjEqClause? =
        when {
            path.size == 1 && path[0] == "type" -> ObjEqClause.Type(value)
            path.size == 1 && path[0] == "schemaVersion" -> ObjEqClause.SchemaVersion(value)
            path.size == 1 && path[0] == "id" -> {
                val uuid = try {
                    UUID.fromString(value)
                } catch (_: IllegalArgumentException) {
                    return null
                }
                ObjEqClause.Id(uuid)
            }
            path.size == 2 && path[0] == "a" -> ObjEqClause.Annotation(path[1], value)
            path.size == 2 && path[0] == "p" -> ObjEqClause.Payload(path[1], value)
            else -> null
        }

    private sealed interface ObjEqClause {
        data class Type(val value: String) : ObjEqClause
        data class Id(val value: UUID) : ObjEqClause
        data class SchemaVersion(val value: String) : ObjEqClause
        data class Annotation(val key: String, val value: String) : ObjEqClause
        data class Payload(val key: String, val value: String) : ObjEqClause
    }
}
