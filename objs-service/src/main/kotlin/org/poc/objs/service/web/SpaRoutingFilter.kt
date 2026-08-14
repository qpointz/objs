package org.poc.objs.service.web

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * Browser SPA routing: redirects the app prefix (and optionally `/`) to `{base}/`,
 * forwards deep-links to [spaIndexPath], and lets API plus static files (`*.ext`) through.
 *
 * Same approach as Mill's `MillUiSpaRoutingFilter`. Servlet filters are registered for
 * `REQUEST` only, so a forward to `index.html` is not re-processed by this filter.
 */
open class SpaRoutingFilter(
    appBasePath: String,
    private val spaIndexPath: String,
    private val redirectRoot: Boolean = false,
) : Filter {

    private val log = LoggerFactory.getLogger(javaClass)

    private val normalizedBase: String = normalizeBasePath(appBasePath)

    private val redirectToAppSlash: String =
        if (normalizedBase.endsWith("/")) normalizedBase else "$normalizedBase/"

    /** Last segment looks like a file (`index.js`, `logo.svg`), not a version (`1.0.0`). */
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
        if (redirectRoot && (uri.isEmpty() || uri == "/")) {
            return true
        }
        return false
    }

    companion object {
        fun normalizeBasePath(raw: String?): String {
            if (raw.isNullOrBlank()) {
                return "/app"
            }
            var b = raw.trim()
            if (!b.startsWith("/")) {
                b = "/$b"
            }
            if (b.endsWith("/") && b.length > 1) {
                b = b.substring(0, b.length - 1)
            }
            return b
        }

        fun servletPath(req: HttpServletRequest): String {
            val uri = req.requestURI ?: return "/"
            val ctx = req.contextPath.orEmpty()
            val path = if (ctx.isNotEmpty() && uri.startsWith(ctx)) uri.substring(ctx.length) else uri
            return path.ifEmpty { "/" }
        }
    }
}
