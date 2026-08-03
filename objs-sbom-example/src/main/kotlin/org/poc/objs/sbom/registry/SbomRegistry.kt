package org.poc.objs.sbom.registry

import org.poc.objs.core.domain.BoMAllowedEdgeRule
import org.poc.objs.core.domain.BoMEdgeCardinality
import org.poc.objs.core.domain.BoMPropertiesPolicy
import org.poc.objs.core.domain.BoMSchema
import org.poc.objs.core.domain.BoMSchemaDsl
import org.poc.objs.core.domain.BoMSchemaNode
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.typed.RegistryPack
import org.poc.objs.core.typed.TypedEdgeMeta
import org.poc.objs.sbom.model.ApiType
import org.poc.objs.sbom.model.ArtifactType
import org.poc.objs.sbom.model.BuildType
import org.poc.objs.sbom.model.CanonicalEdgeType
import org.poc.objs.sbom.model.ComponentType
import org.poc.objs.sbom.model.ContainerImageType
import org.poc.objs.sbom.model.ContainerLayerType
import org.poc.objs.sbom.model.DatabaseType
import org.poc.objs.sbom.model.DatasetType
import org.poc.objs.sbom.model.DeploymentType
import org.poc.objs.sbom.model.EnvironmentType
import org.poc.objs.sbom.model.HostType
import org.poc.objs.sbom.model.KubernetesClusterType
import org.poc.objs.sbom.model.LicenseType
import org.poc.objs.sbom.model.NamespaceType
import org.poc.objs.sbom.model.OperatingSystemType
import org.poc.objs.sbom.model.OrganizationType
import org.poc.objs.sbom.model.PolicyType
import org.poc.objs.sbom.model.ProductType
import org.poc.objs.sbom.model.RuntimeType
import org.poc.objs.sbom.model.SCHEMA_VERSION
import org.poc.objs.sbom.model.ServiceType
import org.poc.objs.sbom.model.SourceModuleType
import org.poc.objs.sbom.model.SourceRepositoryType
import org.poc.objs.sbom.model.VulnerabilityType

object SbomRoles {
    const val DEPENDS_ON = "DEPENDS_ON"
    const val PROVIDED_BY = "PROVIDED_BY"
    const val LICENSED_UNDER = "LICENSED_UNDER"
    const val HAS_VULNERABILITY = "HAS_VULNERABILITY"
    const val CONTAINS = "CONTAINS"
    const val OWNED_BY = "OWNED_BY"
    const val USES = "USES"
    const val PRODUCES = "PRODUCES"
    const val BUILDS = "BUILDS"
    const val PACKAGES = "PACKAGES"
    const val BASED_ON = "BASED_ON"
    const val RUNS_ON = "RUNS_ON"
    const val DEPLOYS = "DEPLOYS"
    const val TARGETS = "TARGETS"
    const val MEMBER_OF = "MEMBER_OF"
    const val LOCATED_IN = "LOCATED_IN"
    const val IMPLEMENTS = "IMPLEMENTS"
    const val CALLS = "CALLS"
    const val CONNECTS_TO = "CONNECTS_TO"
    const val COMPLIES_WITH = "COMPLIES_WITH"
}

object SbomRegistry {
    private val stringProp = BoMSchemaDsl.string("Text", "Text value")
    private val uriProp = BoMSchemaDsl.string("URI", "URI value", format = "uri")
    private val dateTimeProp = BoMSchemaDsl.string("Date and time", "ISO-8601 timestamp", format = "date-time")
    private val numberProp = BoMSchemaDsl.number("Number", "Numeric value")
    private val integerProp = BoMSchemaDsl.integer("Integer", "Whole-number value")
    private val arrayOfString = BoMSchemaDsl.array(
        "Text list",
        "Ordered list of text values",
        BoMSchemaDsl.string("Text item", "One text value"),
    )
    private val objectProp = BoMSchemaDsl.obj("Attributes", "Open attributes object")

    private val commonOptional = mapOf(
        "description" to stringProp,
        "labels" to arrayOfString,
        "attributes" to objectProp,
    )

    private fun edgeRule(
        source: String,
        role: String,
        target: String,
        cardinality: BoMEdgeCardinality = BoMEdgeCardinality.ONE_TO_MANY,
    ) = BoMAllowedEdgeRule(
        sourceType = source,
        role = role,
        targetType = target,
        propertiesPolicy = BoMPropertiesPolicy.SCHEMA,
        emptyPropertiesAllowed = true,
        propertiesSchemaType = CanonicalEdgeType.meta.type,
        propertiesSchemaVersion = SCHEMA_VERSION,
        cardinality = cardinality,
    )

    private fun schema(
        type: String,
        required: List<String>,
        properties: Map<String, BoMSchemaNode>,
    ): BoMSchema = RegistryPack.objectSchema(
        type = type,
        version = SCHEMA_VERSION,
        title = type,
        description = "Canonical SBOM $type payload",
        fields = (properties + commonOptional).map { (name, fieldSchema) ->
            BoMSchemaDsl.field(name, fieldSchema, required = name in required)
        },
    )

    fun canonicalEdgeMeta(
        role: String,
        sourceType: String,
        targetType: String,
        cardinality: BoMEdgeCardinality = BoMEdgeCardinality.ONE_TO_MANY,
    ) = TypedEdgeMeta(
        role = role,
        sourceType = sourceType,
        targetType = targetType,
        propertiesPolicy = BoMPropertiesPolicy.SCHEMA,
        propertiesMeta = CanonicalEdgeType.meta,
        emptyPropertiesAllowed = true,
        cardinality = cardinality,
    )

    fun dependsOnMeta() = canonicalEdgeMeta(
        SbomRoles.DEPENDS_ON,
        "Component",
        "Component",
        BoMEdgeCardinality.ONE_TO_MANY,
    )

    /** Full canonical ontology: all entity schemas + relationship allow-list. */
    fun pack(): RegistryPack {
        val canonicalEdge = RegistryPack.objectSchema(
            type = CanonicalEdgeType.meta.type,
            version = SCHEMA_VERSION,
            title = "Canonical edge",
            description = "Properties shared by canonical SBOM relationships",
            usages = setOf(BoMSchemaUsage.EDGE_PROPERTIES),
            fields = listOf(
                BoMSchemaDsl.field(
                    "createdAt",
                    BoMSchemaDsl.string("Created at", "Relationship creation timestamp", format = "date-time"),
                    required = false,
                ),
                BoMSchemaDsl.field("source", stringProp, required = false),
                BoMSchemaDsl.field("confidence", numberProp, required = false),
                BoMSchemaDsl.field("attributes", objectProp, required = false),
            ),
        )

        val entitySchemas = listOf(
            schema(
                ComponentType.meta.type,
                listOf("name", "version", "ecosystem", "kind"),
                mapOf(
                    "name" to stringProp,
                    "version" to stringProp,
                    "ecosystem" to stringProp,
                    "kind" to stringProp,
                    "coordinates" to stringProp,
                ),
            ),
            schema(
                ProductType.meta.type,
                listOf("name", "version"),
                mapOf(
                    "name" to stringProp,
                    "version" to stringProp,
                    "supplier" to stringProp,
                    "lifecycle" to stringProp,
                    "homepage" to uriProp,
                ),
            ),
            schema(
                OrganizationType.meta.type,
                listOf("name"),
                mapOf(
                    "name" to stringProp,
                    "domain" to stringProp,
                    "website" to uriProp,
                    "country" to stringProp,
                ),
            ),
            schema(
                LicenseType.meta.type,
                listOf("name", "spdxId"),
                mapOf("name" to stringProp, "spdxId" to stringProp, "url" to uriProp),
            ),
            schema(
                VulnerabilityType.meta.type,
                listOf("name", "cve", "severity"),
                mapOf(
                    "name" to stringProp,
                    "cve" to stringProp,
                    "severity" to stringProp,
                    "cvss" to numberProp,
                ),
            ),
            schema(
                BuildType.meta.type,
                listOf("name", "buildNumber", "status"),
                mapOf(
                    "name" to stringProp,
                    "buildNumber" to stringProp,
                    "builder" to stringProp,
                    "status" to stringProp,
                ),
            ),
            schema(
                SourceRepositoryType.meta.type,
                listOf("name", "url"),
                mapOf(
                    "name" to stringProp,
                    "url" to uriProp,
                    "revision" to stringProp,
                    "branch" to stringProp,
                ),
            ),
            schema(
                SourceModuleType.meta.type,
                listOf("name", "path"),
                mapOf("name" to stringProp, "path" to stringProp, "language" to stringProp),
            ),
            schema(
                ArtifactType.meta.type,
                listOf("name", "artifactType"),
                mapOf(
                    "name" to stringProp,
                    "artifactType" to stringProp,
                    "checksum" to stringProp,
                    "size" to integerProp,
                ),
            ),
            schema(
                ContainerImageType.meta.type,
                listOf("name", "tag"),
                mapOf(
                    "name" to stringProp,
                    "tag" to stringProp,
                    "digest" to stringProp,
                    "registry" to stringProp,
                ),
            ),
            schema(
                ContainerLayerType.meta.type,
                listOf("name", "digest"),
                mapOf("name" to stringProp, "digest" to stringProp, "size" to integerProp),
            ),
            schema(
                RuntimeType.meta.type,
                listOf("name", "runtimeType"),
                mapOf("name" to stringProp, "runtimeType" to stringProp, "version" to stringProp),
            ),
            schema(
                OperatingSystemType.meta.type,
                listOf("name", "distribution"),
                mapOf(
                    "name" to stringProp,
                    "distribution" to stringProp,
                    "version" to stringProp,
                    "architecture" to stringProp,
                ),
            ),
            schema(
                DeploymentType.meta.type,
                listOf("name"),
                mapOf(
                    "name" to stringProp,
                    "status" to stringProp,
                    "replicas" to integerProp,
                    "deployedAt" to dateTimeProp,
                ),
            ),
            schema(
                EnvironmentType.meta.type,
                listOf("name", "environment"),
                mapOf("name" to stringProp, "environment" to stringProp),
            ),
            schema(
                HostType.meta.type,
                listOf("name", "hostname"),
                mapOf(
                    "name" to stringProp,
                    "hostname" to stringProp,
                    "ip" to stringProp,
                    "provider" to stringProp,
                ),
            ),
            schema(
                KubernetesClusterType.meta.type,
                listOf("name"),
                mapOf("name" to stringProp, "version" to stringProp),
            ),
            schema(
                NamespaceType.meta.type,
                listOf("name", "namespace"),
                mapOf("name" to stringProp, "namespace" to stringProp),
            ),
            schema(
                ServiceType.meta.type,
                listOf("name"),
                mapOf("name" to stringProp, "protocol" to stringProp, "endpoint" to uriProp),
            ),
            schema(
                ApiType.meta.type,
                listOf("name", "protocol"),
                mapOf("name" to stringProp, "protocol" to stringProp, "version" to stringProp),
            ),
            schema(
                DatabaseType.meta.type,
                listOf("name", "engine"),
                mapOf("name" to stringProp, "engine" to stringProp, "version" to stringProp),
            ),
            schema(
                DatasetType.meta.type,
                listOf("name", "datasetType"),
                mapOf(
                    "name" to stringProp,
                    "datasetType" to stringProp,
                    "classification" to stringProp,
                ),
            ),
            schema(
                PolicyType.meta.type,
                listOf("name"),
                mapOf("name" to stringProp, "policyType" to stringProp, "version" to stringProp),
            ),
        )

        // Exact triples from canonical-spec.md relationship table (+ cardinality)
        val one = BoMEdgeCardinality.ONE_TO_ONE
        val many = BoMEdgeCardinality.ONE_TO_MANY
        val edgeRules = listOf(
            edgeRule("Product", SbomRoles.CONTAINS, "Component", many),
            edgeRule("Product", SbomRoles.CONTAINS, "Artifact", many),
            edgeRule("Container Image", SbomRoles.CONTAINS, "Container Layer", many),
            edgeRule("Database", SbomRoles.CONTAINS, "Dataset", many),
            edgeRule("Source Repository", SbomRoles.CONTAINS, "Source Module", many),
            edgeRule("Component", SbomRoles.DEPENDS_ON, "Component", many),
            edgeRule("Source Module", SbomRoles.PRODUCES, "Artifact", many),
            edgeRule("Build", SbomRoles.BUILDS, "Artifact", many),
            edgeRule("Build", SbomRoles.BUILDS, "Container Image", many),
            edgeRule("Build", SbomRoles.USES, "Component", many),
            edgeRule("Container Image", SbomRoles.PACKAGES, "Artifact", many),
            edgeRule("Container Image", SbomRoles.BASED_ON, "Operating System", one),
            edgeRule("Product", SbomRoles.RUNS_ON, "Runtime", one),
            edgeRule("Runtime", SbomRoles.RUNS_ON, "Operating System", one),
            edgeRule("Deployment", SbomRoles.DEPLOYS, "Container Image", one),
            edgeRule("Deployment", SbomRoles.TARGETS, "Environment", one),
            edgeRule("Deployment", SbomRoles.RUNS_ON, "Host", many),
            edgeRule("Host", SbomRoles.MEMBER_OF, "Kubernetes Cluster", one),
            edgeRule("Deployment", SbomRoles.LOCATED_IN, "Namespace", one),
            edgeRule("Service", SbomRoles.IMPLEMENTS, "API", many),
            edgeRule("Product", SbomRoles.CALLS, "API", many),
            edgeRule("Product", SbomRoles.CONNECTS_TO, "Database", many),
            edgeRule("Component", SbomRoles.PROVIDED_BY, "Organization", one),
            edgeRule("Product", SbomRoles.OWNED_BY, "Organization", one),
            edgeRule("Component", SbomRoles.LICENSED_UNDER, "License", many),
            edgeRule("Component", SbomRoles.HAS_VULNERABILITY, "Vulnerability", many),
            edgeRule("Container Image", SbomRoles.HAS_VULNERABILITY, "Vulnerability", many),
            edgeRule("Product", SbomRoles.COMPLIES_WITH, "Policy", many),
        )

        return RegistryPack(
            schemas = listOf(canonicalEdge) + entitySchemas,
            edgeRules = edgeRules,
        )
    }
}
