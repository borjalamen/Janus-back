package com.janushub.model;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcedureRepository extends MongoRepository<ProcedureDocument, String> {

    ProcedureDocument findByProcedureId(String procedureId);

    List<ProcedureDocument> findByTituloContainingIgnoreCase(String titulo);
    
}
