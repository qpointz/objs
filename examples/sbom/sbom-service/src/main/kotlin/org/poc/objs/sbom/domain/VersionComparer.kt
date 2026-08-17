package org.poc.objs.sbom.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Strict SemVer 2.0 compare plus a Postgres-sortable [NUMERIC] encoding (G-Q7).
 * Invalid strings sort last (below every valid serial).
 */
interface VersionComparer {
    fun compare(left: String, right: String): Int

    fun toSerial(version: String): BigDecimal
}

class SemVerVersionComparer : VersionComparer {
    override fun compare(left: String, right: String): Int {
        val a = parse(left)
        val b = parse(right)
        if (a == null && b == null) return left.compareTo(right)
        if (a == null) return -1
        if (b == null) return 1
        return a.compareTo(b)
    }

    override fun toSerial(version: String): BigDecimal {
        val parsed = parse(version) ?: return INVALID_SERIAL
        val core =
            BigDecimal.valueOf(parsed.major).multiply(MAJOR) +
                BigDecimal.valueOf(parsed.minor).multiply(MINOR) +
                BigDecimal.valueOf(parsed.patch)
        if (parsed.pre.isEmpty()) {
            return core
        }
        var frac = BigDecimal.ZERO
        var place = PRE_PLACE
        for (part in parsed.pre.take(8)) {
            val piece =
                if (part.numeric != null) {
                    BigDecimal.valueOf(part.numeric).add(BigDecimal.ONE)
                } else {
                    BigDecimal.valueOf(alphaRank(part.text)).divide(ALPHA_DIV, MC)
                }
            frac += piece.multiply(place)
            place = place.multiply(PRE_PLACE)
        }
        val bounded = if (frac < BigDecimal.ONE) frac else BigDecimal("0.999999999999")
        return core.subtract(BigDecimal.ONE).add(bounded)
    }

    private fun parse(raw: String): Parsed? {
        val coreAndPre = raw.trim().removePrefix("v").removePrefix("V")
        val plus = coreAndPre.indexOf('+')
        val withoutBuild = if (plus >= 0) coreAndPre.substring(0, plus) else coreAndPre
        val dash = withoutBuild.indexOf('-')
        val core = if (dash >= 0) withoutBuild.substring(0, dash) else withoutBuild
        val pre = if (dash >= 0) withoutBuild.substring(dash + 1) else ""
        val bits = core.split('.')
        if (bits.size != 3) return null
        val major = bits[0].toLongOrNull() ?: return null
        val minor = bits[1].toLongOrNull() ?: return null
        val patch = bits[2].toLongOrNull() ?: return null
        if (bits.any { it != "0" && it.startsWith("0") }) return null
        val preParts =
            if (pre.isEmpty()) {
                emptyList()
            } else {
                pre.split('.').map { token ->
                    if (token.isEmpty()) return null
                    if (token.all { it.isDigit() }) {
                        if (token.length > 1 && token.startsWith("0")) return null
                        PrePart(token.toLong(), token)
                    } else {
                        if (!token.all { it.isLetterOrDigit() || it == '-' }) return null
                        PrePart(null, token)
                    }
                }
            }
        return Parsed(major, minor, patch, preParts)
    }

    private fun Parsed.compareTo(other: Parsed): Int {
        compareValues(major, other.major).let { if (it != 0) return it }
        compareValues(minor, other.minor).let { if (it != 0) return it }
        compareValues(patch, other.patch).let { if (it != 0) return it }
        if (pre.isEmpty() && other.pre.isEmpty()) return 0
        if (pre.isEmpty()) return 1
        if (other.pre.isEmpty()) return -1
        val n = minOf(pre.size, other.pre.size)
        for (i in 0 until n) {
            val c = pre[i].compareTo(other.pre[i])
            if (c != 0) return c
        }
        return pre.size.compareTo(other.pre.size)
    }

    private fun PrePart.compareTo(other: PrePart): Int {
        val ln = numeric
        val rn = other.numeric
        return when {
            ln != null && rn != null -> ln.compareTo(rn)
            ln != null && rn == null -> -1
            ln == null && rn != null -> 1
            else -> text.compareTo(other.text)
        }
    }

    private fun alphaRank(text: String): Long {
        var n = 0L
        for (ch in text.take(6)) {
            n = n * 40 + (ch.code.toLong() and 0x3F)
        }
        return n
    }

    private data class Parsed(
        val major: Long,
        val minor: Long,
        val patch: Long,
        val pre: List<PrePart>,
    )

    private data class PrePart(
        val numeric: Long?,
        val text: String,
    )

    companion object {
        private val MC = MathContext(40, RoundingMode.HALF_UP)
        private val MAJOR = BigDecimal("1000000000000")
        private val MINOR = BigDecimal("1000000")
        private val PRE_PLACE = BigDecimal("0.001")
        private val ALPHA_DIV = BigDecimal("100000000")
        val INVALID_SERIAL: BigDecimal = BigDecimal("-1")
    }
}
