package org.poc.objs.sbom.web

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * Browser SPA routing for the inventory UI at `/sbom`.
 * Copied from foundation `SpaRoutingFilter` so `:sbom-service` does not compile against `:objs-service`.
 */
@Component
@Order(1)
class SbomSpaRoutingFilter : Filter {

    private val log = LoggerFactory.getLogger(javaClass)

    private val normalizedBase = "/sbom"
    private val spaIndexPath = "/sbom/index.html"
    private val redirectToAppSlash = "/sbom/"

    private val staticResourcePredicate: Predicate<String> =
        Pattern.compile(
            "^" + Pattern.quote(normalizedBase) + "/.*\\.[a-zA-Z][a-zA-Z0-9]{0,5}$",
        ).asPredicate()

    private val underAppPrefix: (String) -> Boolean = { uri ->
        uri == normalizedBase || uri.startsWith("$normalizedBase/")
    }

    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
        val req = servletRequest as HttpServletRequest
        val res = servletResponse as HttpServletResponse
        val requestURI = servletPath(req)

        log.trace("SPA filter {}: {}", normalizedBase, requestURI)

        if (shouldRedirectToAppSlash(requestURI)) {
            res.sendRedirect(redirectToAppSlash)
            log.trace("Redirect {} -> {}", requestURI, redirectToAppSlash)
            return
        }

        if (!underAppPrefix(requestURI) || staticResourcePredicate.test(requestURI)) {
            filterChain.doFilter(servletRequest, servletResponse)
            return
        }

        if (!HttpMethod.GET.matches(req.method) && !HttpMethod.HEAD.matches(req.method)) {
            log.warn("Non-GET request to SPA path is not allowed: {} {}", req.method, requestURI)
            res.status = HttpServletResponse.SC_METHOD_NOT_ALLOWED
            res.setHeader("Allow", "GET, HEAD")
            return
        }

        log.debug("Forwarding SPA path {} to {}", requestURI, spaIndexPath)
        req.getRequestDispatcher(spaIndexPath).forward(req, res)
    }

    private fun shouldRedirectToAppSlash(uri: String): Boolean {
        if (uri == normalizedBase || uri == spaIndexPath) {
            return true
        }
        if (uri.isEmpty() || uri == "/") {
            return true
        }
        return false
    }

    companion object {
        fun servletPath(req: HttpServletRequest): String {
            val uri = req.requestURI ?: return "/"
            val ctx = req.contextPath.orEmpty()
            val path = if (ctx.isNotEmpty() && uri.startsWith(ctx)) uri.substring(ctx.length) else uri
            return path.ifEmpty { "/" }
        }
    }
}
