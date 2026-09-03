package org.poc.objs.gremlin.core

import org.apache.tinkerpop.gremlin.jsr223.GremlinLangScriptEngine
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.poc.objs.api.domain.DefaultGraphFragmentPolicy
import org.poc.objs.api.domain.ResolvedGraphFragment
import org.poc.objs.api.match.Matcher
import org.poc.objs.api.store.GraphStore
import org.poc.objs.gremlin.core.materialize.GremlinMaterializer
import org.poc.objs.gremlin.core.materialize.EnvelopeMaterializationStrategy
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.script.Bindings

/**
 * Selects a BoM subgraph (or accepts one), materializes it, and evaluates a **gremlin-lang** script.
 */
class GremlinEngine(
    private val materializer: GremlinMaterializer = GremlinMaterializer(),
) {
    /**
     * Explorer/Composer parity: run [matcher] via [GraphStore.select], then [eval].
     */
    fun selectAndEval(
        store: GraphStore,
        matcher: Matcher,
        script: String,
        bindings: Map<String, Any?>? = null,
        strategy: String = EnvelopeMaterializationStrategy.NAME,
        options: GremlinTraversalOptions? = null,
    ): GremlinResult =
        eval(
            subgraph = DefaultGraphFragmentPolicy.resolve(store.select(matcher)),
            script = script,
            bindings = bindings,
            strategy = strategy,
            options = options,
        )

    fun eval(
        subgraph: ResolvedGraphFragment,
        script: String,
        bindings: Map<String, Any?>? = null,
        strategy: String = EnvelopeMaterializationStrategy.NAME,
        options: GremlinTraversalOptions? = null,
    ): GremlinResult {
        val opts = options ?: GremlinTraversalOptions()
        val language = try {
            opts.effectiveLanguage()
        } catch (ex: IllegalArgumentException) {
            throw GremlinEvalException(ex.message ?: "Invalid language", ex)
        }
        val timeoutSeconds = try {
            opts.effectiveTimeoutSeconds()
        } catch (ex: IllegalArgumentException) {
            throw GremlinEvalException(ex.message ?: "Invalid timeout", ex)
        }
        if (script.isBlank()) {
            throw GremlinEvalException("script must not be blank")
        }

        val started = System.nanoTime()
        val graph = try {
            materializer.materialize(subgraph, strategy)
        } catch (ex: IllegalArgumentException) {
            throw GremlinEvalException(ex.message ?: "Materialization failed", ex)
        }

        val raw = try {
            evalWithTimeout(graph.traversal(), script, bindings.orEmpty(), timeoutSeconds)
        } finally {
            graph.close()
        }

        val items = GremlinResultProjector.projectItems(raw)
        val inputContents = subgraph.asGraphContents()
        val subgraph2 = GremlinResultProjector.buildSubgraph2(items, inputContents)
        val table = GremlinResultProjector.buildTable(items)
        val scalar = when {
            items.size == 1 && items[0] is GremlinItem.Scalar ->
                (items[0] as GremlinItem.Scalar).value
            else -> null
        }
        val primary = GremlinResultProjector.inferPrimary(items, subgraph2)
        val durationMs = (System.nanoTime() - started) / 1_000_000L

        return GremlinResult(
            primary = primary,
            items = items,
            contents = subgraph2,
            views = GremlinViews(
                graph = subgraph2,
                table = table,
                scalar = scalar,
            ),
            meta = GremlinMeta(
                strategy = strategy,
                language = language,
                subgraph1Stats = GremlinGraphStats(
                    entities = subgraph.entities.size,
                    edges = subgraph.edges.size,
                ),
                subgraph2Stats = subgraph2?.let {
                    GremlinGraphStats(entities = it.entities.size, edges = it.edges.size)
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
                    throw GremlinEvalException("gremlin-lang script failed: ${ex.message}", ex)
                }
                flattenEvalResult(evaluated)
            }
            return try {
                future.get(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            } catch (ex: TimeoutException) {
                future.cancel(true)
                throw GremlinEvalException(
                    "Gremlin evaluation timed out after ${timeoutSeconds}s",
                    ex,
                )
            } catch (ex: ExecutionException) {
                val cause = ex.cause
                if (cause is GremlinEvalException) throw cause
                throw GremlinEvalException(
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
