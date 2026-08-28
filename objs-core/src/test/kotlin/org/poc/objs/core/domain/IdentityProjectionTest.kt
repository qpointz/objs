package org.poc.objs.core.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IdentityProjectionTest {

    @Test
    fun shouldProjectNestedIdentifierLeavesWithDottedPaths() {
        val schema = SchemaDsl.obj(
            "Thing",
            "Thing payload",
            listOf(
                SchemaDsl.field(
                    "name",
                    SchemaDsl.string("Name", "Name"),
                    identifier = true,
                ),
                SchemaDsl.field(
                    "description",
                    SchemaDsl.string("Description", "Description"),
                    required = false,
                ),
                SchemaDsl.field(
                    "source",
                    SchemaDsl.obj(
                        "Source",
                        "External source",
                        listOf(
                            SchemaDsl.field(
                                "system",
                                SchemaDsl.string("System", "System"),
                                identifier = true,
                            ),
                            SchemaDsl.field(
                                "id",
                                SchemaDsl.string("Id", "Id"),
                                identifier = true,
                            ),
                        ),
                    ),
                    required = false,
                ),
                SchemaDsl.field(
                    "tags",
                    SchemaDsl.array(
                        "Tags",
                        "Tags",
                        SchemaDsl.string("Tag", "Tag"),
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
        assertThat(IdentityProjection.project(schema, payload)).isEqualTo(
            linkedMapOf(
                "name" to "alpha",
                "source.system" to "jira",
                "source.id" to "ABC-1",
            ),
        )
    }

    @Test
    fun shouldOmitAbsentIdentifierKeysAndReturnEmptyWhenNoneMarked() {
        val withIds = SchemaDsl.obj(
            "Thing",
            "Thing",
            listOf(
                SchemaDsl.field("name", SchemaDsl.string("Name", "Name"), identifier = true),
                SchemaDsl.field("note", SchemaDsl.string("Note", "Note"), required = false, identifier = true),
            ),
        )
        assertThat(IdentityProjection.project(withIds, mapOf("name" to "x")))
            .isEqualTo(linkedMapOf("name" to "x"))

        val none = SchemaDsl.obj(
            "Thing",
            "Thing",
            listOf(SchemaDsl.field("name", SchemaDsl.string("Name", "Name"))),
        )
        assertThat(IdentityProjection.project(none, mapOf("name" to "x"))).isEmpty()
    }

    @Test
    fun shouldOmitNullAndBlankIdentifierValues() {
        val schema = SchemaDsl.obj(
            "Thing",
            "Thing",
            listOf(
                SchemaDsl.field("name", SchemaDsl.string("Name", "Name"), identifier = true),
                SchemaDsl.field("code", SchemaDsl.string("Code", "Code"), identifier = true),
            ),
        )
        assertThat(
            IdentityProjection.project(
                schema,
                mapOf("name" to "  ", "code" to null),
            ),
        ).isEmpty()
        assertThat(IdentityProjection.isUnset(null)).isTrue()
        assertThat(IdentityProjection.isUnset("")).isTrue()
        assertThat(IdentityProjection.isUnset(" \t")).isTrue()
        assertThat(IdentityProjection.isUnset("x")).isFalse()
    }

    @Test
    fun shouldListIdentifierPathsWithoutRequiringValues() {
        val schema = SchemaDsl.obj(
            "Thing",
            "Thing",
            listOf(
                SchemaDsl.field("name", SchemaDsl.string("Name", "Name"), identifier = true),
                SchemaDsl.field("note", SchemaDsl.string("Note", "Note"), required = false),
                SchemaDsl.field(
                    "source",
                    SchemaDsl.obj(
                        "Source",
                        "Source",
                        listOf(
                            SchemaDsl.field(
                                "id",
                                SchemaDsl.string("Id", "Id"),
                                identifier = true,
                            ),
                        ),
                    ),
                    required = false,
                ),
            ),
        )
        assertThat(IdentityProjection.identifierPaths(schema))
            .containsExactly("name", "source.id")
    }
}
