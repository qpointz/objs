package org.poc.objs.core.seed

import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service

data class SeedStartupResourceResult(
    val seedKey: String,
    val location: String,
    val fingerprint: String,
    val status: SeedLedgerStatus,
    val importResult: SeedImportResult? = null,
    val error: String? = null,
)

data class SeedStartupResult(
    val resources: List<SeedStartupResourceResult> = emptyList(),
) {
    val hasFailures: Boolean get() = resources.any { it.status == SeedLedgerStatus.FAILED }
}

/**
 * Loads ordered seed resources at startup using the shared [SeedImporter] and durable ledger.
 */
@Service
class BoMSeedStartupLoader(
    private val properties: BoMSeedProperties,
    private val resourceLoader: ResourceLoader,
    private val importer: SeedImporter,
    private val ledger: BoMSeedLedger,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun loadConfiguredResources(): SeedStartupResult {
        if (!properties.enabled || properties.resources.isEmpty()) {
            return SeedStartupResult()
        }

        val seenKeys = linkedSetOf<String>()
        val results = mutableListOf<SeedStartupResourceResult>()

        for (configuredLocation in properties.resources) {
            val location = configuredLocation.trim()
            if (location.isEmpty()) {
                throw IllegalStateException("objs.seeds.resources[] must not be blank")
            }
            val seedKey = BoMSeedResourceIdentity.ledgerKey(location)
            if (!seenKeys.add(seedKey)) {
                throw IllegalStateException("Duplicate seed ledger key: $seedKey")
            }

            val result = loadOne(seedKey, location)
            results += result
            if (result.status == SeedLedgerStatus.FAILED && properties.onFailure == SeedFailureMode.FAIL_FAST) {
                throw SeedStartupException(
                    "Seed resource '$seedKey' failed: ${result.error}",
                    SeedStartupResult(results),
                )
            }
        }
        return SeedStartupResult(results)
    }

    private fun loadOne(seedKey: String, location: String): SeedStartupResourceResult {
        val resource = resourceLoader.getResource(location)
        if (!resource.exists()) {
            val error = "Seed resource not found: $location"
            ledger.recordFailure(seedKey, "missing", error)
            return SeedStartupResourceResult(
                seedKey = seedKey,
                location = location,
                fingerprint = "missing",
                status = SeedLedgerStatus.FAILED,
                error = error,
            )
        }

        val bytes = readBytes(resource)
        val fingerprint = BoMSeedResourceIdentity.fingerprint(bytes)
        if (ledger.shouldSkip(seedKey, fingerprint)) {
            log.info("Skipping unchanged seed resource {} ({})", seedKey, fingerprint)
            ledger.recordSkipped(seedKey, fingerprint)
            return SeedStartupResourceResult(
                seedKey = seedKey,
                location = location,
                fingerprint = fingerprint,
                status = SeedLedgerStatus.SKIPPED,
            )
        }

        return try {
            val yaml = bytes.toString(Charsets.UTF_8)
            val importResult = importer.importYaml(yaml)
            ledger.recordSuccess(seedKey, fingerprint)
            log.info(
                "Applied seed resource {} ({}): applied={}",
                seedKey,
                fingerprint,
                importResult.appliedByKind(),
            )
            SeedStartupResourceResult(
                seedKey = seedKey,
                location = location,
                fingerprint = fingerprint,
                status = SeedLedgerStatus.SUCCESS,
                importResult = importResult,
            )
        } catch (ex: SeedImportException) {
            val error = ex.message ?: "Seed import failed"
            val details = ex.result.allErrors().joinToString("; ") { "${it.code} ${it.path}: ${it.message}" }
            ledger.recordFailure(seedKey, fingerprint, error)
            log.error("Failed seed resource {} ({}): {} {}", seedKey, fingerprint, error, details)
            SeedStartupResourceResult(
                seedKey = seedKey,
                location = location,
                fingerprint = fingerprint,
                status = SeedLedgerStatus.FAILED,
                importResult = ex.result,
                error = error,
            )
        } catch (ex: Exception) {
            val error = ex.message ?: ex.javaClass.simpleName
            ledger.recordFailure(seedKey, fingerprint, error)
            log.error("Failed seed resource {} ({}): {}", seedKey, fingerprint, error, ex)
            SeedStartupResourceResult(
                seedKey = seedKey,
                location = location,
                fingerprint = fingerprint,
                status = SeedLedgerStatus.FAILED,
                error = error,
            )
        }
    }

    private fun readBytes(resource: Resource): ByteArray =
        resource.inputStream.use { it.readBytes() }
}

class SeedStartupException(
    message: String,
    val result: SeedStartupResult,
) : RuntimeException(message)
