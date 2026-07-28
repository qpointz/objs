package org.poc.objs.core.domain

import java.security.SecureRandom
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

/**
 * RFC 9562 UUID version 7 generator (time-ordered for better B-tree locality).
 */
object UuidV7 {
    private val secureRandom = SecureRandom()

    fun generate(clockMillis: Long = Instant.now().toEpochMilli()): UUID {
        require(clockMillis >= 0) { "clockMillis must be non-negative" }
        val bytes = ByteArray(16)
        // 48-bit big-endian timestamp
        bytes[0] = ((clockMillis ushr 40) and 0xFF).toByte()
        bytes[1] = ((clockMillis ushr 32) and 0xFF).toByte()
        bytes[2] = ((clockMillis ushr 24) and 0xFF).toByte()
        bytes[3] = ((clockMillis ushr 16) and 0xFF).toByte()
        bytes[4] = ((clockMillis ushr 8) and 0xFF).toByte()
        bytes[5] = (clockMillis and 0xFF).toByte()

        val rand = ByteArray(10)
        secureRandom.nextBytes(rand)
        System.arraycopy(rand, 0, bytes, 6, 10)

        // version 7
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x70).toByte()
        // IETF variant
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

        var msb = 0L
        var lsb = 0L
        for (i in 0..7) {
            msb = (msb shl 8) or (bytes[i].toLong() and 0xFF)
        }
        for (i in 8..15) {
            lsb = (lsb shl 8) or (bytes[i].toLong() and 0xFF)
        }
        return UUID(msb, lsb)
    }

    /** Test helper using ThreadLocalRandom when SecureRandom is unnecessary. */
    fun generateFast(clockMillis: Long = Instant.now().toEpochMilli()): UUID {
        val rnd = ThreadLocalRandom.current()
        val bytes = ByteArray(16)
        bytes[0] = ((clockMillis ushr 40) and 0xFF).toByte()
        bytes[1] = ((clockMillis ushr 32) and 0xFF).toByte()
        bytes[2] = ((clockMillis ushr 24) and 0xFF).toByte()
        bytes[3] = ((clockMillis ushr 16) and 0xFF).toByte()
        bytes[4] = ((clockMillis ushr 8) and 0xFF).toByte()
        bytes[5] = (clockMillis and 0xFF).toByte()
        rnd.nextBytes(bytes)
        // restore timestamp after random fill of all bytes — rewrite first 6
        bytes[0] = ((clockMillis ushr 40) and 0xFF).toByte()
        bytes[1] = ((clockMillis ushr 32) and 0xFF).toByte()
        bytes[2] = ((clockMillis ushr 24) and 0xFF).toByte()
        bytes[3] = ((clockMillis ushr 16) and 0xFF).toByte()
        bytes[4] = ((clockMillis ushr 8) and 0xFF).toByte()
        bytes[5] = (clockMillis and 0xFF).toByte()
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x70).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
        var msb = 0L
        var lsb = 0L
        for (i in 0..7) msb = (msb shl 8) or (bytes[i].toLong() and 0xFF)
        for (i in 8..15) lsb = (lsb shl 8) or (bytes[i].toLong() and 0xFF)
        return UUID(msb, lsb)
    }
}
