package com.janushub.model;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;




public interface StepRepository extends MongoRepository<StepDocumentID, String> {
      StepDocumentID findByStepId(String stepId);

    List<StepDocumentID> findByResponsableContainingIgnoreCase(String responsable);

    List<StepDocumentID> findByTagsContaining(String tag);
    
}