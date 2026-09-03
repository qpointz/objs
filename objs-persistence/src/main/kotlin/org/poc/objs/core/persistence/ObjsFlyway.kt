package org.poc.objs.core.persistence

import org.flywaydb.core.Flyway

/** Applied objs-core schema Flyway (not a [Flyway] bean — Boot owns that type). */
class ObjsFlyway(val flyway: Flyway)
