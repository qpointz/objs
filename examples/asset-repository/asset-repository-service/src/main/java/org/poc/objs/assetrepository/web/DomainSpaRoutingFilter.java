package org.poc.objs.assetrepository.web;

import org.poc.objs.service.web.SpaRoutingFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Forwards {@code /app} client routes to {@code /app/index.html}; redirects {@code /} to {@code /app/}. */
@Component
@Order(1)
public class DomainSpaRoutingFilter extends SpaRoutingFilter {

    public DomainSpaRoutingFilter() {
        super("/app", "/app/index.html", true);
    }
}
