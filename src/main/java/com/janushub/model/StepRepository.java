package com.janushub.model;

import org.springframework.data.mongodb.repository.MongoRepository;




public interface StepRepository extends MongoRepository<StepDocumentID, String> {
      StepDocumentID findByStepId(String stepId);

    boolean existsByStepId(String stepId);

    
}