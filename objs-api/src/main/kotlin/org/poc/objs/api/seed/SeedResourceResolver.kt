package org.poc.objs.api.seed

/** Resolves seed resource locations to byte streams (classpath, file, etc.). */
fun interface SeedResourceResolver {
    /** @return stream or null if missing */
    fun open(location: String): java.io.InputStream?
}
