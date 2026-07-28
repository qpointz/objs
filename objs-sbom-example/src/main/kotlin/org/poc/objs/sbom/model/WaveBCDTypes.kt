package org.poc.objs.sbom.model

import org.poc.objs.core.typed.EntityTypeMeta
import org.poc.objs.core.typed.TypedEntity
import java.util.UUID

// --- Wave B: build & packaging ---

data class SourceRepositoryPayload(
    val name: String,
    val url: String,
    val revision: String? = null,
    val branch: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object SourceRepositoryType {
    val meta = EntityTypeMeta(type = "Source Repository", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: SourceRepositoryPayload, id: UUID? = null) =
        TypedEntity(meta, SourceRepositoryPayload::class.java, id, payload)
}

data class SourceModulePayload(
    val name: String,
    val path: String,
    val language: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object SourceModuleType {
    val meta = EntityTypeMeta(type = "Source Module", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: SourceModulePayload, id: UUID? = null) =
        TypedEntity(meta, SourceModulePayload::class.java, id, payload)
}

data class ArtifactPayload(
    val name: String,
    val artifactType: String,
    val checksum: String? = null,
    val size: Long? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object ArtifactType {
    val meta = EntityTypeMeta(type = "Artifact", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: ArtifactPayload, id: UUID? = null) =
        TypedEntity(meta, ArtifactPayload::class.java, id, payload)
}

data class ContainerImagePayload(
    val name: String,
    val tag: String,
    val digest: String? = null,
    val registry: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object ContainerImageType {
    val meta = EntityTypeMeta(type = "Container Image", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: ContainerImagePayload, id: UUID? = null) =
        TypedEntity(meta, ContainerImagePayload::class.java, id, payload)
}

data class ContainerLayerPayload(
    val name: String,
    val digest: String,
    val size: Long? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object ContainerLayerType {
    val meta = EntityTypeMeta(type = "Container Layer", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: ContainerLayerPayload, id: UUID? = null) =
        TypedEntity(meta, ContainerLayerPayload::class.java, id, payload)
}

// --- Wave C: runtime & deploy ---

data class RuntimePayload(
    val name: String,
    val runtimeType: String,
    val version: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object RuntimeType {
    val meta = EntityTypeMeta(type = "Runtime", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: RuntimePayload, id: UUID? = null) =
        TypedEntity(meta, RuntimePayload::class.java, id, payload)
}

data class OperatingSystemPayload(
    val name: String,
    val distribution: String,
    val version: String? = null,
    val architecture: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object OperatingSystemType {
    val meta = EntityTypeMeta(type = "Operating System", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: OperatingSystemPayload, id: UUID? = null) =
        TypedEntity(meta, OperatingSystemPayload::class.java, id, payload)
}

data class DeploymentPayload(
    val name: String,
    val status: String? = null,
    val replicas: Int? = null,
    val deployedAt: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object DeploymentType {
    val meta = EntityTypeMeta(type = "Deployment", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: DeploymentPayload, id: UUID? = null) =
        TypedEntity(meta, DeploymentPayload::class.java, id, payload)
}

data class EnvironmentPayload(
    val name: String,
    val environment: String,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object EnvironmentType {
    val meta = EntityTypeMeta(type = "Environment", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: EnvironmentPayload, id: UUID? = null) =
        TypedEntity(meta, EnvironmentPayload::class.java, id, payload)
}

data class HostPayload(
    val name: String,
    val hostname: String,
    val ip: String? = null,
    val provider: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object HostType {
    val meta = EntityTypeMeta(type = "Host", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: HostPayload, id: UUID? = null) =
        TypedEntity(meta, HostPayload::class.java, id, payload)
}

data class KubernetesClusterPayload(
    val name: String,
    val version: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object KubernetesClusterType {
    val meta = EntityTypeMeta(type = "Kubernetes Cluster", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: KubernetesClusterPayload, id: UUID? = null) =
        TypedEntity(meta, KubernetesClusterPayload::class.java, id, payload)
}

data class NamespacePayload(
    val name: String,
    val namespace: String,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object NamespaceType {
    val meta = EntityTypeMeta(type = "Namespace", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: NamespacePayload, id: UUID? = null) =
        TypedEntity(meta, NamespacePayload::class.java, id, payload)
}

// --- Wave D: architecture & compliance ---

data class ServicePayload(
    val name: String,
    val protocol: String? = null,
    val endpoint: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object ServiceType {
    val meta = EntityTypeMeta(type = "Service", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: ServicePayload, id: UUID? = null) =
        TypedEntity(meta, ServicePayload::class.java, id, payload)
}

data class ApiPayload(
    val name: String,
    val protocol: String,
    val version: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object ApiType {
    val meta = EntityTypeMeta(type = "API", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: ApiPayload, id: UUID? = null) =
        TypedEntity(meta, ApiPayload::class.java, id, payload)
}

data class DatabasePayload(
    val name: String,
    val engine: String,
    val version: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object DatabaseType {
    val meta = EntityTypeMeta(type = "Database", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: DatabasePayload, id: UUID? = null) =
        TypedEntity(meta, DatabasePayload::class.java, id, payload)
}

data class DatasetPayload(
    val name: String,
    val datasetType: String,
    val classification: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object DatasetType {
    val meta = EntityTypeMeta(type = "Dataset", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: DatasetPayload, id: UUID? = null) =
        TypedEntity(meta, DatasetPayload::class.java, id, payload)
}

data class PolicyPayload(
    val name: String,
    val policyType: String? = null,
    val version: String? = null,
    val description: String? = null,
    val labels: List<String>? = null,
    val attributes: Map<String, Any?>? = null,
)

object PolicyType {
    val meta = EntityTypeMeta(type = "Policy", schemaVersion = SCHEMA_VERSION)
    fun entity(payload: PolicyPayload, id: UUID? = null) =
        TypedEntity(meta, PolicyPayload::class.java, id, payload)
}
