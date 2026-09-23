package org.poc.objs.api.typed.upgrade

/**
 * Resolves an ordered list of [SchemaUpgradeStep] from a stored pin to a target pin.
 * Empty list means no explicit path (caller may try additive fallback).
 */
fun interface SchemaUpgradeRegistry {
    fun findChain(type: String, fromVersion: String, toVersion: String): List<SchemaUpgradeStep>
}
