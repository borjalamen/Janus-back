package com.janushub.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.janushub.model.Procedure;

import java.util.List;

public interface ProceduresRepository extends MongoRepository<Procedure, String> {
    // Busca solo los que NO están eliminados lógicamente
    List<Procedure> findByIsDeletedFalse();
    
    // Buscar por titulo (contiene texto, case-insensitive) solo activos
    List<Procedure> findByTituloContainingIgnoreCaseAndIsDeletedFalse(String titulo);

    Procedure findTopByIdStartingWithOrderByIdDesc(String prefix);
    
    
}