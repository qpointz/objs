package org.poc.objs.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import org.poc.objs.api.domain.Schema
import org.poc.objs.api.domain.SchemaCatalog
import org.poc.objs.api.domain.SchemaDsl
import org.poc.objs.core.persistence.GraphStore
import org.poc.objs.core.persistence.JpaSchemaCatalog
import org.poc.objs.core.persistence.ObjsFlyway
import org.poc.objs.core.persistence.tx.UnitOfWork
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration
import java.util.UUID

/**
 * Boot wiring proof (WI-004) plus Flyway slice in [ObjsFlywayAutoConfigurationTest].
 */
class ObjsAutoconfigureWiringTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                DataSourceAutoConfiguration::class.java,
                HibernateJpaAutoConfiguration::class.java,
                TransactionAutoConfiguration::class.java,
                ObjsCoreAutoConfiguration::class.java,
            ),
        )
        .withPropertyValues(
            "spring.datasource.url=jdbc:h2:mem:objs-autoconfigure-wiring;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.flyway.enabled=false",
            "objs.seeds.enabled=false",
        )

    @Test
    fun shouldWirePersistenceBeans() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(UnitOfWork::class.java)
            assertThat(context.getBean(UnitOfWork::class.java))
                .isInstanceOf(TransactionTemplateUnitOfWork::class.java)
            assertThat(context).hasSingleBean(GraphStore::class.java)
            assertThat(context.getBean(SchemaCatalog::class.java))
                .isInstanceOf(JpaSchemaCatalog::class.java)
            assertThat(context).hasSingleBean(ObjsFlyway::class.java)
            assertThat(context.getBean(ObjsFlyway::class.java).flyway.configuration.table)
                .isEqualTo("flyway_schema_history_objs")
        }
    }

    @Test
    fun shouldPersistEntity_throughGraphStore() {
        contextRunner.run { context ->
            val schemas = context.getBean(SchemaCatalog::class.java)
            val store = context.getBean(GraphStore::class.java)
            schemas.register(
                Schema(
                    "Person",
                    "1",
                    SchemaDsl.obj(
                        "Person",
                        "Person payload",
                        listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Person name"))),
                    ),
                ),
            )
            val id = UUID.randomUUID()
            val result = store.write(
                Graph(
                    entities = mutableListOf(
                        Entity(
                            id = id,
                            type = "Person",
                            schemaVersion = "1",
                            payload = mutableMapOf("name" to "Ada"),
                            annotations = mutableMapOf(),
                        ),
                    ),
                ),
            )
            assertThat(result.isValid).isTrue()
            assertThat(store.getEntity(id)!!.payload["name"]).isEqualTo("Ada")
        }
    }
}
