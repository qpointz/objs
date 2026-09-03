package org.poc.objs.api.match

import org.apache.commons.jexl3.JexlExpression
import org.apache.commons.jexl3.internal.ScriptVisitor
import org.apache.commons.jexl3.parser.ASTAndNode
import org.apache.commons.jexl3.parser.ASTArrayAccess
import org.apache.commons.jexl3.parser.ASTEQNode
import org.apache.commons.jexl3.parser.ASTEQSNode
import org.apache.commons.jexl3.parser.ASTGENode
import org.apache.commons.jexl3.parser.ASTGTNode
import org.apache.commons.jexl3.parser.ASTIdentifier
import org.apache.commons.jexl3.parser.ASTIdentifierAccess
import org.apache.commons.jexl3.parser.ASTJexlScript
import org.apache.commons.jexl3.parser.ASTLENode
import org.apache.commons.jexl3.parser.ASTLTNode
import org.apache.commons.jexl3.parser.ASTNENode
import org.apache.commons.jexl3.parser.ASTNESNode
import org.apache.commons.jexl3.parser.ASTOrNode
import org.apache.commons.jexl3.parser.ASTReference
import org.apache.commons.jexl3.parser.ASTReferenceExpression
import org.apache.commons.jexl3.parser.ASTStringLiteral
import org.apache.commons.jexl3.parser.ASTERNode
import org.apache.commons.jexl3.parser.JexlNode
import java.util.UUID

/**
 * Lowers `obj-expr` AST to [ObjExprPushdown] when the tree uses supported predicates
 * (`==` / `!=`, ordered compares, anchored prefix `=~ '^…'`) on `id` / `type` / `schemaVersion` /
 * `a.*` / `p.*` combined with `&&` / `||`. Anything else → null (local eval).
 */
object ObjExprLowerer : ScriptVisitor() {

    fun toPushdown(compiled: JexlExpression): ObjExprPushdown? {
        @Suppress("UNCHECKED_CAST")
        val tree = visitExpression(compiled, null) as? BoolExpr<ObjAtom> ?: return null
        val groups = toDnf(tree).mapNotNull { atoms -> foldAndGroup(atoms) }
        if (groups.isEmpty()) {
            return ObjExprPushdown(dnf = emptyList())
        }
        return ObjExprPushdown(dnf = groups)
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
        val parts = ArrayList<BoolExpr<ObjAtom>>()
        for (i in 0 until node.jjtGetNumChildren()) {
            val child = node.jjtGetChild(i).jjtAccept(this, data) as? BoolExpr<*> ?: return null
            @Suppress("UNCHECKED_CAST")
            parts.add(child as BoolExpr<ObjAtom>)
        }
        return if (parts.isEmpty()) null else BoolExpr.And(parts)
    }

    override fun visit(node: ASTOrNode, data: Any?): Any? {
        val parts = ArrayList<BoolExpr<ObjAtom>>()
        for (i in 0 until node.jjtGetNumChildren()) {
            val child = node.jjtGetChild(i).jjtAccept(this, data) as? BoolExpr<*> ?: return null
            @Suppress("UNCHECKED_CAST")
            parts.add(child as BoolExpr<ObjAtom>)
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

    override fun visit(node: ASTGTNode, data: Any?): Any? =
        parseOrderedComparison(node, PayloadCompareOp.GT)?.let { BoolExpr.Atom(it) }

    override fun visit(node: ASTGENode, data: Any?): Any? =
        parseOrderedComparison(node, PayloadCompareOp.GE)?.let { BoolExpr.Atom(it) }

    override fun visit(node: ASTLTNode, data: Any?): Any? =
        parseOrderedComparison(node, PayloadCompareOp.LT)?.let { BoolExpr.Atom(it) }

    override fun visit(node: ASTLENode, data: Any?): Any? =
        parseOrderedComparison(node, PayloadCompareOp.LE)?.let { BoolExpr.Atom(it) }

    override fun visit(node: ASTERNode, data: Any?): Any? =
        parsePrefixMatch(node)?.let { BoolExpr.Atom(it) }

    override fun visitNode(node: JexlNode, data: Any?): Any? = null

    private fun parseComparison(node: JexlNode, eq: Boolean): ObjAtom? {
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

    private fun parseOrderedComparison(node: JexlNode, op: PayloadCompareOp): ObjAtom? {
        if (node.jjtGetNumChildren() != 2) return null
        val left = node.jjtGetChild(0)
        val right = node.jjtGetChild(1)
        val (path, value) = when {
            right is ASTStringLiteral -> pathFrom(left) to right.literal
            left is ASTStringLiteral -> pathFrom(right) to left.literal
            else -> return null
        }
        val key = payloadKey(path ?: return null) ?: return null
        return ObjAtom.PayloadCompare(key, op, value)
    }

    private fun parsePrefixMatch(node: JexlNode): ObjAtom? {
        if (node.jjtGetNumChildren() != 2) return null
        val left = node.jjtGetChild(0)
        val right = node.jjtGetChild(1)
        val (path, pattern) = when {
            right is ASTStringLiteral -> pathFrom(left) to right.literal
            left is ASTStringLiteral -> pathFrom(right) to left.literal
            else -> return null
        }
        val key = payloadKey(path ?: return null) ?: return null
        val prefix = literalPrefix(pattern) ?: return null
        return ObjAtom.PayloadPrefix(key, prefix)
    }

    private fun payloadKey(path: List<String>): String? =
        if (path.size == 2 && path[0] == "p") path[1] else null

    private fun literalPrefix(pattern: String): String? {
        if (!pattern.startsWith("^")) return null
        var body = pattern.substring(1)
        if (body.endsWith("$")) {
            body = body.dropLast(1)
        }
        if (body.isEmpty() || !LITERAL_PREFIX.matches(body)) return null
        return body
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

    private fun atomFromPath(path: List<String>, value: String, eq: Boolean): ObjAtom? =
        when {
            path.size == 1 && path[0] == "type" -> ObjAtom.Type(value, eq)
            path.size == 1 && path[0] == "schemaVersion" -> ObjAtom.SchemaVersion(value, eq)
            path.size == 1 && path[0] == "id" -> {
                val uuid = try {
                    UUID.fromString(value)
                } catch (_: IllegalArgumentException) {
                    return null
                }
                ObjAtom.Id(uuid, eq)
            }
            path.size == 2 && path[0] == "a" -> ObjAtom.Annotation(path[1], value, eq)
            path.size == 2 && path[0] == "p" -> ObjAtom.Payload(path[1], value, eq)
            else -> null
        }

    private fun foldAndGroup(atoms: List<ObjAtom>): ObjExprAndGroup? {
        var typeEquals: String? = null
        val typeNotEquals = linkedSetOf<String>()
        var idEquals: UUID? = null
        val idNotEquals = linkedSetOf<UUID>()
        var schemaVersionEquals: String? = null
        val schemaVersionNotEquals = linkedSetOf<String>()
        val annotationEquals = linkedMapOf<String, String>()
        val annotationNotEquals = linkedMapOf<String, String>()
        val payloadEquals = linkedMapOf<String, String>()
        val payloadNotEquals = linkedMapOf<String, String>()
        val payloadGt = linkedMapOf<String, String>()
        val payloadGe = linkedMapOf<String, String>()
        val payloadLt = linkedMapOf<String, String>()
        val payloadLe = linkedMapOf<String, String>()
        val payloadPrefix = linkedMapOf<String, String>()

        for (atom in atoms) {
            when (atom) {
                is ObjAtom.Type -> {
                    if (atom.eq) {
                        if (typeEquals != null && typeEquals != atom.value) return null
                        typeEquals = atom.value
                    } else {
                        typeNotEquals += atom.value
                    }
                }
                is ObjAtom.Id -> {
                    if (atom.eq) {
                        if (idEquals != null && idEquals != atom.value) return null
                        idEquals = atom.value
                    } else {
                        idNotEquals += atom.value
                    }
                }
                is ObjAtom.SchemaVersion -> {
                    if (atom.eq) {
                        if (schemaVersionEquals != null && schemaVersionEquals != atom.value) return null
                        schemaVersionEquals = atom.value
                    } else {
                        schemaVersionNotEquals += atom.value
                    }
                }
                is ObjAtom.Annotation -> {
                    if (atom.eq) {
                        val prev = annotationEquals.put(atom.key, atom.value)
                        if (prev != null && prev != atom.value) return null
                    } else {
                        val prev = annotationNotEquals.put(atom.key, atom.value)
                        if (prev != null && prev != atom.value) return null
                    }
                }
                is ObjAtom.Payload -> {
                    if (atom.eq) {
                        val prev = payloadEquals.put(atom.key, atom.value)
                        if (prev != null && prev != atom.value) return null
                    } else {
                        val prev = payloadNotEquals.put(atom.key, atom.value)
                        if (prev != null && prev != atom.value) return null
                    }
                }
                is ObjAtom.PayloadCompare -> {
                    val map =
                        when (atom.op) {
                            PayloadCompareOp.GT -> payloadGt
                            PayloadCompareOp.GE -> payloadGe
                            PayloadCompareOp.LT -> payloadLt
                            PayloadCompareOp.LE -> payloadLe
                        }
                    val prev = map.put(atom.key, atom.value)
                    if (prev != null && prev != atom.value) return null
                }
                is ObjAtom.PayloadPrefix -> {
                    val prev = payloadPrefix.put(atom.key, atom.prefix)
                    if (prev != null && prev != atom.prefix) return null
                }
            }
        }

        if (typeEquals != null && typeEquals in typeNotEquals) return null
        if (idEquals != null && idEquals in idNotEquals) return null
        if (schemaVersionEquals != null && schemaVersionEquals in schemaVersionNotEquals) return null
        for ((k, v) in annotationEquals) {
            if (annotationNotEquals[k] == v) return null
        }
        for ((k, v) in payloadEquals) {
            if (payloadNotEquals[k] == v) return null
        }

        val group =
            ObjExprAndGroup(
                typeEquals = typeEquals,
                typeNotEquals = typeNotEquals,
                idEquals = idEquals,
                idNotEquals = idNotEquals,
                schemaVersionEquals = schemaVersionEquals,
                schemaVersionNotEquals = schemaVersionNotEquals,
                annotationEquals = annotationEquals,
                annotationNotEquals = annotationNotEquals,
                payloadEquals = payloadEquals,
                payloadNotEquals = payloadNotEquals,
                payloadGt = payloadGt,
                payloadGe = payloadGe,
                payloadLt = payloadLt,
                payloadLe = payloadLe,
                payloadPrefix = payloadPrefix,
            )
        return if (group.hasConstraint) group else null
    }

    private sealed interface ObjAtom {
        data class Type(val value: String, val eq: Boolean) : ObjAtom
        data class Id(val value: UUID, val eq: Boolean) : ObjAtom
        data class SchemaVersion(val value: String, val eq: Boolean) : ObjAtom
        data class Annotation(val key: String, val value: String, val eq: Boolean) : ObjAtom
        data class Payload(val key: String, val value: String, val eq: Boolean) : ObjAtom
        data class PayloadCompare(val key: String, val op: PayloadCompareOp, val value: String) : ObjAtom
        data class PayloadPrefix(val key: String, val prefix: String) : ObjAtom
    }

    private val LITERAL_PREFIX = Regex("""[A-Za-z0-9._\-+]+""")
}

enum class PayloadCompareOp {
    GT,
    GE,
    LT,
    LE,
}
