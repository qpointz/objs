package org.poc.objs.core.persistence

import jakarta.persistence.EntityManagerFactory
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

/**
 * Second Flyway line for `bom_*` DDL. Runs before Boot Flyway and JPA validate.
 * Not registered as [Flyway] so Boot still creates the app Flyway bean.
 */
@AutoConfiguration(after = [DataSourceAutoConfiguration::class])
@AutoConfigureBefore(FlywayAutoConfiguration::class, HibernateJpaAutoConfiguration::class)
@ConditionalOnClass(Flyway::class, DataSource::class)
@ConditionalOnProperty(prefix = "objs.flyway", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ObjsFlywayProperties::class)
class ObjsFlywayAutoConfiguration {

    @Bean(name = ["objsFlyway"])
    fun objsFlyway(
        dataSource: DataSource,
        properties: ObjsFlywayProperties,
    ): ObjsFlyway {
        val jdbcUrl = dataSource.connection.use { it.metaData.url }
        val location = ObjsFlywayVendor.resolveLocations(jdbcUrl, properties.locations)
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(location)
            .table(properties.table)
            .load()
        flyway.migrate()
        return ObjsFlyway(flyway)
    }

    /**
     * Objs already created `bom_*`, so Boot Flyway sees a non-empty schema without
     * `flyway_schema_history`. Baseline at `0` so the app line can still apply its own `V1`.
     */
    @Bean
    fun objsFlywayBaselineAppHistory(): FlywayConfigurationCustomizer =
        FlywayConfigurationCustomizer { configuration ->
            configuration.baselineOnMigrate(true)
            configuration.baselineVersion("0")
        }

    companion object {
        @Bean
        @JvmStatic
        fun objsFlywayJpaDependsOn(): BeanFactoryPostProcessor = ObjsFlywayJpaDependsOnPostProcessor()

        @Bean
        @JvmStatic
        fun objsFlywayBootDependsOn(): BeanFactoryPostProcessor = ObjsFlywayBootDependsOnPostProcessor()
    }
}

internal class ObjsFlywayJpaDependsOnPostProcessor :
    AbstractDependsOnBeanFactoryPostProcessor(
        EntityManagerFactory::class.java,
        LocalContainerEntityManagerFactoryBean::class.java,
        "objsFlyway",
    )

internal class ObjsFlywayBootDependsOnPostProcessor :
    AbstractDependsOnBeanFactoryPostProcessor(
        FlywayMigrationInitializer::class.java,
        "objsFlyway",
    )
