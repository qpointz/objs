package org.poc.objs.assetrepository.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ar_collection")
public class CollectionEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String owner;

    @Column(name = "owner_email")
    private String ownerEmail;

    @Column(name = "support_email")
    private String supportEmail;

    @Column(columnDefinition = "TEXT")
    private String sla;

    @Enumerated(EnumType.STRING)
    @Column(name = "object_write_mode", nullable = false, length = 32)
    private ObjectWriteMode objectWriteMode = ObjectWriteMode.UUID_OR_IDENTIFIER;

    @Column(name = "graph_id", nullable = false, unique = true)
    private UUID graphId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "collection",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<CollectionTypeEntity> types = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getSla() {
        return sla;
    }

    public void setSla(String sla) {
        this.sla = sla;
    }

    public ObjectWriteMode getObjectWriteMode() {
        return objectWriteMode;
    }

    public void setObjectWriteMode(ObjectWriteMode objectWriteMode) {
        this.objectWriteMode = objectWriteMode;
    }

    public UUID getGraphId() {
        return graphId;
    }

    public void setGraphId(UUID graphId) {
        this.graphId = graphId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<CollectionTypeEntity> getTypes() {
        return types;
    }

    public List<String> acceptedTypes() {
        return types.stream().map(CollectionTypeEntity::getObjectType).toList();
    }

    public void replaceTypes(List<CollectionTypeEntity> next) {
        Map<String, CollectionTypeEntity> existing = new LinkedHashMap<>();
        for (CollectionTypeEntity row : types) {
            existing.put(row.getObjectType(), row);
        }
        List<CollectionTypeEntity> kept = new ArrayList<>();
        if (next != null) {
            for (CollectionTypeEntity incoming : next) {
                String objectType = incoming.getObjectType();
                CollectionTypeEntity current = existing.remove(objectType);
                if (current != null) {
                    current.setMetadata(incoming.getMetadata());
                    kept.add(current);
                } else {
                    incoming.setCollection(this);
                    kept.add(incoming);
                }
            }
        }
        types.clear();
        types.addAll(kept);
    }
}
