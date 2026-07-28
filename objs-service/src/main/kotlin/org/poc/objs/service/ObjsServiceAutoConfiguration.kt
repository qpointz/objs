package org.poc.objs.service

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.ComponentScan

/**
 * Autoconfiguration entry for objs REST and service beans.
 */
@AutoConfiguration
@ComponentScan(basePackages = ["org.poc.objs.service"])
class ObjsServiceAutoConfiguration
