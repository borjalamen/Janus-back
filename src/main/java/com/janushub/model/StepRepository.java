package com.janushub.model;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.janushub.model.StepDocumentID;

public interface StepRepository extends MongoRepository<StepDocumentID, String> {

    boolean existsByTitulo(String titulo);

    
}