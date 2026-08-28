package org.poc.objs.codegen.java

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

data class JavaCodegenOptions(
    val schemaFile: Path,
    val outputDirectory: Path,
    val targetPackage: String,
)

data class JavaGenerationReport(
    val generatedFiles: List<Path>,
    val diagnostics: List<String>,
)

class JavaCodegenException(message: String) : IllegalArgumentException(message)

/**
 * Generates the application-owned Java binding layer from a codegen-only Objs export.
 *
 * The input document is deliberately represented as generic JSON. The generator consumes the
 * standard dialect-native definitions plus the Objs `x-objs-codegen` and `x-objs-relations`
 * extensions, but never requires those extensions inside a payload schema.
 */
class JavaCodeGenerator(
    private val mapper: ObjectMapper = JsonMapper.builder().build(),
) {
    fun generate(options: JavaCodegenOptions): JavaGenerationReport {
        require(Files.isRegularFile(options.schemaFile)) {
            "Codegen schema file does not exist: ${options.schemaFile}"
        }
        @Suppress("UNCHECKED_CAST")
        val document = mapper.readValue(options.schemaFile.toFile(), Map::class.java) as Map<String, Any?>
        return generate(document, options.outputDirectory, options.targetPackage)
    }

    fun generate(
        document: Map<String, Any?>,
        outputDirectory: Path,
        targetPackage: String,
    ): JavaGenerationReport {
        validatePackageName(targetPackage)
        validateOutputDirectory(outputDirectory)

        val defsKeyword = when {
            document.containsKey("\$defs") -> "\$defs"
            document.containsKey("definitions") -> "definitions"
            else -> throw JavaCodegenException(
                "Codegen schema must contain either '\$defs' (2020-12) or 'definitions' (draft-07)",
            )
        }
        val defs = map(document[defsKeyword])
            ?: throw JavaCodegenException("Codegen schema '$defsKeyword' must be an object")
        val codegen = map(document["x-objs-codegen"])
            ?: throw JavaCodegenException("Codegen schema is missing root x-objs-codegen metadata")
        val relationValues = list(document["x-objs-relations"])
            ?: throw JavaCodegenException("Codegen schema is missing root x-objs-relations metadata")
        val definitions = parseDefinitions(codegen["definitions"])
        val entities = definitions.filter { it.kind == "ENTITY" && it.generated && !it.skip }
        validateDefinitions(entities, defs)
        val diagnostics = mutableListOf<String>()
        diagnostics += parseDiagnostics(codegen["diagnostics"])
        val relations = parseRelations(relationValues, definitions, diagnostics)

        Files.createDirectories(outputDirectory)
        val packageDirectory = outputDirectory.resolve(targetPackage.replace('.', '/'))
        Files.createDirectories(packageDirectory)
        val files = mutableListOf<Path>()
        fun write(name: String, source: String) {
            val path = packageDirectory.resolve(name)
            writeIfChanged(path, source)
            files.add(path)
        }

        write("GeneratedNode.java", generatedNodeSource(targetPackage))
        write("GeneratedNodeHandle.java", nodeHandleSource(targetPackage))
        write("ReadNodeCapability.java", capabilitySource(targetPackage, "ReadNodeCapability"))
        write("MutationNodeCapability.java", capabilitySource(targetPackage, "MutationNodeCapability"))
        write("GeneratedRelationMetadata.java", relationMetadataSource(targetPackage, relations))
        write("GraphMutationBuilder.java", mutationBuilderSource(targetPackage, entities, relations))
        write("GeneratedReadView.java", readViewSource(targetPackage, entities))
        for (entity in entities.sortedBy { it.definitionKey }) {
            write("${entity.definitionKey}Ref.java", refSource(targetPackage, entity))
            write("${entity.definitionKey}Node.java", nodeSource(targetPackage, entity))
            write(
                "${entity.definitionKey}ReadNode.java",
                readNodeSource(targetPackage, entity, entities, relations),
            )
            write("${entity.definitionKey}Type.java", typeSource(targetPackage, entity))
        }
        return JavaGenerationReport(files, diagnostics)
    }

    private fun parseDefinitions(raw: Any?): List<DefinitionSpec> =
        list(raw)?.mapIndexed { index, value ->
            val entry = map(value)
                ?: throw JavaCodegenException("x-objs-codegen.definitions[$index] must be an object")
            DefinitionSpec(
                definitionKey = requiredString(entry, "definitionKey", "x-objs-codegen.definitions[$index]"),
                kind = requiredString(entry, "kind", "x-objs-codegen.definitions[$index]"),
                type = requiredString(entry, "type", "x-objs-codegen.definitions[$index]"),
                schemaVersion = requiredString(
                    entry,
                    "schemaVersion",
                    "x-objs-codegen.definitions[$index]",
                ),
                generated = boolean(entry, "generated", false),
                skip = boolean(entry, "skip", false),
                javaTypeName = validJavaIdentifier(
                    requiredString(entry, "javaTypeName", "x-objs-codegen.definitions[$index]"),
                    "x-objs-codegen.definitions[$index].javaTypeName",
                ),
                baseClass = optionalJavaType(entry["baseClass"], "x-objs-codegen.definitions[$index].baseClass"),
                interfaces = parseInterfaces(entry["interfaces"], "x-objs-codegen.definitions[$index].interfaces"),
            )
        } ?: throw JavaCodegenException("x-objs-codegen.definitions must be an array")

    private fun validateDefinitions(entities: List<DefinitionSpec>, defs: Map<String, Any?>) {
        val definitionKeys = mutableMapOf<String, String>()
        val javaNames = mutableMapOf<String, String>()
        for (entity in entities) {
            if (entity.definitionKey !in defs) {
                throw JavaCodegenException(
                    "Entity definition '${entity.definitionKey}' for ${entity.type}@${entity.schemaVersion} " +
                        "is missing from the dialect definitions",
                )
            }
            val previousKey = definitionKeys.putIfAbsent(entity.definitionKey, entity.type)
            if (previousKey != null && previousKey != entity.type) {
                throw JavaCodegenException(
                    "Definition key collision '${entity.definitionKey}' for '$previousKey' and '${entity.type}'",
                )
            }
            val owner = "${entity.type}@${entity.schemaVersion}"
            val previousJavaName = javaNames.putIfAbsent(entity.javaTypeName, owner)
            if (previousJavaName != null && previousJavaName != owner) {
                throw JavaCodegenException(
                    "Java type name collision '${entity.javaTypeName}' for '$previousJavaName' and '$owner'",
                )
            }
        }
    }

    private fun parseRelations(
        values: List<Any?>,
        definitions: List<DefinitionSpec>,
        diagnostics: MutableList<String>,
    ): List<RelationSpec> {
        val definitionsByKey = definitions.associateBy { it.definitionKey }
        val methodOwners = mutableMapOf<String, String>()
        return values.mapIndexed { index, value ->
            val entry = map(value)
                ?: throw JavaCodegenException("x-objs-relations[$index] must be an object")
            val codegen = map(entry["codegen"])
                ?: throw JavaCodegenException("x-objs-relations[$index].codegen must be an object")
            val sourceType = requiredString(entry, "sourceType", "x-objs-relations[$index]")
            val targetType = requiredString(entry, "targetType", "x-objs-relations[$index]")
            val role = requiredString(entry, "role", "x-objs-relations[$index]")
            val sourceDefinition = optionalString(entry["sourceDefinition"])
            val targetDefinition = optionalString(entry["targetDefinition"])
            val sourceStatic = boolean(codegen, "sourceStaticBinding", false)
            val targetStatic = boolean(codegen, "targetStaticBinding", false)
            if (sourceStatic && sourceDefinition == null) {
                throw JavaCodegenException(
                    "x-objs-relations[$index] declares a static source binding without a source definition",
                )
            }
            if (targetStatic && targetDefinition == null) {
                throw JavaCodegenException(
                    "x-objs-relations[$index] declares a static target binding without a target definition",
                )
            }
            if (sourceDefinition != null && sourceDefinition !in definitionsByKey) {
                diagnostics += "Unresolved source definition '$sourceDefinition' for relation " +
                    "($sourceType, $role, $targetType)"
            }
            if (targetDefinition != null && targetDefinition !in definitionsByKey) {
                diagnostics += "Unresolved target definition '$targetDefinition' for relation " +
                    "($sourceType, $role, $targetType)"
            }
            val outboundMethod = validJavaIdentifier(
                requiredString(codegen, "outboundMethod", "x-objs-relations[$index].codegen"),
                "x-objs-relations[$index].codegen.outboundMethod",
            )
            val inboundMethod = validJavaIdentifier(
                requiredString(codegen, "inboundMethod", "x-objs-relations[$index].codegen"),
                "x-objs-relations[$index].codegen.inboundMethod",
            )
            if (sourceStatic) registerMethod(methodOwners, sourceType, outboundMethod, sourceType, role, targetType)
            if (targetStatic) registerMethod(methodOwners, targetType, inboundMethod, sourceType, role, targetType)
            val propertySchema = map(entry["propertySchema"])
            val propertyDefinition = propertySchema?.get("definitionKey")?.toString()
                ?.let(definitionsByKey::get)
            val policy = optionalString(entry["propertiesPolicy"]) ?: "NONE"
            if (policy !in setOf("NONE", "SCHEMA")) {
                throw JavaCodegenException(
                    "x-objs-relations[$index].propertiesPolicy must be NONE or SCHEMA",
                )
            }
            RelationSpec(
                sourceType = sourceType,
                role = role,
                targetType = targetType,
                sourceDefinition = sourceDefinition,
                targetDefinition = targetDefinition,
                outboundMethod = outboundMethod,
                inboundMethod = inboundMethod,
                sourceStaticBinding = sourceStatic && sourceDefinition in definitionsByKey,
                targetStaticBinding = targetStatic && targetDefinition in definitionsByKey,
                cardinality = optionalString(entry["cardinality"]) ?: "UNSPECIFIED",
                noInverse = boolean(codegen, "noInverse", false),
                skip = boolean(codegen, "skip", false),
                propertiesPolicy = policy,
                emptyPropertiesAllowed = boolean(entry, "emptyPropertiesAllowed", true),
                edgePropertyType = propertySchema?.get("type")?.toString(),
                edgePropertySchemaVersion = propertySchema?.get("schemaVersion")?.toString(),
                edgePropertyJavaType = propertyDefinition
                    ?.takeIf { it.kind == "EDGE_PROPERTIES" }
                    ?.javaTypeName,
            )
        }
    }

    private fun registerMethod(
        owners: MutableMap<String, String>,
        owner: String,
        method: String,
        sourceType: String,
        role: String,
        targetType: String,
    ) {
        val relation = "($sourceType, $role, $targetType)"
        val previous = owners.putIfAbsent("$owner#$method", relation)
        if (previous != null && previous != relation) {
            throw JavaCodegenException(
                "Java relation method collision '$method' for '$owner': $previous and $relation",
            )
        }
    }

    private fun generatedNodeSource(packageName: String): String = """
        package $packageName;

        import java.util.LinkedHashMap;
        import java.util.UUID;
        import org.poc.objs.api.typed.EntityTypeMeta;
        import org.poc.objs.api.typed.TypedEntity;

        /** Common identity/payload handle for generated application nodes. */
        public class GeneratedNode<P> extends TypedEntity<P> implements GeneratedNodeHandle {
            protected GeneratedNode(EntityTypeMeta meta, Class<P> payloadType, UUID id, P payload) {
                super(meta, payloadType, id, payload, new LinkedHashMap<>());
            }

            @Override
            public UUID id() {
                return getId();
            }
        }
    """.trimIndent() + "\n"

    private fun nodeHandleSource(packageName: String): String = """
        package $packageName;

        import java.util.UUID;

        /** Common generated node identity contract used by mutation relation methods. */
        public interface GeneratedNodeHandle {
            UUID id();
        }
    """.trimIndent() + "\n"

    private fun capabilitySource(packageName: String, name: String): String = """
        package $packageName;

        /** Independent generated capability marker; behavior is added by later codegen stages. */
        public interface $name {
        }
    """.trimIndent() + "\n"

    private fun refSource(packageName: String, entity: DefinitionSpec): String {
        val refName = "${entity.definitionKey}Ref"
        return """
            package $packageName;

            import java.util.Objects;
            import java.util.UUID;

            /** Typed identity-only reference for ${entity.type}@${entity.schemaVersion}. */
            public record $refName(UUID id) {
                public $refName {
                    Objects.requireNonNull(id, "id");
                }
            }
        """.trimIndent() + "\n"
    }

    private fun nodeSource(packageName: String, entity: DefinitionSpec): String {
        val nodeName = "${entity.definitionKey}Node"
        val refName = "${entity.definitionKey}Ref"
        val base = entity.baseClass ?: "GeneratedNode<${entity.javaTypeName}>"
        val implements = (entity.interfaces + listOf("ReadNodeCapability", "MutationNodeCapability"))
            .distinct()
            .joinToString(", ")
        val constructorSuper = if (entity.baseClass == null) {
            "super(new EntityTypeMeta(\"${java(entity.type)}\", \"${java(entity.schemaVersion)}\", null), " +
                "${entity.javaTypeName}.class, id, payload);"
        } else {
            "super();"
        }
        val accessors = if (entity.baseClass == null) {
            """
                public ${entity.javaTypeName} payload() {
                    return getPayload();
                }

                public UUID id() {
                    return getId();
                }
            """.trimIndent()
        } else {
            """
                private final UUID nodeId;
                private final ${entity.javaTypeName} nodePayload;

                public ${entity.javaTypeName} payload() {
                    return nodePayload;
                }

                public UUID id() {
                    return nodeId;
                }
            """.trimIndent()
        }
        val ref = if (entity.baseClass == null) {
            "return new $refName(java.util.Objects.requireNonNull(getId(), \"id\"));"
        } else {
            "return new $refName(java.util.Objects.requireNonNull(nodeId, \"id\"));"
        }
        val toEntity = if (entity.baseClass == null) {
            ""
        } else {
            """
                public Entity toEntity(PayloadMapper mapper) {
                    return new Entity(id(), "${java(entity.type)}", "${java(entity.schemaVersion)}",
                        mapper.toMap(payload()));
                }
            """.trimIndent()
        }
        return """
            package $packageName;

            import java.util.UUID;
            import org.poc.objs.api.domain.Entity;
            import org.poc.objs.api.typed.EntityTypeMeta;
            import org.poc.objs.api.typed.PayloadMapper;

            /** Typed node binding for ${entity.type}@${entity.schemaVersion}. */
            public final class $nodeName extends $base implements $implements, GeneratedNodeHandle {
                public $nodeName(UUID id, ${entity.javaTypeName} payload) {
                    $constructorSuper
                    ${if (entity.baseClass != null) "this.nodeId = id; this.nodePayload = payload;" else ""}
                }

                public $nodeName(${entity.javaTypeName} payload) {
                    this(null, payload);
                }

                $accessors

                public $refName ref() {
                    $ref
                }

                $toEntity
            }
        """.trimIndent() + "\n"
    }

    private fun typeSource(packageName: String, entity: DefinitionSpec): String {
        val typeName = "${entity.definitionKey}Type"
        val nodeName = "${entity.definitionKey}Node"
        val refName = "${entity.definitionKey}Ref"
        return """
            package $packageName;

            import java.util.UUID;
            import org.poc.objs.api.typed.EntityTypeMeta;

            /** Stable generated metadata and factories for ${entity.type}. */
            public final class $typeName {
                public static final String TYPE = "${java(entity.type)}";
                public static final String SCHEMA_VERSION = "${java(entity.schemaVersion)}";
                public static final EntityTypeMeta META = new EntityTypeMeta(TYPE, SCHEMA_VERSION, null);

                private $typeName() {
                }

                public static $nodeName node(${entity.javaTypeName} payload) {
                    return new $nodeName(payload);
                }

                public static $nodeName node(UUID id, ${entity.javaTypeName} payload) {
                    return new $nodeName(id, payload);
                }

                public static $refName ref(UUID id) {
                    return new $refName(id);
                }
            }
        """.trimIndent() + "\n"
    }

    private fun relationMetadataSource(packageName: String, relations: List<RelationSpec>): String {
        val entries = relations.sortedWith(compareBy({ it.sourceType }, { it.role }, { it.targetType }))
            .joinToString(",\n") {
                "        new RelationSpec(\"${java(it.sourceType)}\", \"${java(it.role)}\", " +
                    "\"${java(it.targetType)}\", \"${java(it.sourceDefinition)}\", " +
                    "\"${java(it.targetDefinition)}\", ${it.sourceStaticBinding}, " +
                    "${it.targetStaticBinding}, \"${java(it.edgePropertyType)}\")"
            }
        return """
            package $packageName;

            import java.util.List;

            /** Deterministic relation metadata consumed by later read/write generator stages. */
            public final class GeneratedRelationMetadata {
                public record RelationSpec(
                    String sourceType,
                    String role,
                    String targetType,
                    String sourceDefinition,
                    String targetDefinition,
                    boolean sourceStaticBinding,
                    boolean targetStaticBinding,
                    String edgePropertyType
                ) {
                }

                public static final List<RelationSpec> ALL = List.of(
            $entries
                );

                private GeneratedRelationMetadata() {
                }
            }
        """.trimIndent() + "\n"
    }

    private fun readViewSource(
        packageName: String,
        entities: List<DefinitionSpec>,
    ): String {
        val bindings = entities.sortedBy { it.definitionKey }.joinToString("\n") { entity ->
            """
                if ("${java(entity.type)}".equals(type) &&
                    "${java(entity.schemaVersion)}".equals(schemaVersion)) {
                    return (raw, mapper) -> mapper.fromMap(
                        raw.getPayload(), ${entity.javaTypeName}.class
                    );
                }
            """.trimIndent()
        }
        val roots = entities.sortedBy { it.definitionKey }.joinToString("\n\n") { entity ->
            val method = lowerCamel(entity.definitionKey)
            val node = "${entity.definitionKey}ReadNode"
            """
                public TypedCollection<$node> ${method}s() {
                    List<$node> values = new ArrayList<>();
                    for (ReadNode node : view.nodes("${java(entity.type)}")) {
                        values.add($method(node));
                    }
                    return TypedCollection.of(values);
                }

                public $node $method(ReadNode node) {
                    return node == null ? null : new $node(this, node);
                }
            """.trimIndent()
        }
        return """
            package $packageName;

            import java.util.ArrayList;
            import java.util.List;
            import java.util.UUID;
            import org.poc.objs.api.domain.Entity;
            import org.poc.objs.api.domain.Graph;
            import org.poc.objs.api.typed.PayloadMapper;
            import org.poc.objs.api.typed.ReadNode;
            import org.poc.objs.api.typed.RelationEdgeView;
            import org.poc.objs.api.typed.TypedCollection;
            import org.poc.objs.api.typed.TypedEntityBinding;
            import org.poc.objs.api.typed.TypedEntityBindingRegistry;
            import org.poc.objs.api.typed.TypedGraphView;

            /** Application-owned typed facade over one immutable in-memory graph snapshot. */
            public final class GeneratedReadView {
                private static final TypedEntityBindingRegistry BINDINGS = (type, schemaVersion) -> {
            $bindings
                    return null;
                };

                private final TypedGraphView view;

                private GeneratedReadView(TypedGraphView view) {
                    this.view = view;
                }

                public static GeneratedReadView from(Graph graph) {
                    return new GeneratedReadView(TypedGraphView.from(graph));
                }

                public static GeneratedReadView from(Graph graph, PayloadMapper mapper) {
                    return new GeneratedReadView(TypedGraphView.from(graph, BINDINGS, mapper));
                }

                public static GeneratedReadView from(
                    Graph graph,
                    TypedEntityBindingRegistry bindings,
                    PayloadMapper mapper
                ) {
                    return new GeneratedReadView(TypedGraphView.from(graph, bindings, mapper));
                }

                public TypedGraphView raw() {
                    return view;
                }

                public TypedCollection<ReadNode> allNodes() {
                    return view.allNodes();
                }

                public TypedCollection<RelationEdgeView> allEdges() {
                    return view.allEdges();
                }

                public ReadNode node(UUID id) {
                    return view.node(id);
                }

            $roots
            }
        """.trimIndent() + "\n"
    }

    private fun readNodeSource(
        packageName: String,
        entity: DefinitionSpec,
        entities: List<DefinitionSpec>,
        relations: List<RelationSpec>,
    ): String {
        val entitiesByKey = entities.associateBy { it.definitionKey }
        fun relationIsUsable(relation: RelationSpec, source: String?, target: String?): Boolean =
            !relation.skip &&
                relation.sourceDefinition == source &&
                relation.targetDefinition == target &&
                source in entitiesByKey &&
                target in entitiesByKey

        val outgoing = relations
            .filter { relationIsUsable(it, entity.definitionKey, it.targetDefinition) }
            .sortedWith(compareBy({ it.role }, { it.targetType }))
        val incoming = relations
            .filter {
                !it.noInverse &&
                    relationIsUsable(it, it.sourceDefinition, entity.definitionKey)
            }
            .sortedWith(compareBy({ it.role }, { it.sourceType }))

        fun relationMethods(
            relation: RelationSpec,
            target: DefinitionSpec,
            method: String,
            direction: String,
        ): String {
            val targetNode = "${target.definitionKey}ReadNode"
            val prefix = "get${capitalizeJava(method)}"
            val collectionMethod = if (method.endsWith("s")) prefix else "${prefix}s"
            val edgeMethod = "${prefix}Edges"
            val directionEnum = "RelationDirection.$direction"
            val targetFactory = lowerCamel(target.definitionKey)
            val singular = if (relation.cardinality == "1:1") {
                """
                    public $targetNode $prefix() {
                        ReadNode node = delegate.singular(
                            "${java(relation.role)}", $directionEnum
                        );
                        return node == null ? null : view.$targetFactory(node);
                    }
                """.trimIndent()
            } else {
                ""
            }
            return """
                public TypedCollection<$targetNode> $collectionMethod() {
                    List<$targetNode> values = new ArrayList<>();
                    for (RelationEdgeView edge : delegate.edges(
                        "${java(relation.role)}", $directionEnum
                    )) {
                        $targetNode node = view.$targetFactory(
                            ${if (direction == "OUTBOUND") "edge.getTarget()" else "edge.getSource()"}
                        );
                        if (node != null) values.add(node);
                    }
                    return TypedCollection.of(values);
                }

                public TypedCollection<RelationEdgeView> $edgeMethod() {
                    return delegate.edges("${java(relation.role)}", $directionEnum);
                }

                $singular
            """.trimIndent()
        }

        val methods = buildList {
            outgoing.forEach { relation ->
                val target = entitiesByKey.getValue(relation.targetDefinition!!)
                add(relationMethods(relation, target, relation.outboundMethod, "OUTBOUND"))
            }
            incoming.forEach { relation ->
                val target = entitiesByKey.getValue(relation.sourceDefinition!!)
                add(relationMethods(relation, target, relation.inboundMethod, "INBOUND"))
            }
        }.joinToString("\n\n")
        val nodeName = "${entity.definitionKey}ReadNode"
        return """
            package $packageName;

            import java.util.ArrayList;
            import java.util.List;
            import java.util.UUID;
            import org.poc.objs.api.typed.EntityRef;
            import org.poc.objs.api.typed.ReadNode;
            import org.poc.objs.api.typed.RelationDirection;
            import org.poc.objs.api.typed.RelationEdgeView;
            import org.poc.objs.api.typed.TypedCollection;

            /** Typed, read-only facade for ${entity.type}@${entity.schemaVersion}. */
            public final class $nodeName {
                private final GeneratedReadView view;
                private final ReadNode delegate;

                $nodeName(GeneratedReadView view, ReadNode delegate) {
                    this.view = view;
                    this.delegate = delegate;
                }

                public UUID id() {
                    return delegate.getId();
                }

                public ${entity.javaTypeName} payload() {
                    return (${entity.javaTypeName}) delegate.ref().getPayload();
                }

                public EntityRef<?> ref() {
                    return delegate.ref();
                }

                public TypedCollection<RelationEdgeView> edges(
                    String role,
                    RelationDirection direction
                ) {
                    return delegate.edges(role, direction);
                }

                public TypedCollection<RelationEdgeView> edges() {
                    return delegate.edges();
                }

            $methods
            }
        """.trimIndent() + "\n"
    }

    private fun lowerCamel(value: String): String =
        value.replaceFirstChar { it.lowercaseChar() }

    private fun capitalizeJava(value: String): String =
        value.replaceFirstChar { it.uppercaseChar() }

    private fun mutationBuilderSource(
        packageName: String,
        entities: List<DefinitionSpec>,
        relations: List<RelationSpec>,
    ): String {
        val entitiesByKey = entities.associateBy { it.definitionKey }
        val addMethods = entities.sortedBy { it.definitionKey }.joinToString("\n\n") { entity ->
            val method = "add${entity.definitionKey}"
            val node = "${entity.definitionKey}Node"
            """
                public $node $method(${entity.javaTypeName} payload) {
                    return $method(UUID.randomUUID(), payload);
                }

                public $node $method(UUID id, ${entity.javaTypeName} payload) {
                    $node node = ${entity.definitionKey}Type.node(requireId(id), payload);
                    registerEntity(node.toEntity(mapper));
                    return node;
                }

                public $node $method($node node) {
                    Objects.requireNonNull(node, "node");
                    registerEntity(node.toEntity(mapper));
                    return node;
                }
            """.trimIndent()
        }
        val relationMethods = relations
            .filter {
                it.sourceType != "*" &&
                    it.targetType != "*" &&
                    it.sourceDefinition != null &&
                    it.targetDefinition != null &&
                    it.sourceDefinition in entitiesByKey &&
                    it.targetDefinition in entitiesByKey
            }
            .sortedWith(compareBy({ it.sourceType }, { it.role }, { it.targetType }))
            .joinToString("\n\n") { relation ->
                val source = entitiesByKey.getValue(relation.sourceDefinition!!)
                val target = entitiesByKey.getValue(relation.targetDefinition!!)
                val sourceNode = "${source.definitionKey}Node"
                val targetNode = "${target.definitionKey}Node"
                val propertyType = relation.edgePropertyJavaType ?: "Map<String, Object>"
                val propertyArgument = if (relation.propertiesPolicy == "SCHEMA") {
                    ", $propertyType properties"
                } else {
                    ""
                }
                val typeArguments = if (relation.propertiesPolicy == "SCHEMA") {
                    "\"${java(relation.edgePropertyType)}\", \"${java(relation.edgePropertySchemaVersion)}\""
                } else {
                    "null, null"
                }
                val withProperties = if (relation.propertiesPolicy == "SCHEMA") {
                    """
                        public Edge ${relation.outboundMethod}(
                            ${sourceNode} source,
                            ${targetNode} target$propertyArgument
                        ) {
                            return addRelation(
                                UUID.randomUUID(), source, target, "${java(relation.role)}",
                                $typeArguments, edgePropertiesFor(
                                    properties, ${relation.emptyPropertiesAllowed}
                                )
                            );
                        }
                    """.trimIndent()
                } else {
                    ""
                }
                val noProperties = if (
                    relation.propertiesPolicy == "NONE" ||
                    relation.emptyPropertiesAllowed
                ) {
                    """
                        public Edge ${relation.outboundMethod}(
                            ${sourceNode} source,
                            ${targetNode} target
                        ) {
                            return addRelation(
                                UUID.randomUUID(), source, target, "${java(relation.role)}",
                                $typeArguments,
                                ${if (relation.propertiesPolicy == "SCHEMA") {
                        "new LinkedHashMap<>()"
                    } else {
                        "null"
                    }}
                            );
                        }

                        public Edge ${relation.outboundMethod}(
                            UUID edgeId,
                            ${sourceNode} source,
                            ${targetNode} target
                        ) {
                            return addRelation(
                                edgeId, source, target, "${java(relation.role)}",
                                $typeArguments,
                                ${if (relation.propertiesPolicy == "SCHEMA") {
                        "new LinkedHashMap<>()"
                    } else {
                        "null"
                    }}
                            );
                        }
                    """.trimIndent()
                } else {
                    ""
                }
                val schemaOverload = if (relation.propertiesPolicy == "SCHEMA") {
                    """
                        public Edge ${relation.outboundMethod}(
                            UUID edgeId,
                            ${sourceNode} source,
                            ${targetNode} target,
                            ${propertyType} properties
                        ) {
                            return addRelation(
                                edgeId, source, target, "${java(relation.role)}",
                                $typeArguments,
                                edgePropertiesFor(properties, ${relation.emptyPropertiesAllowed})
                            );
                        }
                    """.trimIndent()
                } else {
                    ""
                }
                """
                    $withProperties

                    $noProperties

                    $schemaOverload
                """.trimIndent()
            }
        return """
            package $packageName;

            import java.util.ArrayList;
            import java.util.Collections;
            import java.util.LinkedHashMap;
            import java.util.List;
            import java.util.Map;
            import java.util.Objects;
            import java.util.UUID;
            import org.poc.objs.api.domain.Edge;
            import org.poc.objs.api.domain.EdgeMutation;
            import org.poc.objs.api.domain.Entity;
            import org.poc.objs.api.domain.EntityMutation;
            import org.poc.objs.api.domain.GraphMutation;
            import org.poc.objs.api.domain.MutationMode;
            import org.poc.objs.api.typed.PayloadMapper;

            /** Generated, in-memory mutation assembly; it never persists or validates remotely. */
            public final class GraphMutationBuilder {
                private final PayloadMapper mapper;
                private final Map<UUID, Entity> entities = new LinkedHashMap<>();
                private final Map<UUID, Edge> edges = new LinkedHashMap<>();
                private final List<UUID> unsetEntities = new ArrayList<>();
                private final List<UUID> unsetEdges = new ArrayList<>();
                private final List<String> diagnostics = new ArrayList<>();
                private MutationMode mode = MutationMode.MERGE;

                public GraphMutationBuilder(PayloadMapper mapper) {
                    this.mapper = Objects.requireNonNull(mapper, "mapper");
                }

                public GraphMutationBuilder(PayloadMapper mapper, MutationMode mode) {
                    this(mapper);
                    this.mode = Objects.requireNonNull(mode, "mode");
                }

                public GraphMutationBuilder mode(MutationMode mode) {
                    this.mode = Objects.requireNonNull(mode, "mode");
                    return this;
                }

                $addMethods

                $relationMethods

                public GraphMutationBuilder unsetEntity(UUID id) {
                    unsetEntities.add(requireId(id));
                    return this;
                }

                public GraphMutationBuilder unsetEdge(UUID id) {
                    unsetEdges.add(requireId(id));
                    return this;
                }

                private void registerEdge(Edge edge) {
                    Objects.requireNonNull(edge, "edge");
                    if (edge.getId() == null) {
                        edge.setId(UUID.randomUUID());
                    }
                    UUID id = edge.getId();
                    if (edges.putIfAbsent(id, edge) != null) {
                        throw new IllegalArgumentException("Duplicate edge UUID: " + id);
                    }
                }

                public List<String> diagnostics() {
                    return Collections.unmodifiableList(new ArrayList<>(diagnostics));
                }

                public GraphMutation build() {
                    return new GraphMutation(
                        new EntityMutation(
                            new ArrayList<>(entities.values()),
                            new ArrayList<>(unsetEntities)
                        ),
                        new EdgeMutation(
                            new ArrayList<>(edges.values()),
                            new ArrayList<>(unsetEdges)
                        ),
                        mode
                    );
                }

                private void registerEntity(Entity entity) {
                    Objects.requireNonNull(entity, "entity");
                    UUID id = requireId(entity.getId());
                    if (entities.putIfAbsent(id, entity) != null) {
                        throw new IllegalArgumentException("Duplicate entity UUID: " + id);
                    }
                }

                private Edge addRelation(
                    UUID edgeId,
                    GeneratedNodeHandle source,
                    GeneratedNodeHandle target,
                    String role,
                    String propertyType,
                    String propertyVersion,
                    Map<String, Object> properties
                ) {
                    Objects.requireNonNull(source, "source");
                    Objects.requireNonNull(target, "target");
                    UUID sourceId = requireId(source.id());
                    UUID targetId = requireId(target.id());
                    if (!entities.containsKey(sourceId) || !entities.containsKey(targetId)) {
                        diagnostics.add(
                            "Relation endpoint was not registered: " + sourceId + " -> " + targetId
                        );
                    }
                    Edge edge = new Edge(
                        requireId(edgeId), null, sourceId, targetId, role,
                        propertyType, propertyVersion, properties
                    );
                    registerEdge(edge);
                    return edge;
                }

                private Map<String, Object> edgePropertiesFor(
                    Object properties,
                    boolean emptyPropertiesAllowed
                ) {
                    if (properties == null) {
                        return emptyPropertiesAllowed ? new LinkedHashMap<>() : null;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mapped = mapper.toMap(properties);
                    return mapped;
                }

                private UUID requireId(UUID id) {
                    return Objects.requireNonNull(id, "id");
                }
            }
        """.trimIndent() + "\n"
    }

    private fun parseDiagnostics(raw: Any?): List<String> =
        list(raw).orEmpty().mapNotNull { map(it)?.get("message")?.toString() }

    private fun validateOutputDirectory(outputDirectory: Path) {
        val normalized = outputDirectory.toAbsolutePath().normalize()
        if (normalized.any { it.fileName.toString().startsWith("objs-") }) {
            throw JavaCodegenException(
                "Generated output must be application-owned and cannot be under a root objs-* module: $normalized",
            )
        }
    }

    private fun validatePackageName(packageName: String) {
        if (packageName.isBlank()) throw JavaCodegenException("targetPackage must not be blank")
        packageName.split('.').forEachIndexed { index, part ->
            validJavaIdentifier(part, "targetPackage[$index]")
        }
    }

    private fun parseInterfaces(raw: Any?, owner: String): List<String> {
        val values = when (raw) {
            null -> emptyList()
            is List<*> -> raw.map { it?.toString().orEmpty() }
            is String -> raw.split(",")
            else -> throw JavaCodegenException("$owner must be an array or comma-separated string")
        }
        return values.mapIndexed { index, value ->
            optionalJavaType(value, "$owner[$index]")
                ?: throw JavaCodegenException("$owner[$index] must not be blank")
        }.distinct()
    }

    private fun optionalJavaType(raw: Any?, owner: String): String? =
        when (raw) {
            null -> null
            else -> {
                val value = raw.toString().trim()
                if (value.isEmpty()) {
                    throw JavaCodegenException("$owner must not be blank")
                }
                validJavaType(value, owner)
            }
        }

    private fun validJavaType(raw: String, owner: String): String {
        val parts = raw.trim().split('.')
        if (parts.any { !JAVA_IDENTIFIER.matches(it) || it in JAVA_KEYWORDS }) {
            throw JavaCodegenException("$owner is not a valid Java type: '$raw'")
        }
        return parts.joinToString(".")
    }

    private fun validJavaIdentifier(raw: String, owner: String): String {
        val value = raw.trim()
        if (!JAVA_IDENTIFIER.matches(value) || value in JAVA_KEYWORDS) {
            throw JavaCodegenException("$owner is not a valid Java identifier: '$raw'")
        }
        return value
    }

    private fun requiredString(map: Map<String, Any?>, key: String, owner: String): String =
        optionalString(map[key])?.takeIf { it.isNotBlank() }
            ?: throw JavaCodegenException("$owner.$key must be a non-blank string")

    private fun optionalString(value: Any?): String? = value?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    private fun boolean(map: Map<String, Any?>, key: String, default: Boolean): Boolean =
        when (val value = map[key]) {
            null -> default
            is Boolean -> value
            else -> throw JavaCodegenException("$key must be boolean")
        }

    private fun map(value: Any?): Map<String, Any?>? =
        (value as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value }

    private fun list(value: Any?): List<Any?>? = value as? List<Any?>

    private fun writeIfChanged(path: Path, source: String) {
        val bytes = source.toByteArray(StandardCharsets.UTF_8)
        if (!Files.exists(path) || !Files.readAllBytes(path).contentEquals(bytes)) {
            Files.write(path, bytes)
        }
    }

    private fun java(value: String?): String =
        value?.replace("\\", "\\\\")?.replace("\"", "\\\"") ?: ""

    private data class DefinitionSpec(
        val definitionKey: String,
        val kind: String,
        val type: String,
        val schemaVersion: String,
        val generated: Boolean,
        val skip: Boolean,
        val javaTypeName: String,
        val baseClass: String?,
        val interfaces: List<String>,
    )

    private data class RelationSpec(
        val sourceType: String,
        val role: String,
        val targetType: String,
        val sourceDefinition: String?,
        val targetDefinition: String?,
        val outboundMethod: String,
        val inboundMethod: String,
        val sourceStaticBinding: Boolean,
        val targetStaticBinding: Boolean,
        val cardinality: String,
        val noInverse: Boolean,
        val skip: Boolean,
        val propertiesPolicy: String,
        val emptyPropertiesAllowed: Boolean,
        val edgePropertyType: String?,
        val edgePropertySchemaVersion: String?,
        val edgePropertyJavaType: String?,
    )

    companion object {
        private val JAVA_IDENTIFIER = Regex("[A-Za-z_$][A-Za-z0-9_$]*")
        private val JAVA_KEYWORDS = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false",
            "null", "var", "yield", "record", "sealed", "permits",
        )
    }
}

object JavaCodegenMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 3) {
            "Usage: JavaCodegenMain <schema-file> <output-directory> <target-package>"
        }
        val report = JavaCodeGenerator().generate(
            JavaCodegenOptions(
                schemaFile = Path.of(args[0]),
                outputDirectory = Path.of(args[1]),
                targetPackage = args[2],
            ),
        )
        println("Generated ${report.generatedFiles.size} Java files")
        report.diagnostics.forEach { println("diagnostic: $it") }
    }
}
