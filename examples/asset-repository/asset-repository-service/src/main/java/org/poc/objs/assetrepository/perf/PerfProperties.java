package org.poc.objs.assetrepository.perf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ar.perf")
public class PerfProperties {

    /**
     * Number of collections / named graphs to create. Totals for objects and edges are
     * distributed across them; {@link #versions} is applied <em>per graph</em>.
     */
    private int graphs = 1;

    /** Total objects across all graphs (split as evenly as possible). */
    private int objects = 1000;

    /** Total edges across all graphs (split; each edge stays inside one graph). */
    private int edges = 2000;

    /** Entities (or edges) per NamedGraphStore.mutate call. */
    private int batchSize = 500;

    /**
     * Collection name prefix. With {@code graphs=1} the collection is exactly this name;
     * with {@code graphs>1} names are {@code {collection}-0} … {@code {collection}-(n-1)}.
     */
    private String collection = "perf-noise";

    /**
     * Deep graph versions (snapshots) <em>per graph</em>. {@code 0} skips version capture.
     * Each version copies that graph's current members + edges.
     */
    private int versions = 0;

    public int getGraphs() {
        return graphs;
    }

    public void setGraphs(int graphs) {
        this.graphs = graphs;
    }

    public int getObjects() {
        return objects;
    }

    public void setObjects(int objects) {
        this.objects = objects;
    }

    public int getEdges() {
        return edges;
    }

    public void setEdges(int edges) {
        this.edges = edges;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public int getVersions() {
        return versions;
    }

    public void setVersions(int versions) {
        this.versions = versions;
    }
}
