package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.core.typed.PayloadMapper

class BoMObjectSchemaTest {

    @Test
    fun shouldProjectRecursiveDslToJsonSchema() {
        val definition = BoMSchema(
            type = "Release",
            version = "1.0.0",
            contentSchema = BoMSchemaDsl.obj(
                title = "Release",
                description = "A software release",
                fields = listOf(
                    BoMSchemaDsl.field(
                        "name",
                        BoMSchemaDsl.string("Name", "Release name"),
                    ),
                    BoMSchemaDsl.field(
                        "build",
                        BoMSchemaDsl.integer("Build", "Build number"),
                        required = false,
                    ),
                    BoMSchemaDsl.field(
                        "publishedAt",
                        BoMSchemaDsl.string("Published at", "Publication timestamp", format = "date-time"),
                        required = false,
                    ),
                    BoMSchemaDsl.field(
                        "channels",
                        BoMSchemaDsl.array(
                            "Channels",
                            "Release channels",
                            BoMSchemaDsl.enum(
                                "Channel",
                                "One release channel",
                                listOf(
                                    BoMEnumValue("stable", "Production-ready release"),
                                    BoMEnumValue("preview", "Pre-release build"),
                                ),
                            ),
                        ),
                        stereotype = listOf("tags"),
                    ),
                ),
            ),
        )

        val jsonSchema = definition.toJsonSchema()
        assertThat(jsonSchema["\$schema"]).isEqualTo(BoMJsonSchema.DIALECT)
        assertThat(jsonSchema["x-objs-type"]).isEqualTo("Release")
        assertThat(jsonSchema["type"]).isEqualTo("object")
        assertThat(jsonSchema["required"]).isEqualTo(listOf("name", "channels"))
        assertThat(jsonSchema["additionalProperties"]).isEqualTo(true)

        @Suppress("UNCHECKED_CAST")
        val properties = jsonSchema["properties"] as Map<String, Map<String, Any?>>
        assertThat(properties["build"]!!["type"]).isEqualTo("integer")
        assertThat(properties["publishedAt"]!!["format"]).isEqualTo("date-time")
        assertThat(properties["channels"]!!["x-objs-stereotype"]).isEqualTo(listOf("tags"))

        @Suppress("UNCHECKED_CAST")
        val channelItems = properties["channels"]!!["items"] as Map<String, Any?>
        assertThat(channelItems["enum"]).isEqualTo(listOf("stable", "preview"))
        assertThat(channelItems["x-objs-enumDescriptions"]).isEqualTo(
            mapOf(
                "stable" to "Production-ready release",
                "preview" to "Pre-release build",
            ),
        )
    }

    @Test
    fun shouldNormalizeRequiredFieldsAndStereotypes() {
        val normalized = BoMSchemaNormalizer.normalizeStrict(
            BoMSchema(
                "Thing",
                " 1 ",
                BoMSchemaDsl.obj(
                    " Thing ",
                    " Thing payload ",
                    listOf(
                        BoMSchemaDsl.field(
                            " name ",
                            BoMSchemaDsl.string(" Name ", " Name value "),
                            stereotype = listOf(" tags ", "", "tags"),
                        ),
                        BoMSchemaDsl.field(
                            "note",
                            BoMSchemaDsl.string("Note", "Optional note"),
                            required = false,
                        ),
                    ),
                ),
            ),
        )

        assertThat(normalized.version).isEqualTo("1")
        assertThat(normalized.contentSchema.required).containsExactly("name")
        assertThat(normalized.contentSchema.fields!![0].name).isEqualTo("name")
        assertThat(normalized.contentSchema.fields!![0].stereotype).containsExactly("tags")
    }

    @Test
    fun shouldRoundTripAuthoritativeDefinitionThroughJsonMap() {
        val node = BoMSchemaDsl.obj(
            "Artifact",
            "Artifact payload",
            listOf(
                BoMSchemaDsl.field("size", BoMSchemaDsl.integer("Size", "Size in bytes")),
            ),
        )
        val restored = PayloadMapper.fromMap(PayloadMapper.toMap(node), BoMSchemaNode::class.java)
        assertThat(restored).isEqualTo(node)
    }

    @Test
    fun shouldRejectStructurallyInvalidDefinitions() {
        assertThatThrownBy {
            BoMSchemaNormalizer.normalizeStrict(
                BoMSchema(
                    "Broken",
                    "1",
                    BoMSchemaNode(
                        type = BoMSchemaType.ARRAY,
                        title = "Broken",
                        description = "Not an object root",
                    ),
                ),
            )
        }.isInstanceOf(BoMSchemaDefinitionException::class.java)
            .hasMessageContaining("items is required")

        assertThatThrownBy {
            BoMSchemaNormalizer.normalizeStrict(
                BoMSchema(
                    "Broken",
                    "1",
                    BoMSchemaDsl.obj(
                        "Broken",
                        "Duplicate fields",
                        listOf(
                            BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "First")),
                            BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Second")),
                        ),
                    ),
                ),
            )
        }.isInstanceOf(BoMSchemaDefinitionException::class.java)
            .hasMessageContaining("duplicate field name")
    }
}
