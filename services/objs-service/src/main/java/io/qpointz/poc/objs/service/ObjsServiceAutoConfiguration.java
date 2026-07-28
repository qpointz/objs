package io.qpointz.poc.objs.service;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Autoconfiguration entry for objs REST and service beans.
 */
@AutoConfiguration
@ComponentScan(basePackages = "io.qpointz.poc.objs.service")
public class ObjsServiceAutoConfiguration {
}
