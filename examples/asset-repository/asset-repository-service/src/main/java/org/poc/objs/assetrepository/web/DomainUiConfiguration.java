package org.poc.objs.assetrepository.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the domain SPA from classpath {@code static/ar} at {@code /ar/}.
 */
@Configuration
public class DomainUiConfiguration implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/ar", "/ar/");
        registry.addRedirectViewController("/", "/ar/");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/ar/**")
                .addResourceLocations("classpath:/static/ar/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location)
                            throws java.io.IOException {
                        String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
                        if (path.isEmpty()) {
                            return readableRelative(location, "index.html");
                        }
                        Resource existing = readableRelative(location, path);
                        if (existing != null) {
                            return existing;
                        }
                        if (!looksLikeStaticFile(path)) {
                            return readableRelative(location, "index.html");
                        }
                        return null;
                    }
                });
    }

    @Controller
    static class DomainSpaController {

        @GetMapping(value = "/ar/", produces = MediaType.TEXT_HTML_VALUE)
        ResponseEntity<Resource> index() {
            Resource index = new ClassPathResource("static/ar/index.html");
            if (!index.exists()) {
                return ResponseEntity.status(503)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(new org.springframework.core.io.ByteArrayResource(
                                "Domain UI is not on the classpath (static/ar/index.html missing)."
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            }
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(index);
        }
    }

    private static boolean looksLikeStaticFile(String path) {
        int slash = path.lastIndexOf('/');
        String last = slash >= 0 ? path.substring(slash + 1) : path;
        return last.matches(".*\\.[a-zA-Z][a-zA-Z0-9]{0,5}$");
    }

    private static Resource readableRelative(Resource location, String relative) {
        try {
            Resource resource = location.createRelative(relative);
            return resource.isReadable() ? resource : null;
        } catch (java.io.IOException ignored) {
            return null;
        }
    }
}
