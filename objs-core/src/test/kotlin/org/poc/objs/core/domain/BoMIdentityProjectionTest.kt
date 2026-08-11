package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BoMIdentityProjectionTest {

    @Test
    fun shouldProjectNestedIdentifierLeavesWithDottedPaths() {
        val schema = BoMSchemaDsl.obj(
            "Thing",
            "Thing payload",
            listOf(
                BoMSchemaDsl.field(
                    "name",
                    BoMSchemaDsl.string("Name", "Name"),
                    identifier = true,
                ),
                BoMSchemaDsl.field(
                    "description",
                    BoMSchemaDsl.string("Description", "Description"),
                    required = false,
                ),
                BoMSchemaDsl.field(
                    "source",
                    BoMSchemaDsl.obj(
                        "Source",
                        "External source",
                        listOf(
                            BoMSchemaDsl.field(
                                "system",
                                BoMSchemaDsl.string("System", "System"),
                                identifier = true,
                            ),
                            BoMSchemaDsl.field(
                                "id",
                                BoMSchemaDsl.string("Id", "Id"),
                                identifier = true,
                            ),
                        ),
                    ),
                    required = false,
                ),
                BoMSchemaDsl.field(
                    "tags",
                    BoMSchemaDsl.array(
                        "Tags",
                        "Tags",
                        BoMSchemaDsl.string("Tag", "Tag"),
                    ),
                    required = false,
                ),
            ),
        )
        val payload = mapOf(
            "name" to "alpha",
            "description" to "ignored",
            "source" to mapOf("system" to "jira", "id" to "ABC-1"),
            "tags" to listOf("a", "b"),
        )
        assertThat(BoMIdentityProjection.project(schema, payload)).isEqualTo(
            linkedMapOf(
                "name" to "alpha",
                "source.system" to "jira",
                "source.id" to "ABC-1",
            ),
        )
    }

    @Test
    fun shouldOmitAbsentIdentifierKeysAndReturnEmptyWhenNoneMarked() {
        val withIds = BoMSchemaDsl.obj(
            "Thing",
            "Thing",
            listOf(
                BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Name"), identifier = true),
                BoMSchemaDsl.field("note", BoMSchemaDsl.string("Note", "Note"), required = false, identifier = true),
            ),
        )
        assertThat(BoMIdentityProjection.project(withIds, mapOf("name" to "x")))
            .isEqualTo(linkedMapOf("name" to "x"))

        val none = BoMSchemaDsl.obj(
            "Thing",
            "Thing",
            listOf(BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Name"))),
        )
        assertThat(BoMIdentityProjection.project(none, mapOf("name" to "x"))).isEmpty()
    }

    @Test
    fun shouldOmitNullAndBlankIdentifierValues() {
        val schema = BoMSchemaDsl.obj(
            "Thing",
            "Thing",
            listOf(
                BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Name"), identifier = true),
                BoMSchemaDsl.field("code", BoMSchemaDsl.string("Code", "Code"), identifier = true),
            ),
        )
        assertThat(
            BoMIdentityProjection.project(
                schema,
                mapOf("name" to "  ", "code" to null),
            ),
        ).isEmpty()
        assertThat(BoMIdentityProjection.isUnset(null)).isTrue()
        assertThat(BoMIdentityProjection.isUnset("")).isTrue()
        assertThat(BoMIdentityProjection.isUnset(" \t")).isTrue()
        assertThat(BoMIdentityProjection.isUnset("x")).isFalse()
    }

    @Test
    fun shouldListIdentifierPathsWithoutRequiringValues() {
        val schema = BoMSchemaDsl.obj(
            "Thing",
            "Thing",
            listOf(
                BoMSchemaDsl.field("name", BoMSchemaDsl.string("Name", "Name"), identifier = true),
                BoMSchemaDsl.field("note", BoMSchemaDsl.string("Note", "Note"), required = false),
                BoMSchemaDsl.field(
                    "source",
                    BoMSchemaDsl.obj(
                        "Source",
                        "Source",
                        listOf(
                            BoMSchemaDsl.field(
                                "id",
                                BoMSchemaDsl.string("Id", "Id"),
                                identifier = true,
                            ),
                        ),
                    ),
                    required = false,
                ),
            ),
        )
        assertThat(BoMIdentityProjection.identifierPaths(schema))
            .containsExactly("name", "source.id")
    }
}
