package org.poc.objs.jgrapht.core

import java.util.UUID

object UuidOrdering {
    /** Unsigned 128-bit RFC 4122 byte order. */
    fun compare(left: UUID, right: UUID): Int {
        val msbCompare = java.lang.Long.compareUnsigned(left.mostSignificantBits, right.mostSignificantBits)
        if (msbCompare != 0) {
            return msbCompare
        }
        return java.lang.Long.compareUnsigned(left.leastSignificantBits, right.leastSignificantBits)
    }

    fun sorted(values: Iterable<UUID>): List<UUID> = values.sortedWith(::compare)
}
