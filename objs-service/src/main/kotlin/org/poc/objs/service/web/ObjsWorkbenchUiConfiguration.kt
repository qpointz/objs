package org.poc.objs.service.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver
import org.springframework.web.servlet.view.RedirectView
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Serves the workbench SPA from classpath `static/ui` at `/workbench/`.
 *
 * Index HTML is served by [WorkbenchSpaController] (not a view-controller forward into
 * [org.springframework.web.servlet.resource.ResourceHttpRequestHandler]), so a missing
 * `static/ui/index.html` returns 503 with a plain message instead of FileNotFoundException/500
 * from `Resource.lastModified()`.
 */
@Configuration
class ObjsWorkbenchUiConfiguration : WebMvcConfigurer {
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addRedirectViewController("/workbench", "/workbench/")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/workbench/**")
            .addResourceLocations("classpath:/static/ui/")
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                @Throws(IOException::class)
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    val path = resourcePath.trimStart('/')
                    if (path.isEmpty()) {
                        return readableRelative(location, "index.html")
                    }
                    readableRelative(location, path)?.let { return it }
                    if (!looksLikeStaticFile(path)) {
                        return readableRelative(location, "index.html")
                    }
                    return null
                }
            })
    }

    companion object {
        fun readableRelative(location: Resource, relative: String): Resource? =
            try {
                val resource = location.createRelative(relative)
                if (resource.isReadable) resource else null
            } catch (_: IOException) {
                null
            }

        fun workbenchIndex(): Resource = ClassPathResource("static/ui/index.html")

        fun looksLikeStaticFile(path: String): Boolean {
            val last = path.substringAfterLast('/')
            return last.matches(Regex(".*\\.[a-zA-Z][a-zA-Z0-9]{0,5}$"))
        }
    }
}

/** Entry HTML for `/workbench/` without ResourceHttpRequestHandler lastModified on a missing file. */
@Controller
class WorkbenchSpaController {
    @GetMapping("/workbench/", produces = [MediaType.TEXT_HTML_VALUE])
    fun index(): ResponseEntity<ByteArray> {
        val index = ObjsWorkbenchUiConfiguration.workbenchIndex()
        if (!index.isReadable) {
            val message =
                """
                Workbench UI is not on the classpath (static/ui/index.html missing).
                Rebuild without -PskipUi=true, e.g. ./gradlew :objs-app:run
                """.trimIndent()
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.TEXT_PLAIN)
                .body(message.toByteArray(StandardCharsets.UTF_8))
        }
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(index.inputStream.use { it.readBytes() })
    }
}

/** Redirect legacy `/ui` bookmarks into `/workbench`. */
@Controller
class LegacyWorkbenchUiRedirectController {
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
