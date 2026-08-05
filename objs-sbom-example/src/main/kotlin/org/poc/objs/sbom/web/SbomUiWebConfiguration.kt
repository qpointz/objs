package org.poc.objs.sbom.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver
import org.springframework.web.servlet.view.RedirectView
import java.io.IOException

/**
 * Serves the workbench SPA from classpath static/ui at /workbench/.
 */
@Configuration
class SbomUiWebConfiguration : WebMvcConfigurer {
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addRedirectViewController("/workbench", "/workbench/")
        registry.addViewController("/workbench/").setViewName("forward:/workbench/index.html")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/workbench/**")
            .addResourceLocations("classpath:/static/ui/")
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                @Throws(IOException::class)
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    val requested = location.createRelative(resourcePath)
                    return if (requested.exists() && requested.isReadable) {
                        requested
                    } else {
                        ClassPathResource("/static/ui/index.html")
                    }
                }
            })
    }
}

/** Redirect legacy /ui bookmarks into /workbench. */
@Controller
class LegacyUiRedirectController {
    @GetMapping("/ui", "/ui/**")
    fun redirect(request: HttpServletRequest): RedirectView {
        val suffix = request.requestURI.removePrefix(request.contextPath).removePrefix("/ui")
        val mapped = when {
            suffix == "/graph" || suffix.startsWith("/graph/") ->
                suffix.replaceFirst("/graph", "/explorer")
            suffix == "/object-linter" || suffix.startsWith("/object-linter/") ->
                suffix.replaceFirst("/object-linter", "/composer")
            suffix == "/schemas" || suffix.startsWith("/schemas/") ->
                suffix.replaceFirst("/schemas", "/model")
            suffix.isEmpty() || suffix == "/" -> "/"
            else -> suffix
        }
        val target = "/workbench" + if (mapped.startsWith("/")) mapped else "/$mapped"
        return RedirectView(target, true)
    }
}
