package org.poc.objs.sbom.web

import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver
import java.nio.charset.StandardCharsets

/**
 * Serves the inventory SPA from classpath `static/sbom` at `/ui/`
 * (distinct from workbench `static/ui` at `/workbench/`).
 */
@Configuration
class SbomUiConfiguration : WebMvcConfigurer {
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addRedirectViewController("/ui", "/ui/")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/ui/**")
            .addResourceLocations("classpath:/static/sbom/")
            .resourceChain(true)
            .addResolver(
                object : PathResourceResolver() {
                    override fun getResource(resourcePath: String, location: Resource): Resource? {
                        val path = if (resourcePath.startsWith("/")) resourcePath.substring(1) else resourcePath
                        if (path.isEmpty()) {
                            return readableRelative(location, "index.html")
                        }
                        val existing = readableRelative(location, path)
                        if (existing != null) {
                            return existing
                        }
                        if (!looksLikeStaticFile(path)) {
                            return readableRelative(location, "index.html")
                        }
                        return null
                    }
                },
            )
    }

    @Controller
    class SbomSpaController {
        @GetMapping(value = ["/ui/"], produces = [MediaType.TEXT_HTML_VALUE])
        fun index(): ResponseEntity<Resource> {
            val index = ClassPathResource("static/sbom/index.html")
            if (!index.exists()) {
                return ResponseEntity.status(503)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(
                        org.springframework.core.io.ByteArrayResource(
                            "Inventory UI is not on the classpath (static/sbom/index.html missing)."
                                .toByteArray(StandardCharsets.UTF_8),
                        ),
                    )
            }
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(index)
        }
    }

    companion object {
        private fun looksLikeStaticFile(path: String): Boolean {
            val slash = path.lastIndexOf('/')
            val last = if (slash >= 0) path.substring(slash + 1) else path
            return last.matches(Regex(".*\\.[a-zA-Z][a-zA-Z0-9]{0,5}$"))
        }

        private fun readableRelative(location: Resource, relative: String): Resource? {
            return try {
                val resource = location.createRelative(relative)
                if (resource.isReadable) resource else null
            } catch (_: java.io.IOException) {
                null
            }
        }
    }
}
