package org.poc.objs.assetrepository.web;

import org.poc.objs.service.web.SpaRoutingFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Forwards {@code /ar} client routes to {@code /ar/index.html}; redirects {@code /} to {@code /ar/}. */
@Component
@Order(1)
public class DomainSpaRoutingFilter extends SpaRoutingFilter {

    public DomainSpaRoutingFilter() {
        super("/ar", "/ar/index.html", true);
    }
}
