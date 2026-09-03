package org.poc.objs.api.seed

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

/**
 * Name-based UUIDv5 (SHA-1) for deterministic seed identity.
 *
 * Do not use [UUID.nameUUIDFromBytes] — that produces UUIDv3 (MD5).
 */
object UuidV5 {
    /** Namespace for Objs seed entity/edge identity (`objs.poc.org/seed/v1`). */
    val OBJS_SEED_NAMESPACE: UUID = UUID.fromString("0b5e5eed-0001-5000-8000-000000000001")

    fun of(namespace: UUID, name: String): UUID {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(toBytes(namespace))
        digest.update(name.toByteArray(StandardCharsets.UTF_8))
        val hash = digest.digest()
        hash[6] = ((hash[6].toInt() and 0x0f) or 0x50).toByte() // version 5
        hash[8] = ((hash[8].toInt() and 0x3f) or 0x80).toByte() // IETF variant
        val buffer = ByteBuffer.wrap(hash, 0, 16)
        return UUID(buffer.long, buffer.long)
    }

    fun entityId(graphName: String, entityKey: String): UUID =
        of(OBJS_SEED_NAMESPACE, "$graphName/entity/$entityKey")

    fun edgeId(graphName: String, edgeKey: String): UUID =
        of(OBJS_SEED_NAMESPACE, "$graphName/edge/$edgeKey")

    private fun toBytes(uuid: UUID): ByteArray {
        val buffer = ByteBuffer.allocate(16)
        buffer.putLong(uuid.mostSignificantBits)
        buffer.putLong(uuid.leastSignificantBits)
        return buffer.array()
    }
}
