package com.janushub.model;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.janushub.model.StepDocumentID;

public interface StepRepository extends MongoRepository<StepDocumentID, String> {
    Optional<StepDocumentID> findByStepId(String stepId);

    boolean existsByTitulo(String titulo);

    
}