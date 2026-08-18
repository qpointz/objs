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
    private fun text(title: String, description: String) = BoMSchemaDsl.string(title, description)

    private fun uri(title: String, description: String) =
        BoMSchemaDsl.string(title, description, format = "uri")

    private fun timestamp(title: String, description: String) =
        BoMSchemaDsl.string(title, description, format = "date-time")

    private fun number(title: String, description: String) = BoMSchemaDsl.number(title, description)

    private fun integer(title: String, description: String) = BoMSchemaDsl.integer(title, description)

    private val nameField = text("Name", "Display name of this asset")
    private val descriptionField = text("Description", "Short summary")
    private val versionField = text("Version", "Version identifier")

    private val commonOptional = mapOf(
        "description" to descriptionField,
    )

    private fun edgeRule(
        source: String,
        role: String,
        target: String,
        cardinality: BoMEdgeCardinality = BoMEdgeCardinality.ONE_TO_MANY,
        description: String? = null,
        sourceVerb: String? = null,
        targetVerb: String? = null,
        tags: List<String> = emptyList(),
    ) = BoMAllowedEdgeRule(
        sourceType = source,
        role = role,
        targetType = target,
        propertiesPolicy = BoMPropertiesPolicy.SCHEMA,
        emptyPropertiesAllowed = true,
        propertiesSchemaType = CanonicalEdgeType.meta.type,
        propertiesSchemaVersion = SCHEMA_VERSION,
        cardinality = cardinality,
        description = description,
        sourceVerb = sourceVerb,
        targetVerb = targetVerb,
        tags = tags,
    )

    private val typeColors: Map<String, String> = mapOf(
        "API" to "#fab005",
        "Artifact" to "#fab005",
        "Build" to "#82c91e",
        "Component" to "#4c6ef5",
        "Container Image" to "#228be6",
        "Container Layer" to "#fab005",
        "Database" to "#fa5252",
        "Dataset" to "#228be6",
        "Deployment" to "#40c057",
        "Environment" to "#fd7e14",
        "Host" to "#228be6",
        "Kubernetes Cluster" to "#12b886",
        "License" to "#40c057",
        "Namespace" to "#fd7e14",
        "Operating System" to "#82c91e",
        "Organization" to "#fd7e14",
        "Policy" to "#e64980",
        "Product" to "#fa5252",
        "Runtime" to "#228be6",
        "Service" to "#15aabf",
        "Source Module" to "#40c057",
        "Source Repository" to "#be4bdb",
        "Vulnerability" to "#12b886",
    )

    private fun schema(
        type: String,
        required: List<String>,
        properties: Map<String, BoMSchemaNode>,
        identifiers: Set<String> = setOf("name"),
        searchable: Set<String> = properties.keys - setOf("description"),
        description: String,
    ): BoMSchema = RegistryPack.objectSchema(
        type = type,
        version = SCHEMA_VERSION,
        title = type,
        description = description,
        fields = (properties + commonOptional).map { (name, fieldSchema) ->
            BoMSchemaDsl.field(
                name,
                fieldSchema,
                required = name in required,
                identifier = name in identifiers,
                searchable = name in searchable,
            )
        },
        attributes = mapOf("color" to (typeColors[type] ?: error("missing graph color for $type"))),
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
            usage = BoMSchemaUsage.EDGE_PROPERTIES,
            fields = listOf(
                BoMSchemaDsl.field(
                    "createdAt",
                    BoMSchemaDsl.string("Created at", "Relationship creation timestamp", format = "date-time"),
                    required = false,
                ),
                BoMSchemaDsl.field(
                    "source",
                    text("Source", "How this relationship was recorded"),
                    required = false,
                ),
                BoMSchemaDsl.field(
                    "confidence",
                    number("Confidence", "Confidence that the relationship is correct (0–1)"),
                    required = false,
                ),
            ),
        )

        val entitySchemas = listOf(
            schema(
                ComponentType.meta.type,
                listOf("name", "version", "ecosystem", "kind"),
                mapOf(
                    "name" to nameField,
                    "version" to versionField,
                    "ecosystem" to text("Ecosystem", "Package ecosystem (Maven, npm, PyPI, …)"),
                    "kind" to text("Kind", "Component kind (library, application, framework, …)"),
                    "coordinates" to text("Coordinates", "Package coordinates or PURL"),
                ),
                identifiers = setOf("name", "version", "ecosystem"),
                searchable = setOf("name", "version", "ecosystem", "kind", "coordinates"),
                description = "A software package or library in an application bill of materials",
            ),
            schema(
                ProductType.meta.type,
                listOf("name", "version"),
                mapOf(
                    "name" to nameField,
                    "version" to versionField,
                    "supplier" to text("Supplier", "Vendor or internal team that supplies the product"),
                    "lifecycle" to text("Lifecycle", "Product lifecycle stage"),
                    "homepage" to uri("Homepage", "Product home page"),
                ),
                identifiers = setOf("name", "version"),
                description = "A product composed of components, runtimes, and deployments",
            ),
            schema(
                OrganizationType.meta.type,
                listOf("name"),
                mapOf(
                    "name" to nameField,
                    "domain" to text("Domain", "Primary DNS domain"),
                    "website" to uri("Website", "Organization website"),
                    "country" to text("Country", "Country of registration or headquarters"),
                ),
                description = "A company, business unit, or team that owns or supplies software",
            ),
            schema(
                LicenseType.meta.type,
                listOf("name", "spdxId"),
                mapOf(
                    "name" to nameField,
                    "spdxId" to text("SPDX ID", "SPDX license identifier"),
                    "url" to uri("License URL", "Canonical license text URL"),
                ),
                identifiers = setOf("spdxId"),
                searchable = setOf("name", "spdxId", "url"),
                description = "A software license",
            ),
            schema(
                VulnerabilityType.meta.type,
                listOf("name", "cve", "severity"),
                mapOf(
                    "name" to nameField,
                    "cve" to text("CVE", "CVE identifier"),
                    "severity" to text("Severity", "Severity rating (critical, high, medium, low)"),
                    "cvss" to number("CVSS score", "CVSS base score"),
                ),
                identifiers = setOf("cve"),
                searchable = setOf("name", "cve", "severity"),
                description = "A known vulnerability affecting components or images",
            ),
            schema(
                BuildType.meta.type,
                listOf("name", "buildNumber", "status"),
                mapOf(
                    "name" to nameField,
                    "buildNumber" to text("Build number", "CI build number or identifier"),
                    "builder" to text("Builder", "CI system or pipeline that produced the build"),
                    "status" to text("Status", "Build status (succeeded, failed, running)"),
                ),
                identifiers = setOf("name", "buildNumber"),
                description = "A CI build that produces artifacts or images",
            ),
            schema(
                SourceRepositoryType.meta.type,
                listOf("name", "url"),
                mapOf(
                    "name" to nameField,
                    "url" to uri("Repository URL", "Clone or browse URL"),
                    "revision" to text("Revision", "Git commit or revision"),
                    "branch" to text("Branch", "Default or tracked branch"),
                ),
                identifiers = setOf("url"),
                searchable = setOf("name", "url", "revision", "branch"),
                description = "A source-control repository",
            ),
            schema(
                SourceModuleType.meta.type,
                listOf("name", "path"),
                mapOf(
                    "name" to nameField,
                    "path" to text("Path", "Path of the module inside the repository"),
                    "language" to text("Language", "Primary programming language"),
                ),
                identifiers = setOf("name", "path"),
                description = "A module or package inside a source repository",
            ),
            schema(
                ArtifactType.meta.type,
                listOf("name", "artifactType"),
                mapOf(
                    "name" to nameField,
                    "artifactType" to text("Artifact type", "Kind of artifact (jar, wheel, binary, …)"),
                    "checksum" to text("Checksum", "Content checksum"),
                    "size" to integer("Size", "Size in bytes"),
                ),
                identifiers = setOf("name", "checksum"),
                searchable = setOf("name", "artifactType", "checksum"),
                description = "A built artifact produced from source or a build",
            ),
            schema(
                ContainerImageType.meta.type,
                listOf("name", "tag"),
                mapOf(
                    "name" to nameField,
                    "tag" to text("Tag", "Image tag"),
                    "digest" to text("Digest", "Image content digest"),
                    "registry" to text("Registry", "Container registry host"),
                ),
                description = "A container image",
            ),
            schema(
                ContainerLayerType.meta.type,
                listOf("name", "digest"),
                mapOf(
                    "name" to nameField,
                    "digest" to text("Digest", "Layer digest"),
                    "size" to integer("Size", "Layer size in bytes"),
                ),
                description = "A layer inside a container image",
            ),
            schema(
                RuntimeType.meta.type,
                listOf("name", "runtimeType"),
                mapOf(
                    "name" to nameField,
                    "runtimeType" to text("Runtime type", "Runtime family (JVM, Node, Python, …)"),
                    "version" to versionField,
                ),
                description = "A language or application runtime",
            ),
            schema(
                OperatingSystemType.meta.type,
                listOf("name", "distribution"),
                mapOf(
                    "name" to nameField,
                    "distribution" to text("Distribution", "OS distribution (Ubuntu, RHEL, Windows, …)"),
                    "version" to versionField,
                    "architecture" to text("Architecture", "CPU architecture"),
                ),
                description = "An operating system used by hosts or images",
            ),
            schema(
                DeploymentType.meta.type,
                listOf("name"),
                mapOf(
                    "name" to nameField,
                    "status" to text("Status", "Deployment status"),
                    "replicas" to integer("Replicas", "Number of running replicas"),
                    "deployedAt" to timestamp("Deployed at", "When this deployment last went live"),
                ),
                description = "A deployment of software into an environment",
            ),
            schema(
                EnvironmentType.meta.type,
                listOf("name", "environment"),
                mapOf(
                    "name" to nameField,
                    "environment" to text("Stage", "Environment stage (production, staging, development)"),
                ),
                description = "A runtime environment that deployments target",
            ),
            schema(
                HostType.meta.type,
                listOf("name", "hostname"),
                mapOf(
                    "name" to nameField,
                    "hostname" to text("Hostname", "Network hostname"),
                    "ip" to text("IP address", "Primary IP address"),
                    "provider" to text("Provider", "Cloud or infrastructure provider"),
                ),
                description = "A host or virtual machine that runs deployments",
            ),
            schema(
                KubernetesClusterType.meta.type,
                listOf("name"),
                mapOf(
                    "name" to nameField,
                    "version" to text("Kubernetes version", "Cluster Kubernetes version"),
                ),
                description = "A Kubernetes cluster",
            ),
            schema(
                NamespaceType.meta.type,
                listOf("name", "namespace"),
                mapOf(
                    "name" to nameField,
                    "namespace" to text("Namespace", "Kubernetes or tenancy namespace"),
                ),
                description = "A namespace that groups deployed workloads",
            ),
            schema(
                ServiceType.meta.type,
                listOf("name"),
                mapOf(
                    "name" to nameField,
                    "protocol" to text("Protocol", "Service protocol"),
                    "endpoint" to uri("Endpoint", "Service endpoint URL"),
                ),
                description = "A network service exposed by a product",
            ),
            schema(
                ApiType.meta.type,
                listOf("name", "protocol"),
                mapOf(
                    "name" to nameField,
                    "protocol" to text("Protocol", "API protocol (HTTPS, gRPC, …)"),
                    "version" to versionField,
                ),
                description = "An API contract implemented by services",
            ),
            schema(
                DatabaseType.meta.type,
                listOf("name", "engine"),
                mapOf(
                    "name" to nameField,
                    "engine" to text("Engine", "Database engine (PostgreSQL, MongoDB, Redis, …)"),
                    "version" to text("Engine version", "Database engine version"),
                ),
                description = "A database used by applications",
            ),
            schema(
                DatasetType.meta.type,
                listOf("name", "datasetType"),
                mapOf(
                    "name" to nameField,
                    "datasetType" to text("Dataset type", "Kind of dataset (operational, analytics, reference)"),
                    "classification" to text("Classification", "Data classification (public, internal, confidential)"),
                ),
                description = "A dataset stored in a database or data platform",
            ),
            schema(
                PolicyType.meta.type,
                listOf("name"),
                mapOf(
                    "name" to nameField,
                    "policyType" to text("Policy type", "Kind of policy (license, security, quality)"),
                    "version" to versionField,
                ),
                description = "A compliance or governance policy",
            ),
        )

        // Exact triples from canonical-spec.md relationship table (+ cardinality)
        val one = BoMEdgeCardinality.ONE_TO_ONE
        val many = BoMEdgeCardinality.ONE_TO_MANY
        val edgeRules = listOf(
            edgeRule(
                "Product",
                SbomRoles.CONTAINS,
                "Component",
                many,
                description = "Product includes the software component in its bill",
                sourceVerb = "contains",
                targetVerb = "contained in",
                tags = listOf("composition"),
            ),
            edgeRule(
                "Product",
                SbomRoles.CONTAINS,
                "Artifact",
                many,
                description = "Product includes the artifact in its bill",
                sourceVerb = "contains",
                targetVerb = "contained in",
                tags = listOf("composition"),
            ),
            edgeRule(
                "Container Image",
                SbomRoles.CONTAINS,
                "Container Layer",
                many,
                description = "Image is built from filesystem layers",
                sourceVerb = "contains",
                targetVerb = "contained in",
                tags = listOf("composition"),
            ),
            edgeRule(
                "Database",
                SbomRoles.CONTAINS,
                "Dataset",
                many,
                description = "Database hosts the dataset",
                sourceVerb = "contains",
                targetVerb = "contained in",
                tags = listOf("composition"),
            ),
            edgeRule(
                "Source Repository",
                SbomRoles.CONTAINS,
                "Source Module",
                many,
                description = "Repository contains the source module",
                sourceVerb = "contains",
                targetVerb = "contained in",
                tags = listOf("composition"),
            ),
            edgeRule(
                "Component",
                SbomRoles.DEPENDS_ON,
                "Component",
                many,
                description = "Component requires the other component at build or runtime",
                sourceVerb = "depends on",
                targetVerb = "depended on by",
            ),
            edgeRule(
                "Source Module",
                SbomRoles.PRODUCES,
                "Artifact",
                many,
                description = "Source module produces the artifact",
                sourceVerb = "produces",
                targetVerb = "produced by",
            ),
            edgeRule(
                "Build",
                SbomRoles.BUILDS,
                "Artifact",
                many,
                description = "Build produces the artifact",
                sourceVerb = "builds",
                targetVerb = "built by",
            ),
            edgeRule(
                "Build",
                SbomRoles.BUILDS,
                "Container Image",
                many,
                description = "Build produces the container image",
                sourceVerb = "builds",
                targetVerb = "built by",
            ),
            edgeRule(
                "Build",
                SbomRoles.USES,
                "Component",
                many,
                description = "Build compiles or consumes the component",
                sourceVerb = "uses",
                targetVerb = "used by",
            ),
            edgeRule(
                "Container Image",
                SbomRoles.PACKAGES,
                "Artifact",
                many,
                description = "Image packages the artifact in its filesystem",
                sourceVerb = "packages",
                targetVerb = "packaged in",
            ),
            edgeRule(
                "Container Image",
                SbomRoles.BASED_ON,
                "Operating System",
                one,
                description = "Image is based on the operating system",
                sourceVerb = "based on",
                targetVerb = "base of",
            ),
            edgeRule(
                "Product",
                SbomRoles.RUNS_ON,
                "Runtime",
                one,
                description = "Product runs on the language runtime",
                sourceVerb = "runs on",
                targetVerb = "runs",
            ),
            edgeRule(
                "Runtime",
                SbomRoles.RUNS_ON,
                "Operating System",
                one,
                description = "Runtime runs on the operating system",
                sourceVerb = "runs on",
                targetVerb = "hosts",
            ),
            edgeRule(
                "Deployment",
                SbomRoles.DEPLOYS,
                "Container Image",
                one,
                description = "Deployment rolls out the container image",
                sourceVerb = "deploys",
                targetVerb = "deployed by",
            ),
            edgeRule(
                "Deployment",
                SbomRoles.TARGETS,
                "Environment",
                one,
                description = "Deployment targets the runtime environment",
                sourceVerb = "targets",
                targetVerb = "targeted by",
            ),
            edgeRule(
                "Deployment",
                SbomRoles.RUNS_ON,
                "Host",
                many,
                description = "Deployment runs on the host",
                sourceVerb = "runs on",
                targetVerb = "hosts",
            ),
            edgeRule(
                "Host",
                SbomRoles.MEMBER_OF,
                "Kubernetes Cluster",
                one,
                description = "Host is a node in the Kubernetes cluster",
                sourceVerb = "member of",
                targetVerb = "includes",
            ),
            edgeRule(
                "Deployment",
                SbomRoles.LOCATED_IN,
                "Namespace",
                one,
                description = "Deployment runs in the namespace",
                sourceVerb = "located in",
                targetVerb = "contains",
            ),
            edgeRule(
                "Service",
                SbomRoles.IMPLEMENTS,
                "API",
                many,
                description = "Service implements the API contract",
                sourceVerb = "implements",
                targetVerb = "implemented by",
            ),
            edgeRule(
                "Product",
                SbomRoles.CALLS,
                "API",
                many,
                description = "Product invokes the API",
                sourceVerb = "calls",
                targetVerb = "called by",
            ),
            edgeRule(
                "Product",
                SbomRoles.CONNECTS_TO,
                "Database",
                many,
                description = "Product connects to the database",
                sourceVerb = "connects to",
                targetVerb = "connected from",
            ),
            edgeRule(
                "Component",
                SbomRoles.PROVIDED_BY,
                "Organization",
                one,
                description = "Organization supplies the component",
                sourceVerb = "provided by",
                targetVerb = "provides",
            ),
            edgeRule(
                "Product",
                SbomRoles.OWNED_BY,
                "Organization",
                one,
                description = "Organization owns the product",
                sourceVerb = "owned by",
                targetVerb = "owns",
            ),
            edgeRule(
                "Component",
                SbomRoles.LICENSED_UNDER,
                "License",
                many,
                description = "Component is distributed under the license",
                sourceVerb = "licensed under",
                targetVerb = "licenses",
            ),
            edgeRule(
                "Component",
                SbomRoles.HAS_VULNERABILITY,
                "Vulnerability",
                many,
                description = "Component is affected by the vulnerability",
                sourceVerb = "has",
                targetVerb = "found in",
            ),
            edgeRule(
                "Container Image",
                SbomRoles.HAS_VULNERABILITY,
                "Vulnerability",
                many,
                description = "Image is affected by the vulnerability",
                sourceVerb = "has",
                targetVerb = "found in",
            ),
            edgeRule(
                "Product",
                SbomRoles.COMPLIES_WITH,
                "Policy",
                many,
                description = "Product is governed by the policy",
                sourceVerb = "complies with",
                targetVerb = "constrains",
            ),
        )

        return RegistryPack(
            schemas = listOf(canonicalEdge) + entitySchemas,
            edgeRules = edgeRules,
        )
    }
}
