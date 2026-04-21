package com.hyperbrain.sopfc.cognitive.infrastructure.adapter.out.persistence;

import com.hyperbrain.sopfc.cognitive.infrastructure.adapter.out.persistence.entity.CognitiveLoadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SpringDataCognitiveRepository extends JpaRepository<CognitiveLoadEntity, UUID> {
}
