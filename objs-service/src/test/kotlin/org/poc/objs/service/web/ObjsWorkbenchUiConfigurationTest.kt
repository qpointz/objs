package org.poc.objs.service.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockServletContext
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc

/**
 * Missing SPA assets must not produce HTTP 500 / FileNotFoundException from
 * ResourceHttpRequestHandler.lastModified on a non-existent ClassPathResource.
 */
class ObjsWorkbenchUiConfigurationTest {

    @Test
    fun shouldNotReturn500_whenWorkbenchIndexMissingOrPresent() {
        AnnotationConfigWebApplicationContext().use { ctx ->
            ctx.servletContext = MockServletContext()
            ctx.register(
                EnableWebMvcConfig::class.java,
                ObjsWorkbenchUiConfiguration::class.java,
                WorkbenchSpaController::class.java,
            )
            ctx.refresh()
            val mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build()

            val status = mockMvc.perform(get("/workbench/").accept(MediaType.TEXT_HTML))
                .andReturn()
                .response
                .status
            assertThat(status)
                .describedAs("GET /workbench/")
                .isNotEqualTo(500)
            assertThat(status).isIn(200, 503)

            val assetStatus = mockMvc.perform(get("/workbench/index.html").accept(MediaType.TEXT_HTML))
                .andReturn()
                .response
                .status
            assertThat(assetStatus)
                .describedAs("GET /workbench/index.html")
                .isNotEqualTo(500)
            assertThat(assetStatus).isIn(200, 404)
        }
    }

    @EnableWebMvc
    private class EnableWebMvcConfig
}
