package org.poc.objs.api

import java.util.UUID

/** Root unchecked exception for schema-agnostic API failures. */
open class ObjsException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** Raised when a singular relation accessor has more than one matching target. */
class AmbiguousRelationException(
    val nodeId: UUID?,
    val relation: String,
    val matches: Int,
) : ObjsException(
    "Relation '$relation' on node $nodeId has $matches matches; expected at most one",
)
