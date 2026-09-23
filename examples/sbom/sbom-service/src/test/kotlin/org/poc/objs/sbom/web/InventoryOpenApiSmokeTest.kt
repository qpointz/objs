package org.poc.objs.sbom.web

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.poc.objs.sbom.SbomApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(classes = [SbomApplication::class])
@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:sbom_openapi_smoke;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/{vendor}",
        "objs.seeds.enabled=false",
    ],
)
class InventoryOpenApiSmokeTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun shouldExposeInventoryOpenApiGroup() {
        mockMvc.perform(get("/v3/api-docs/inventory"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.info.title").value(containsString("SBOM inventory API")))
            .andExpect(jsonPath("$.paths['/api/v1/inventory/applications']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/inventory/assets']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/inventory/portfolios']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/inventory/assessment/suites']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/inventory/schema-catalog']").exists())
            .andExpect(
                jsonPath(
                    "$.paths['/api/v1/inventory/applications'].post.responses['200'].content['*/*'].schema.\$ref",
                ).value(containsString("ApplicationSummary")),
            )
    }
}
