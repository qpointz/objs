package org.poc.objs.sbom

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.poc.objs.core.domain.BoMAllowedEdgeCatalog
import org.poc.objs.core.domain.BoMSchemaCatalog
import org.poc.objs.core.domain.BoMSchemaUsage
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.core.persistence.BoMNamedGraphStore
import org.poc.objs.core.persistence.BoMPoolEntityReader
import org.poc.objs.core.persistence.ObjsCoreAutoConfiguration
import org.poc.objs.sbom.annotations.Provenance
import org.poc.objs.sbom.annotations.SbomContext
import org.poc.objs.sbom.builder.SbomGraphBuilder
import org.poc.objs.sbom.model.ApiPayload
import org.poc.objs.sbom.model.ApiType
import org.poc.objs.sbom.model.ArtifactPayload
import org.poc.objs.sbom.model.ArtifactType
import org.poc.objs.sbom.model.ContainerImagePayload
import org.poc.objs.sbom.model.ContainerImageType
import org.poc.objs.sbom.model.ContainerLayerPayload
import org.poc.objs.sbom.model.ContainerLayerType
import org.poc.objs.sbom.model.DatabasePayload
import org.poc.objs.sbom.model.DatabaseType
import org.poc.objs.sbom.model.DatasetPayload
import org.poc.objs.sbom.model.DatasetType
import org.poc.objs.sbom.model.DeploymentPayload
import org.poc.objs.sbom.model.DeploymentType
import org.poc.objs.sbom.model.EnvironmentPayload
import org.poc.objs.sbom.model.EnvironmentType
import org.poc.objs.sbom.model.HostPayload
import org.poc.objs.sbom.model.HostType
import org.poc.objs.sbom.model.KubernetesClusterPayload
import org.poc.objs.sbom.model.KubernetesClusterType
import org.poc.objs.sbom.model.NamespacePayload
import org.poc.objs.sbom.model.NamespaceType
import org.poc.objs.sbom.model.OperatingSystemPayload
import org.poc.objs.sbom.model.OperatingSystemType
import org.poc.objs.sbom.model.PolicyPayload
import org.poc.objs.sbom.model.PolicyType
import org.poc.objs.sbom.model.ProductPayload
import org.poc.objs.sbom.model.ProductType
import org.poc.objs.sbom.model.RuntimePayload
import org.poc.objs.sbom.model.RuntimeType
import org.poc.objs.sbom.model.ServicePayload
import org.poc.objs.sbom.model.ServiceType
import org.poc.objs.sbom.model.SourceModulePayload
import org.poc.objs.sbom.model.SourceModuleType
import org.poc.objs.sbom.model.SourceRepositoryPayload
import org.poc.objs.sbom.model.SourceRepositoryType
import org.poc.objs.sbom.registry.SbomRegistry
import org.poc.objs.sbom.registry.SbomRoles
import org.poc.objs.sbom.service.SbomService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@ImportAutoConfiguration(ObjsCoreAutoConfiguration::class)
@Import(BoMGraphStore::class, BoMNamedGraphStore::class, BoMPoolEntityReader::class, SbomService::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:sbom-canonical;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
    ],
)
class CanonicalOntologyTest {

    @SpringBootConfiguration
    class TestApp

    @Autowired
    lateinit var sbom: SbomService

    @Autowired
    lateinit var schemas: BoMSchemaCatalog

    @Autowired
    lateinit var edges: BoMAllowedEdgeCatalog

    @BeforeEach
    fun reset() {
        schemas.clear()
        edges.clear()
        val field = SbomService::class.java.getDeclaredField("packRegistered")
        field.isAccessible = true
        field.setBoolean(sbom, false)
        sbom.ensureRegistry()
    }

    @Test
    fun shouldRegisterFullCanonicalPack() {
        val pack = SbomRegistry.pack()
        // CanonicalEdge + 23 entity types
        assertThat(pack.schemas).hasSize(24)
        // Exact relationship table in canonical-spec.md
        assertThat(pack.edgeRules).hasSize(28)
        assertThat(pack.edgeRules)
            .allSatisfy {
                assertThat(it.propertiesSchemaType).isEqualTo("CanonicalEdge")
                assertThat(it.propertiesSchemaVersion).isEqualTo("1.0.0")
            }
        assertThat(schemas.get("Container Image", "1.0.0")).isNotNull
        assertThat(schemas.get("Kubernetes Cluster", "1.0.0")).isNotNull
        assertThat(edges.all()).hasSize(28)

        @Suppress("UNCHECKED_CAST")
        val artifactProperties =
            schemas.get("Artifact", "1.0.0")!!.toJsonSchema()["properties"] as Map<String, Map<String, Any?>>
        assertThat(artifactProperties["size"]!!["type"]).isEqualTo("integer")

        @Suppress("UNCHECKED_CAST")
        val productProperties =
            schemas.get("Product", "1.0.0")!!.toJsonSchema()["properties"] as Map<String, Map<String, Any?>>
        assertThat(productProperties["homepage"]!!["format"]).isEqualTo("uri")

        assertThat(schemas.get("CanonicalEdge", "1.0.0")!!.usage)
            .isEqualTo(BoMSchemaUsage.EDGE_PROPERTIES)
        assertThat(schemas.get("Component", "1.0.0")!!.usage)
            .isEqualTo(BoMSchemaUsage.ENTITY)
    }

    @Test
    fun shouldPersistCrossWaveCanonicalGraph() {
        val ctx = SbomContext("payments-api", "2.3.1")
        val b = SbomGraphBuilder(ctx)
        val p = Provenance.detected()

        val product = b.add(ProductType.entity(ProductPayload("Payments API", "2.3.1")), p, "product")
        val repo = b.add(
            SourceRepositoryType.entity(SourceRepositoryPayload("payments", "https://git.example/payments")),
            p,
            "repo",
        )
        val module = b.add(
            SourceModuleType.entity(SourceModulePayload("api", "services/api")),
            p,
            "module",
        )
        val artifact = b.add(
            ArtifactType.entity(ArtifactPayload("payments-api.jar", "jar")),
            p,
            "artifact",
        )
        val image = b.add(
            ContainerImageType.entity(ContainerImagePayload("payments-api", "2.3.1")),
            p,
            "image",
        )
        val layer = b.add(
            ContainerLayerType.entity(ContainerLayerPayload("base", "sha256:abc")),
            p,
            "layer",
        )
        val os = b.add(
            OperatingSystemType.entity(OperatingSystemPayload("Alpine", "alpine")),
            p,
            "os",
        )
        val runtime = b.add(
            RuntimeType.entity(RuntimePayload("JVM", "jvm", "21")),
            p,
            "runtime",
        )
        val env = b.add(
            EnvironmentType.entity(EnvironmentPayload("prod", "production")),
            p,
            "env",
        )
        val host = b.add(HostType.entity(HostPayload("worker-1", "worker-1.local")), p, "host")
        val cluster = b.add(
            KubernetesClusterType.entity(KubernetesClusterPayload("prod-cluster")),
            p,
            "cluster",
        )
        val ns = b.add(NamespaceType.entity(NamespacePayload("payments", "payments")), p, "ns")
        val deployment = b.add(
            DeploymentType.entity(DeploymentPayload("payments-api")),
            p,
            "deploy",
        )
        val service = b.add(ServiceType.entity(ServicePayload("payments-svc")), p, "svc")
        val api = b.add(ApiType.entity(ApiPayload("Payments API", "https")), p, "api")
        val db = b.add(DatabaseType.entity(DatabasePayload("payments-db", "postgres")), p, "db")
        val dataset = b.add(
            DatasetType.entity(DatasetPayload("orders", "table")),
            p,
            "dataset",
        )
        val policy = b.add(PolicyType.entity(PolicyPayload("Secure Build")), p, "policy")

        b.link(repo, SbomRoles.CONTAINS, module)
        b.link(module, SbomRoles.PRODUCES, artifact)
        b.link(product, SbomRoles.CONTAINS, artifact)
        b.link(image, SbomRoles.CONTAINS, layer)
        b.link(image, SbomRoles.PACKAGES, artifact)
        b.link(image, SbomRoles.BASED_ON, os)
        b.link(product, SbomRoles.RUNS_ON, runtime)
        b.link(runtime, SbomRoles.RUNS_ON, os)
        b.link(deployment, SbomRoles.DEPLOYS, image)
        b.link(deployment, SbomRoles.TARGETS, env)
        b.link(deployment, SbomRoles.RUNS_ON, host)
        b.link(host, SbomRoles.MEMBER_OF, cluster)
        b.link(deployment, SbomRoles.LOCATED_IN, ns)
        b.link(service, SbomRoles.IMPLEMENTS, api)
        b.link(product, SbomRoles.CALLS, api)
        b.link(product, SbomRoles.CONNECTS_TO, db)
        b.link(db, SbomRoles.CONTAINS, dataset)
        b.link(product, SbomRoles.COMPLIES_WITH, policy)

        val result = sbom.save(b.build())
        assertThat(result.isValid)
            .withFailMessage { result.issues.joinToString { "${it.code}: ${it.message}" } }
            .isTrue()

        val bom = sbom.getSbom(ctx)
        assertThat(bom.entities).hasSize(18)
        assertThat(bom.edges).hasSize(18)
    }
}
