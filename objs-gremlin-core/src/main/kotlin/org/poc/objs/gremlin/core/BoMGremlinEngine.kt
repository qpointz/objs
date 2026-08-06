package org.poc.objs.gremlin.core

import org.apache.tinkerpop.gremlin.jsr223.GremlinLangScriptEngine
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.poc.objs.core.domain.BoMSubgraph
import org.poc.objs.core.match.BoMMatcher
import org.poc.objs.core.persistence.BoMGraphStore
import org.poc.objs.gremlin.core.materialize.BoMGremlinMaterializer
import org.poc.objs.gremlin.core.materialize.EnvelopeMaterializationStrategy
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.script.Bindings

/**
 * Selects a BoM subgraph (or accepts one), materializes it, and evaluates a **gremlin-lang** script.
 */
class BoMGremlinEngine(
    private val materializer: BoMGremlinMaterializer = BoMGremlinMaterializer(),
) {
    /**
     * Explorer/Composer parity: run [matcher] via [BoMGraphStore.selectSubgraph], then [eval].
     */
    fun selectAndEval(
        store: BoMGraphStore,
        matcher: BoMMatcher,
        script: String,
        bindings: Map<String, Any?>? = null,
        strategy: String = EnvelopeMaterializationStrategy.NAME,
        options: BoMGremlinTraversalOptions? = null,
    ): BoMGremlinResult =
        eval(
            subgraph = store.selectSubgraph(matcher),
            script = script,
            bindings = bindings,
            strategy = strategy,
            options = options,
        )

    fun eval(
        subgraph: BoMSubgraph,
        script: String,
        bindings: Map<String, Any?>? = null,
        strategy: String = EnvelopeMaterializationStrategy.NAME,
        options: BoMGremlinTraversalOptions? = null,
    ): BoMGremlinResult {
        val opts = options ?: BoMGremlinTraversalOptions()
        val language = try {
            opts.effectiveLanguage()
        } catch (ex: IllegalArgumentException) {
            throw BoMGremlinEvalException(ex.message ?: "Invalid language", ex)
        }
        val timeoutSeconds = try {
            opts.effectiveTimeoutSeconds()
        } catch (ex: IllegalArgumentException) {
            throw BoMGremlinEvalException(ex.message ?: "Invalid timeout", ex)
        }
        if (script.isBlank()) {
            throw BoMGremlinEvalException("script must not be blank")
        }

        val started = System.nanoTime()
        val graph = try {
            materializer.materialize(subgraph, strategy)
        } catch (ex: IllegalArgumentException) {
            throw BoMGremlinEvalException(ex.message ?: "Materialization failed", ex)
        }

        val raw = try {
            evalWithTimeout(graph.traversal(), script, bindings.orEmpty(), timeoutSeconds)
        } finally {
            graph.close()
        }

        val items = BoMGremlinResultProjector.projectItems(raw)
        val subgraph2 = BoMGremlinResultProjector.buildSubgraph2(items, subgraph)
        val table = BoMGremlinResultProjector.buildTable(items)
        val scalar = when {
            items.size == 1 && items[0] is BoMGremlinItem.Scalar ->
                (items[0] as BoMGremlinItem.Scalar).value
            else -> null
        }
        val primary = BoMGremlinResultProjector.inferPrimary(items, subgraph2)
        val durationMs = (System.nanoTime() - started) / 1_000_000L

        return BoMGremlinResult(
            primary = primary,
            items = items,
            subgraph = subgraph2,
            views = BoMGremlinViews(
                graph = subgraph2,
                table = table,
                scalar = scalar,
            ),
            meta = BoMGremlinMeta(
                strategy = strategy,
                language = language,
                subgraph1Stats = BoMGremlinGraphStats(
                    entities = subgraph.entities.size,
                    edges = subgraph.edges.size,
                ),
                subgraph2Stats = subgraph2?.let {
                    BoMGremlinGraphStats(entities = it.entities.size, edges = it.edges.size)
                },
                resultCount = items.size,
                durationMs = durationMs,
            ),
        )
    }

    private fun evalWithTimeout(
        g: org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource,
        script: String,
        extraBindings: Map<String, Any?>,
        timeoutSeconds: Int,
    ): List<Any?> {
        val executor = Executors.newSingleThreadExecutor { r ->
            Thread(r, "objs-gremlin-eval").apply { isDaemon = true }
        }
        try {
            val future = executor.submit<List<Any?>> {
                val engine = GremlinLangScriptEngine()
                val scriptBindings: Bindings = engine.createBindings().apply {
                    put("g", g)
                    put("graph", g.graph)
                    extraBindings.forEach { (k, v) -> put(k, v) }
                }
                val evaluated = try {
                    engine.eval(script, scriptBindings)
                } catch (ex: Exception) {
                    throw BoMGremlinEvalException("gremlin-lang script failed: ${ex.message}", ex)
                }
                flattenEvalResult(evaluated)
            }
            return try {
                future.get(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            } catch (ex: TimeoutException) {
                future.cancel(true)
                throw BoMGremlinEvalException(
                    "Gremlin evaluation timed out after ${timeoutSeconds}s",
                    ex,
                )
            } catch (ex: ExecutionException) {
                val cause = ex.cause
                if (cause is BoMGremlinEvalException) throw cause
                throw BoMGremlinEvalException(
                    cause?.message ?: "Gremlin evaluation failed",
                    cause ?: ex,
                )
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun flattenEvalResult(evaluated: Any?): List<Any?> =
        when (evaluated) {
            null -> listOf(null)
            is Traversal<*, *> -> evaluated.toList() as List<Any?>
            is Iterable<*> -> evaluated.toList()
            is Array<*> -> evaluated.toList()
            else -> listOf(evaluated)
        }
}
