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

/**
 * Serves the domain SPA from classpath {@code static/ar} at {@code /app/}.
 */
@Configuration
public class DomainUiConfiguration implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/app", "/app/");
        registry.addRedirectViewController("/", "/app/");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/app/**")
                .addResourceLocations("classpath:/static/ar/");
    }

    @Controller
    static class DomainSpaController {

        @GetMapping(value = "/app/", produces = MediaType.TEXT_HTML_VALUE)
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
}
