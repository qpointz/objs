package org.poc.objs.sbom

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.ComponentScan

/**
 * SBOM example auto-configuration.
 *
 * Seed resources are configured exclusively by the consuming application's `objs.seeds`
 * properties. Typed [org.poc.objs.sbom.registry.SbomRegistry] remains available for builders
 * and parity tests.
 */
@AutoConfiguration
@ComponentScan(basePackageClasses = [ObjsSbomExample::class])
class ObjsSbomExampleAutoConfiguration
