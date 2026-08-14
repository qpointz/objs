package org.poc.objs.assetrepository.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionRepository extends JpaRepository<CollectionEntity, UUID> {

    Optional<CollectionEntity> findByGraphId(UUID graphId);

    @Query("""
            SELECT c FROM CollectionEntity c
            WHERE (:nameContains IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:nameContains AS string), '%')))
              AND (:owner IS NULL OR LOWER(c.owner) = LOWER(:owner))
            """)
    List<CollectionEntity> search(
            @Param("nameContains") String nameContains,
            @Param("owner") String owner);
}
