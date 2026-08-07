package org.poc.objs.sbom.generated

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Smoke-test that jsonschema2pojo output from the linked catalog schema carries
 * relation properties (intended eventual replacement for Wave* payload models).
 */
class GeneratedSbomModelTest {
    @Test
    fun shouldExposeLinkedDatabaseDatasetRelations() {
        val dataset = Dataset()
            .withName("orders")
            .withDatasetType("table")
        val database = Database()
            .withName("payments-db")
            .withEngine("postgres")
            .withContainsDataset(listOf(dataset))
        dataset.containsFromDatabase = database

        assertThat(database.containsDataset).containsExactly(dataset)
        assertThat(dataset.containsFromDatabase).isSameAs(database)
        assertThat(database.name).isEqualTo("payments-db")
    }

    @Test
    fun shouldExposeProductPayloadFields() {
        val product = Product()
            .withName("app")
            .withVersion("1.0.0")
        assertThat(product.name).isEqualTo("app")
        assertThat(product.version).isEqualTo("1.0.0")
    }
}
