package org.poc.objs.codegen.java

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.poc.objs.api.domain.Edge
import org.poc.objs.api.domain.Entity
import org.poc.objs.api.domain.Graph
import org.poc.objs.api.domain.MutationMode
import org.poc.objs.api.typed.PayloadMapper
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import java.net.URLClassLoader
import java.util.UUID
import javax.tools.ToolProvider

class JavaCodeGeneratorTest {
    @TempDir
    lateinit var tempDirectory: Path

    @Test
    fun shouldGenerateStableTypedBindings_andConsumeBothDefinitionSections() {
        val output = tempDirectory.resolve("generated")
        val report = JavaCodeGenerator().generate(document(), output, "com.example.generated")

        assertThat(report.generatedFiles.map { it.fileName.toString() }).containsExactly(
            "GeneratedNode.java",
            "GeneratedNodeHandle.java",
            "ReadNodeCapability.java",
            "MutationNodeCapability.java",
            "GeneratedRelationMetadata.java",
            "GraphMutationBuilder.java",
            "GeneratedReadView.java",
            "ProductRef.java",
            "ProductNode.java",
            "ProductReadNode.java",
            "ProductType.java",
        )
        assertThat(Files.readString(output.resolve("com/example/generated/ProductNode.java")))
            .contains("extends GeneratedNode<ProductDto>")
            .contains("implements ReadNodeCapability, MutationNodeCapability")
            .contains("new EntityTypeMeta(\"Product\", \"1.0.0\", null)")
        assertThat(Files.readString(output.resolve("com/example/generated/GeneratedRelationMetadata.java")))
            .contains("RELATED")
        val builderSource = Files.readString(output.resolve("com/example/generated/GraphMutationBuilder.java"))
        assertThat(builderSource)
            .contains("addProduct")
            .contains("related")

        val first = Files.readAllBytes(output.resolve("com/example/generated/ProductNode.java"))
        JavaCodeGenerator().generate(document(), output, "com.example.generated")
        assertThat(Files.readAllBytes(output.resolve("com/example/generated/ProductNode.java")))
            .isEqualTo(first)
    }

    @Test
    fun shouldGenerateJavaThatCompilesAgainstObjsApi() {
        val output = tempDirectory.resolve("generated")
        JavaCodeGenerator().generate(document(), output, "com.example.generated")
        val dto = output.resolve("com/example/generated/ProductDto.java")
        Files.writeString(
            dto,
            """
                package com.example.generated;
                public class ProductDto {
                }
            """.trimIndent(),
        )
        val classes = tempDirectory.resolve("classes")
        Files.createDirectories(classes)
        val sourceFiles = Files.walk(output).use { stream ->
            stream.filter { it.toString().endsWith(".java") }.toList()
        }
        val compiler = ToolProvider.getSystemJavaCompiler()
        requireNotNull(compiler) { "A JDK compiler is required for generator tests" }
        val result = compiler.run(
            null,
            null,
            null,
            "-classpath",
            System.getProperty("java.class.path"),
            "-d",
            classes.toString(),
            *sourceFiles.map { it.toString() }.toTypedArray(),
        )
        assertThat(result).isZero()
    }

    @Test
    fun shouldBuildEntitiesAndAllowedBareEdges_withoutPersistence() {
        val output = tempDirectory.resolve("generated")
        JavaCodeGenerator().generate(document(), output, "com.example.generated")
        val dto = output.resolve("com/example/generated/ProductDto.java")
        Files.writeString(dto, "package com.example.generated; public class ProductDto {}")
        val classes = tempDirectory.resolve("classes")
        Files.createDirectories(classes)
        val sourceFiles = Files.walk(output).use { stream ->
            stream.filter { it.toString().endsWith(".java") }.toList()
        }
        val compiler = ToolProvider.getSystemJavaCompiler()
        requireNotNull(compiler)
        assertThat(
            compiler.run(
                null,
                null,
                null,
                "-classpath",
                System.getProperty("java.class.path"),
                "-d",
                classes.toString(),
                *sourceFiles.map { it.toString() }.toTypedArray(),
            ),
        ).isZero()

        URLClassLoader(arrayOf(classes.toUri().toURL()), javaClass.classLoader).use { loader ->
            val dtoClass = loader.loadClass("com.example.generated.ProductDto")
            val nodeClass = loader.loadClass("com.example.generated.ProductNode")
            val builderClass = loader.loadClass("com.example.generated.GraphMutationBuilder")
            val builder = builderClass
                .getConstructor(PayloadMapper::class.java)
                .newInstance(PayloadMapper(JsonMapper.builder().build()))
            val add = builderClass.getMethod("addProduct", UUID::class.java, dtoClass)
            val firstId = UUID.randomUUID()
            val secondId = UUID.randomUUID()
            val first = add.invoke(builder, firstId, dtoClass.getConstructor().newInstance())
            val second = add.invoke(builder, secondId, dtoClass.getConstructor().newInstance())
            val relation = builderClass.getMethod("related", nodeClass, nodeClass)
            val edge = relation.invoke(builder, first, second)
            val mutation = builderClass.getMethod("build").invoke(builder)

            assertThat(edge.javaClass.getMethod("getProperties").invoke(edge)).isNull()
            val entityMutation = mutation.javaClass.getMethod("getEntities").invoke(mutation)
            val edgeMutation = mutation.javaClass.getMethod("getEdges").invoke(mutation)
            @Suppress("UNCHECKED_CAST")
            assertThat(entityMutation.javaClass.getMethod("getSet").invoke(entityMutation) as List<*>).hasSize(2)
            @Suppress("UNCHECKED_CAST")
            assertThat(edgeMutation.javaClass.getMethod("getSet").invoke(edgeMutation) as List<*>).hasSize(1)
            assertThat(mutation.javaClass.getMethod("getMode").invoke(mutation)).isEqualTo(MutationMode.MERGE)

            @Suppress("UNCHECKED_CAST")
            val graph = Graph(
                (entityMutation.javaClass.getMethod("getSet").invoke(entityMutation) as List<Entity>)
                    .toMutableList(),
                (edgeMutation.javaClass.getMethod("getSet").invoke(edgeMutation) as List<Edge>)
                    .toMutableList(),
            )
            val readViewClass = loader.loadClass("com.example.generated.GeneratedReadView")
            val readView = readViewClass
                .getMethod("from", Graph::class.java, PayloadMapper::class.java)
                .invoke(readViewClass, graph, PayloadMapper(JsonMapper.builder().build()))
            val products = readViewClass.getMethod("products").invoke(readView)
            val firstProduct = products.javaClass.getMethod("get", Int::class.java).invoke(products, 0)
            val related = firstProduct.javaClass.getMethod("getRelateds").invoke(firstProduct)
            assertThat(products.javaClass.getMethod("getSize").invoke(products)).isEqualTo(2)
            assertThat(related.javaClass.getMethod("getSize").invoke(related)).isEqualTo(1)

            assertThatThrownBy {
                add.invoke(builder, firstId, dtoClass.getConstructor().newInstance())
            }.hasRootCauseInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun shouldGenerateSchemaPropertyRelation_signatureAndEmptyPolicy() {
        val document = document().toMutableMap()
        document["\$defs"] = mapOf(
            "Product" to mapOf("type" to "object", "properties" to emptyMap<String, Any?>()),
            "ProductEdge" to mapOf("type" to "object", "properties" to emptyMap<String, Any?>()),
        )
        document["x-objs-relations"] = listOf(
            mapOf(
                "sourceType" to "Product",
                "role" to "HAS_EDGE",
                "targetType" to "Product",
                "sourceDefinition" to "Product",
                "targetDefinition" to "Product",
                "propertiesPolicy" to "SCHEMA",
                "emptyPropertiesAllowed" to false,
                "propertySchema" to mapOf(
                    "resolved" to true,
                    "type" to "ProductEdge",
                    "schemaVersion" to "1.0.0",
                    "definitionKey" to "ProductEdge",
                ),
                "codegen" to mapOf(
                    "outboundMethod" to "hasEdge",
                    "inboundMethod" to "hasEdgeFromProduct",
                    "sourceStaticBinding" to true,
                    "targetStaticBinding" to true,
                ),
            ),
        )
        @Suppress("UNCHECKED_CAST")
        val codegen = (document["x-objs-codegen"] as Map<String, Any?>).toMutableMap()
        @Suppress("UNCHECKED_CAST")
        val definitions = (codegen["definitions"] as List<Map<String, Any?>>).toMutableList()
        definitions += mapOf(
            "definitionKey" to "ProductEdge",
            "kind" to "EDGE_PROPERTIES",
            "type" to "ProductEdge",
            "schemaVersion" to "1.0.0",
            "generated" to true,
            "skip" to false,
            "javaTypeName" to "ProductEdge",
            "interfaces" to emptyList<String>(),
        )
        codegen["definitions"] = definitions
        document["x-objs-codegen"] = codegen

        val output = tempDirectory.resolve("generated")
        JavaCodeGenerator().generate(document, output, "com.example.generated")
        val builder = Files.readString(output.resolve("com/example/generated/GraphMutationBuilder.java"))

        assertThat(builder).contains("ProductEdge properties")
        assertThat(builder).contains("UUID edgeId")
        assertThat(builder).doesNotContain("hasEdge(ProductNode source, ProductNode target)")
    }

    @Test
    fun shouldApplyInheritanceMetadata_andPreserveDiagnostics() {
        val document = document().toMutableMap()
        @Suppress("UNCHECKED_CAST")
        val codegen = (document["x-objs-codegen"] as Map<String, Any?>).toMutableMap()
        @Suppress("UNCHECKED_CAST")
        val definitions = (codegen["definitions"] as List<Map<String, Any?>>).map {
            if (it["type"] == "Product") {
                it + mapOf(
                    "baseClass" to "com.example.BaseNode",
                    "interfaces" to listOf("com.example.Named", "com.example.Named"),
                )
            } else {
                it
            }
        }
        codegen["definitions"] = definitions
        document["x-objs-codegen"] = codegen

        val report = JavaCodeGenerator().generate(document, tempDirectory.resolve("generated"), "com.example")

        assertThat(report.diagnostics).containsExactly("edge schema unresolved")
        assertThat(Files.readString(tempDirectory.resolve("generated/com/example/ProductNode.java")))
            .contains("extends com.example.BaseNode")
            .contains("implements com.example.Named, ReadNodeCapability, MutationNodeCapability")
    }

    @Test
    fun shouldRejectMalformedMetadata_andRootModuleOutput() {
        assertThatThrownBy {
            JavaCodeGenerator().generate(
                mapOf("\$defs" to emptyMap<String, Any?>()),
                tempDirectory.resolve("generated"),
                "com.example",
            )
        }
            .isInstanceOf(JavaCodegenException::class.java)
            .hasMessageContaining("missing root x-objs-codegen")

        assertThatThrownBy {
            JavaCodeGenerator().generate(
                document(),
                Path.of("objs-core", "build", "generated"),
                "com.example",
            )
        }
            .isInstanceOf(JavaCodegenException::class.java)
            .hasMessageContaining("cannot be under a root objs-* module")
    }

    private fun document(): Map<String, Any?> = mapOf(
        "\$schema" to "https://json-schema.org/draft/2020-12/schema",
        "properties" to mapOf("Product" to mapOf("\$ref" to "#/\$defs/Product")),
        "\$defs" to mapOf(
            "Product" to mapOf("type" to "object", "properties" to emptyMap<String, Any?>()),
        ),
        "x-objs-relations" to listOf(
            mapOf(
                "sourceType" to "Product",
                "role" to "RELATED",
                "targetType" to "Product",
                "sourceDefinition" to "Product",
                "targetDefinition" to "Product",
                "propertySchema" to mapOf("type" to "MissingEdge"),
                "codegen" to mapOf(
                    "outboundMethod" to "related",
                    "inboundMethod" to "relatedFromProduct",
                    "sourceStaticBinding" to true,
                    "targetStaticBinding" to true,
                ),
            ),
        ),
        "x-objs-codegen" to mapOf(
            "definitions" to listOf(
                mapOf(
                    "definitionKey" to "Product",
                    "kind" to "ENTITY",
                    "type" to "Product",
                    "schemaVersion" to "1.0.0",
                    "generated" to true,
                    "skip" to false,
                    "javaTypeName" to "ProductDto",
                    "interfaces" to emptyList<String>(),
                ),
            ),
            "diagnostics" to listOf(mapOf("message" to "edge schema unresolved")),
        ),
    )
}
