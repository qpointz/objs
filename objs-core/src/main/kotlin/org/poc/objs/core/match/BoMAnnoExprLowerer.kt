package org.poc.objs.core.match

import org.apache.commons.jexl3.JexlExpression
import org.apache.commons.jexl3.internal.ScriptVisitor
import org.apache.commons.jexl3.parser.ASTAndNode
import org.apache.commons.jexl3.parser.ASTEQNode
import org.apache.commons.jexl3.parser.ASTEQSNode
import org.apache.commons.jexl3.parser.ASTIdentifier
import org.apache.commons.jexl3.parser.ASTJexlScript
import org.apache.commons.jexl3.parser.ASTOrNode
import org.apache.commons.jexl3.parser.ASTReference
import org.apache.commons.jexl3.parser.ASTReferenceExpression
import org.apache.commons.jexl3.parser.ASTStringLiteral
import org.apache.commons.jexl3.parser.JexlNode

/**
 * Lowers a compiled JEXL annotation expression to [BoMMatchExpression] when the AST is only
 * identifier `==` / `===` string-literal comparisons combined with `&&` / `||`.
 * Returns null for any unsupported shape (caller falls back to in-memory matching).
 */
object BoMAnnoExprLowerer : ScriptVisitor() {

    fun toMatchExpression(compiled: JexlExpression): BoMMatchExpression? =
        visitExpression(compiled, null) as? BoMMatchExpression

    /** Containment maps for `OR` of `@>` pushdown, or null if not lowerable. */
    fun toContainmentDisjuncts(compiled: JexlExpression): List<Map<String, String>>? {
        val lowered = toMatchExpression(compiled) ?: return null
        return BoMMatchExpression.containmentDisjuncts(lowered)
    }

    override fun visit(node: ASTJexlScript, data: Any?): Any? {
        if (node.jjtGetNumChildren() != 1) {
            return null
        }
        return node.jjtGetChild(0).jjtAccept(this, data)
    }

    override fun visit(node: ASTReferenceExpression, data: Any?): Any? = unwrapSingleChild(node, data)

    override fun visit(node: ASTReference, data: Any?): Any? = unwrapSingleChild(node, data)

    override fun visit(node: ASTAndNode, data: Any?): Any? {
        val parts = ArrayList<BoMMatchExpression>(node.jjtGetNumChildren())
        for (i in 0 until node.jjtGetNumChildren()) {
            val child = node.jjtGetChild(i).jjtAccept(this, data) as? BoMMatchExpression ?: return null
            when (child) {
                is BoMMatchExpression.And -> parts.addAll(child.expressions)
                else -> parts.add(child)
            }
        }
        return if (parts.isEmpty()) null else BoMMatchExpression.And(parts)
    }

    override fun visit(node: ASTOrNode, data: Any?): Any? {
        val parts = ArrayList<BoMMatchExpression>(node.jjtGetNumChildren())
        for (i in 0 until node.jjtGetNumChildren()) {
            val child = node.jjtGetChild(i).jjtAccept(this, data) as? BoMMatchExpression ?: return null
            when (child) {
                is BoMMatchExpression.Or -> parts.addAll(child.expressions)
                else -> parts.add(child)
            }
        }
        return if (parts.isEmpty()) null else BoMMatchExpression.Or(parts)
    }

    override fun visit(node: ASTEQNode, data: Any?): Any? = lowerEquality(node)

    override fun visit(node: ASTEQSNode, data: Any?): Any? = lowerEquality(node)

    override fun visitNode(node: JexlNode, data: Any?): Any? = null

    private fun unwrapSingleChild(node: JexlNode, data: Any?): Any? {
        if (node.jjtGetNumChildren() != 1) {
            return null
        }
        return node.jjtGetChild(0).jjtAccept(this, data)
    }

    private fun lowerEquality(node: JexlNode): BoMMatchExpression? {
        if (node.jjtGetNumChildren() != 2) {
            return null
        }
        val left = node.jjtGetChild(0)
        val right = node.jjtGetChild(1)
        return when {
            left is ASTIdentifier && right is ASTStringLiteral ->
                BoMMatchExpression.AnnotationEquals(left.name, right.literal)
            right is ASTIdentifier && left is ASTStringLiteral ->
                BoMMatchExpression.AnnotationEquals(right.name, left.literal)
            else -> null
        }
    }
}
