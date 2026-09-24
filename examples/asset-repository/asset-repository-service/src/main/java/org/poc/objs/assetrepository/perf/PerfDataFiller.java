package org.poc.objs.assetrepository.perf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.poc.objs.api.domain.Edge;
import org.poc.objs.api.domain.EdgeMutation;
import org.poc.objs.api.domain.EntityMutation;
import org.poc.objs.api.domain.GraphMutation;
import org.poc.objs.api.validation.ValidationResult;
import org.poc.objs.assetrepository.domain.CollectionEntity;
import org.poc.objs.assetrepository.domain.CollectionTypeSpec;
import org.poc.objs.assetrepository.domain.ObjectWriteMode;
import org.poc.objs.assetrepository.service.CollectionService;
import org.poc.objs.core.persistence.NamedGraphStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Absolute-N noise fill distributed across one or more collections/graphs via batched
 * {@link NamedGraphStore#mutate} (preassigned UUIDs — no per-row identity scans).
 *
 * <p>{@code objects} / {@code edges} are <em>totals</em> split across {@code graphs}.
 * {@code versions} is applied <em>per graph</em>.
 */
@Component
@Profile("perf")
@Order(1500)
@EnableConfigurationProperties(PerfProperties.class)
public class PerfDataFiller implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PerfDataFiller.class);

    private final PerfProperties props;
    private final CollectionService collections;
    private final NamedGraphStore namedGraphs;
    private final PerfFillGate gate;

    public PerfDataFiller(
            PerfProperties props,
            CollectionService collections,
            NamedGraphStore namedGraphs,
            PerfFillGate gate
    ) {
        this.props = props;
        this.collections = collections;
        this.namedGraphs = namedGraphs;
        this.gate = gate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            fill();
        } finally {
            gate.markReady();
            log.info("Perf fill gate open — /api/** accepts traffic");
        }
    }

    private void fill() {
        int graphCount = props.getGraphs();
        int objectTotal = props.getObjects();
        int edgeTotal = props.getEdges();
        int versionsPerGraph = props.getVersions();
        int batchSize = Math.max(1, props.getBatchSize());
        String prefix = props.getCollection() == null || props.getCollection().isBlank()
                ? "perf-noise"
                : props.getCollection().trim();

        if (graphCount < 1) {
            throw new IllegalStateException("ar.perf.graphs must be >= 1");
        }
        if (objectTotal < 0 || edgeTotal < 0 || versionsPerGraph < 0) {
            throw new IllegalStateException(
                    "ar.perf.objects, ar.perf.edges, and ar.perf.versions must be >= 0");
        }
        if (edgeTotal > 0 && objectTotal < 2) {
            throw new IllegalStateException("ar.perf.edges > 0 requires ar.perf.objects >= 2");
        }

        long started = System.nanoTime();
        log.info(
                "Perf fill starting: graphs={} objectsTotal={} edgesTotal={} versionsPerGraph={} "
                        + "prefix='{}' batchSize={}",
                graphCount,
                objectTotal,
                edgeTotal,
                versionsPerGraph,
                prefix,
                batchSize);
        log.info("Perf fill gate closed — /api/** returns 503 until fill finishes");

        int objectsWritten = 0;
        int edgesWritten = 0;
        int versionsWritten = 0;
        ProgressTracker graphProgress = new ProgressTracker("graphs", graphCount);

        for (int g = 0; g < graphCount; g++) {
            String name = collectionName(prefix, graphCount, g);
            int objectTarget = share(objectTotal, graphCount, g);
            int edgeTarget = share(edgeTotal, graphCount, g);
            if (edgeTarget > 0 && objectTarget < 2) {
                // Keep edges inside a graph that has enough endpoints; push leftovers conceptually
                // by requiring at least 2 objects when this share would be 1 — clamp edges to 0.
                log.warn(
                        "Perf fill graph '{}': object share {} < 2; skipping {} edges for this graph",
                        name,
                        objectTarget,
                        edgeTarget);
                edgeTarget = 0;
            }

            GraphFillResult part = fillOneGraph(
                    name,
                    g,
                    objectTarget,
                    edgeTarget,
                    versionsPerGraph,
                    batchSize);
            objectsWritten += part.objectsWritten();
            edgesWritten += part.edgesWritten();
            versionsWritten += part.versionsWritten();
            graphProgress.advanced(1);
        }

        long ms = (System.nanoTime() - started) / 1_000_000L;
        log.info(
                "Perf fill complete: graphs={} wrote objects={} edges={} versions={} in {} ms",
                graphCount,
                objectsWritten,
                edgesWritten,
                versionsWritten,
                ms);
    }

    private GraphFillResult fillOneGraph(
            String collectionName,
            int graphIndex,
            int objectTarget,
            int edgeTarget,
            int versionTarget,
            int batchSize
    ) {
        CollectionEntity collection = ensureCollection(collectionName);
        UUID graphId = collection.getGraphId();
        int existingObjects = collections.objectCount(collection);
        int existingEdges = collections.edgeCount(collection);
        int existingVersions = namedGraphs.listGraphVersions(graphId).size();

        if (existingObjects >= objectTarget
                && existingEdges >= edgeTarget
                && existingVersions >= versionTarget) {
            log.info(
                    "Perf fill graph '{}': already at targets objects={}/{} edges={}/{} versions={}/{}",
                    collectionName,
                    existingObjects,
                    objectTarget,
                    existingEdges,
                    edgeTarget,
                    existingVersions,
                    versionTarget);
            return GraphFillResult.EMPTY;
        }
        if (existingObjects > objectTarget) {
            log.warn(
                    "Perf fill graph '{}': has {} objects > target {}; not deleting extras",
                    collectionName,
                    existingObjects,
                    objectTarget);
        }

        log.info(
                "Perf fill graph '{}': objects={}/{} edges={}/{} versions={}/{}",
                collectionName,
                existingObjects,
                objectTarget,
                existingEdges,
                edgeTarget,
                existingVersions,
                versionTarget);

        int indexSpace = Math.max(objectTarget, existingObjects);
        List<UUID> entityIds = new ArrayList<>(indexSpace);
        Map<String, List<Integer>> indexesByType = new HashMap<>();
        for (int i = 0; i < indexSpace; i++) {
            entityIds.add(PerfNoiseFactory.entityId(collectionName, graphIndex, i));
            indexesByType
                    .computeIfAbsent(PerfNoiseFactory.typeForIndex(i), t -> new ArrayList<>())
                    .add(i);
        }

        int objectsWritten = 0;
        int objectsToWrite = Math.max(0, objectTarget - existingObjects);
        if (objectsToWrite > 0) {
            ProgressTracker objectProgress =
                    new ProgressTracker("objects[" + collectionName + "]", objectsToWrite);
            for (int from = existingObjects; from < objectTarget; from += batchSize) {
                int to = Math.min(from + batchSize, objectTarget);
                EntityMutation entities = new EntityMutation();
                for (int i = from; i < to; i++) {
                    entities.getSet().add(PerfNoiseFactory.entity(collectionName, graphIndex, i));
                }
                mutate(graphId, new GraphMutation(entities, new EdgeMutation()));
                objectsWritten += to - from;
                objectProgress.advanced(to - from);
            }
        }

        int edgesWritten = 0;
        if (edgeTarget > existingEdges) {
            List<PerfNoiseFactory.EdgeRule> usable = PerfNoiseFactory.EDGE_RULES.stream()
                    .filter(rule -> canWire(rule, indexesByType))
                    .toList();
            if (usable.isEmpty()) {
                throw new IllegalStateException(
                        "Cannot create edges on '" + collectionName
                                + "': no allowed rule has enough typed endpoints");
            }

            List<Edge> pending = new ArrayList<>(edgeTarget - existingEdges);
            for (int e = existingEdges; e < edgeTarget; e++) {
                PerfNoiseFactory.EdgeRule rule = usable.get(e % usable.size());
                List<Integer> sources = indexesByType.get(rule.sourceType());
                List<Integer> targets = indexesByType.get(rule.targetType());
                int srcIdx = sources.get(e % sources.size());
                int tgtIdx = targets.get((e * 7 + 1) % targets.size());
                if (srcIdx == tgtIdx && rule.sourceType().equals(rule.targetType())) {
                    tgtIdx = targets.get((tgtIdx + 1) % targets.size());
                    if (srcIdx == tgtIdx) {
                        continue;
                    }
                }
                pending.add(new Edge(
                        PerfNoiseFactory.edgeId(collectionName, graphIndex, e),
                        graphId,
                        entityIds.get(srcIdx),
                        entityIds.get(tgtIdx),
                        rule.role(),
                        null,
                        null,
                        null));
            }

            ProgressTracker edgeProgress =
                    new ProgressTracker("edges[" + collectionName + "]", pending.size());
            for (int from = 0; from < pending.size(); from += batchSize) {
                int to = Math.min(from + batchSize, pending.size());
                EdgeMutation edges = new EdgeMutation();
                edges.getSet().addAll(pending.subList(from, to));
                mutate(graphId, new GraphMutation(new EntityMutation(), edges));
                edgesWritten += to - from;
                edgeProgress.advanced(to - from);
            }
        }

        int versionsWritten = 0;
        int versionsNeeded = Math.max(0, versionTarget - existingVersions);
        if (versionsNeeded > 0) {
            ProgressTracker versionProgress =
                    new ProgressTracker("versions[" + collectionName + "]", versionsNeeded);
            for (int v = 0; v < versionsNeeded; v++) {
                int n = existingVersions + v + 1;
                namedGraphs.createDeepGraphVersion(
                        graphId,
                        Map.of(
                                "kind", "perf",
                                "n", Integer.toString(n),
                                "collection", collectionName,
                                "graphIndex", Integer.toString(graphIndex)));
                versionsWritten++;
                versionProgress.advanced(1);
            }
        }

        return new GraphFillResult(objectsWritten, edgesWritten, versionsWritten);
    }

    private void mutate(UUID graphId, GraphMutation mutation) {
        ValidationResult result = namedGraphs.mutate(graphId, mutation);
        if (!result.isValid()) {
            String detail = result.getIssues().isEmpty()
                    ? "invalid"
                    : result.getIssues().get(0).getMessage();
            throw new IllegalStateException("Perf bulk mutate failed: " + detail);
        }
    }

    private CollectionEntity ensureCollection(String name) {
        return collections.findByName(name).orElseGet(() -> {
            List<CollectionTypeSpec> types = PerfNoiseFactory.TYPES.stream()
                    .map(t -> new CollectionTypeSpec(t, null))
                    .toList();
            return collections.create(
                    name,
                    "Perf noise graph (" + name + ")",
                    "perf",
                    "perf@example.com",
                    null,
                    "P3 — best effort",
                    ObjectWriteMode.UUID_OR_IDENTIFIER,
                    types);
        });
    }

    /** Even split: first {@code total % parts} buckets get one extra. */
    static int share(int total, int parts, int index) {
        if (parts <= 0) {
            throw new IllegalArgumentException("parts must be >= 1");
        }
        int base = total / parts;
        int rem = total % parts;
        return index < rem ? base + 1 : base;
    }

    static String collectionName(String prefix, int graphCount, int index) {
        if (graphCount <= 1) {
            return prefix;
        }
        return prefix + "-" + index;
    }

    private static boolean canWire(
            PerfNoiseFactory.EdgeRule rule,
            Map<String, List<Integer>> indexesByType
    ) {
        List<Integer> sources = indexesByType.get(rule.sourceType());
        List<Integer> targets = indexesByType.get(rule.targetType());
        if (sources == null || sources.isEmpty() || targets == null || targets.isEmpty()) {
            return false;
        }
        if (rule.sourceType().equals(rule.targetType())) {
            return sources.size() >= 2;
        }
        return true;
    }

    private record GraphFillResult(int objectsWritten, int edgesWritten, int versionsWritten) {
        static final GraphFillResult EMPTY = new GraphFillResult(0, 0, 0);
    }

    private static final class ProgressTracker {
        private final String label;
        private final int total;
        private int done;
        private int lastLoggedBucket = -1;

        ProgressTracker(String label, int total) {
            this.label = label;
            this.total = Math.max(0, total);
            if (this.total == 0) {
                log.info("Perf fill {}: nothing to write", label);
            }
        }

        void advanced(int delta) {
            if (total == 0 || delta <= 0) {
                return;
            }
            done = Math.min(total, done + delta);
            int pct = (int) ((100L * done) / total);
            int bucket = done == total ? 20 : pct / 5;
            if (bucket > lastLoggedBucket) {
                lastLoggedBucket = bucket;
                log.info("Perf fill {}: {}/{} ({}%)", label, done, total, pct);
            }
        }
    }
}
