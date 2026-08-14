package org.poc.objs.service.web

import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/** Forwards `/workbench` client routes to `/workbench/index.html`. */
@Component
@Order(1)
class WorkbenchSpaRoutingFilter : SpaRoutingFilter(
    appBasePath = "/workbench",
    spaIndexPath = "/workbench/index.html",
    redirectRoot = false,
)
