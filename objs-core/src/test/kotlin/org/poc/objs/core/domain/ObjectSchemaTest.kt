package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.poc.objs.core.typed.PayloadMapper

class ObjectSchemaTest {

    @Test
    fun shouldProjectRecursiveDslToJsonSchema() {
        val definition = Schema(
            type = "Release",
            version = "1.0.0",
            contentSchema = SchemaDsl.obj(
                title = "Release",
                description = "A software release",
                fields = listOf(
                    SchemaDsl.field(
                        "name",
                        SchemaDsl.string("Name", "Release name"),
                    ),
                    SchemaDsl.field(
                        "build",
                        SchemaDsl.integer("Build", "Build number"),
                        required = false,
                    ),
                    SchemaDsl.field(
                        "publishedAt",
                        SchemaDsl.string("Published at", "Publication timestamp", format = "date-time"),
                        required = false,
                    ),
                    SchemaDsl.field(
                        "channels",
                        SchemaDsl.array(
                            "Channels",
                            "Release channels",
                            SchemaDsl.enum(
                                "Channel",
                                "One release channel",
                                listOf(
                                    EnumValue("stable", "Production-ready release"),
                                    EnumValue("preview", "Pre-release build"),
                                ),
                            ),
                        ),
                        stereotype = listOf("tags"),
                    ),
                ),
            ),
        )

        val jsonSchema = definition.toJsonSchema()
        assertThat(jsonSchema["\$schema"]).isEqualTo(JsonSchema.DIALECT)
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
        val normalized = SchemaNormalizer.normalizeStrict(
            Schema(
                "Thing",
                " 1 ",
                SchemaDsl.obj(
                    " Thing ",
                    " Thing payload ",
                    listOf(
                        SchemaDsl.field(
                            " name ",
                            SchemaDsl.string(" Name ", " Name value "),
                            stereotype = listOf(" tags ", "", "tags"),
                        ),
                        SchemaDsl.field(
                            "note",
                            SchemaDsl.string("Note", "Optional note"),
                            required = false,
                        ),
                    ),
                ),
            ),
        )

        assertThat(normalized.version).isEqualTo("1")
        assertThat(normalized.contentSchema.fields!!.filter { it.required }.map { it.name })
            .containsExactly("name")
        assertThat(normalized.contentSchema.fields!![0].name).isEqualTo("name")
        assertThat(normalized.contentSchema.fields!![0].stereotype).containsExactly("tags")
    }

    @Test
    fun shouldIgnoreLegacyObjectLevelRequiredListOnDeserialize() {
        val map = linkedMapOf<String, Any?>(
            "type" to "OBJECT",
            "title" to "Thing",
            "description" to "Thing payload",
            "fields" to listOf(
                linkedMapOf(
                    "name" to "name",
                    "required" to true,
                    "schema" to linkedMapOf(
                        "type" to "STRING",
                        "title" to "Name",
                        "description" to "Name value",
                    ),
                ),
            ),
            "required" to listOf("name"),
        )
        val node = PayloadMapper.fromMap(map, SchemaNode::class.java)
        assertThat(node.fields).hasSize(1)
        val roundTrip = PayloadMapper.toMap(node)
        assertThat(roundTrip).doesNotContainKey("required")
    }

    @Test
    fun shouldRoundTripAuthoritativeDefinitionThroughJsonMap() {
        val node = SchemaDsl.obj(
            "Artifact",
            "Artifact payload",
            listOf(
                SchemaDsl.field("size", SchemaDsl.integer("Size", "Size in bytes")),
            ),
        )
        val restored = PayloadMapper.fromMap(PayloadMapper.toMap(node), SchemaNode::class.java)
        assertThat(restored).isEqualTo(node)
    }

    @Test
    fun shouldRejectStructurallyInvalidDefinitions() {
        assertThatThrownBy {
            SchemaNormalizer.normalizeStrict(
                Schema(
                    "Broken",
                    "1",
                    SchemaNode(
                        type = SchemaType.ARRAY,
                        title = "Broken",
                        description = "Not an object root",
                    ),
                ),
            )
        }.isInstanceOf(SchemaDefinitionException::class.java)
            .hasMessageContaining("items is required")

        assertThatThrownBy {
            SchemaNormalizer.normalizeStrict(
                Schema(
                    "Broken",
                    "1",
                    SchemaDsl.obj(
                        "Broken",
                        "Duplicate fields",
                        listOf(
                            SchemaDsl.field("name", SchemaDsl.string("Name", "First")),
                            SchemaDsl.field("name", SchemaDsl.string("Name", "Second")),
                        ),
                    ),
                ),
            )
        }.isInstanceOf(SchemaDefinitionException::class.java)
            .hasMessageContaining("duplicate field name")
    }

    @Test
    fun shouldProjectIdentifierAndSearchableExtensions() {
        val definition = Schema(
            type = "Thing",
            version = "1",
            contentSchema = SchemaDsl.obj(
                "Thing",
                "Thing payload",
                listOf(
                    SchemaDsl.field(
                        "name",
                        SchemaDsl.string("Name", "Name value"),
                        identifier = true,
                        searchable = true,
                    ),
                    SchemaDsl.field(
                        "note",
                        SchemaDsl.string("Note", "Optional note"),
                        required = false,
                    ),
                ),
            ),
        )
        @Suppress("UNCHECKED_CAST")
        val properties = definition.toJsonSchema()["properties"] as Map<String, Map<String, Any?>>
        assertThat(properties["name"]!!["x-objs-identifier"]).isEqualTo(true)
        assertThat(properties["name"]!!["x-objs-searchable"]).isEqualTo(true)
        assertThat(properties["note"]!!).doesNotContainKey("x-objs-identifier")
    }

    @Test
    fun shouldRejectIdentifierOnObjectOrUnderArray() {
        assertThatThrownBy {
            SchemaNormalizer.normalizeStrict(
                Schema(
                    "Broken",
                    "1",
                    SchemaDsl.obj(
                        "Broken",
                        "Bad identifier",
                        listOf(
                            SchemaDsl.field(
                                "nested",
                                SchemaDsl.obj("Nested", "Nested object"),
                                identifier = true,
                            ),
                        ),
                    ),
                ),
            )
        }.isInstanceOf(SchemaDefinitionException::class.java)
            .hasMessageContaining("identifier is only allowed on scalar")

        assertThatThrownBy {
            SchemaNormalizer.normalizeStrict(
                Schema(
                    "Broken",
                    "1",
                    SchemaDsl.obj(
                        "Broken",
                        "Bad array identifier",
                        listOf(
                            SchemaDsl.field(
                                "items",
                                SchemaDsl.array(
                                    "Items",
                                    "List",
                                    SchemaDsl.obj(
                                        "Item",
                                        "One item",
                                        listOf(
                                            SchemaDsl.field(
                                                "id",
                                                SchemaDsl.string("Id", "Item id"),
                                                identifier = true,
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }.isInstanceOf(SchemaDefinitionException::class.java)
            .hasMessageContaining("not allowed under ARRAY")
    }

    @Test
    fun shouldAcceptOptionalEnumCaptionAndOmitBlank() {
        val normalized = SchemaNormalizer.normalizeStrict(
            Schema(
                type = "Thing",
                version = "1",
                contentSchema = SchemaDsl.obj(
                    "Thing",
                    "Thing payload",
                    listOf(
                        SchemaDsl.field(
                            "level",
                            SchemaDsl.enum(
                                "Level",
                                "Severity level",
                                listOf(
                                    EnumValue("LOW", "Limited impact", "Low"),
                                    EnumValue("HIGH", "Serious impact", "  "),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val values = normalized.contentSchema.fields!!.single().schema.values!!
        assertThat(values[0].caption).isEqualTo("Low")
        assertThat(values[1].caption).isNull()

        @Suppress("UNCHECKED_CAST")
        val projected = normalized.toJsonSchema()["properties"] as Map<String, Map<String, Any?>>
        val level = projected["level"]!!
        assertThat(level["enum"]).isEqualTo(listOf("LOW", "HIGH"))
        assertThat(level["x-objs-enumCaptions"]).isEqualTo(mapOf("LOW" to "Low"))
    }

    @Test
    fun shouldAcceptApplicationSpecificStringFormat() {
        val normalized = SchemaNormalizer.normalizeStrict(
            Schema(
                type = "Thing",
                version = "1",
                contentSchema = SchemaDsl.obj(
                    "Thing",
                    "Thing payload",
                    listOf(
                        SchemaDsl.field(
                            "purl",
                            SchemaDsl.string("PURL", "Package URL", format = "purl"),
                        ),
                    ),
                ),
            ),
        )
        assertThat(normalized.contentSchema.fields!!.single().schema.format).isEqualTo("purl")
    }
}
