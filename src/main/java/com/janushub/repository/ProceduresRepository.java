package com.janushub.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.Optional;

import com.janushub.model.Procedure;

import java.util.List;

public interface ProceduresRepository extends MongoRepository<Procedure, String> {
    // Busca solo los que NO están eliminados lógicamente
    List<Procedure> findByIsDeletedFalse();
    
    // Buscar por titulo (contiene texto, case-insensitive) solo activos
    List<Procedure> findByTituloContainingIgnoreCaseAndIsDeletedFalse(String titulo);

    @Query("{ 'titulo': { $regex: ?0, $options: 'i' }, 'isDeleted': false }")
    List<Procedure> searchByTitulo(String titulo);

    @Query("{ '_id': ?0, 'isDeleted': false }")
    Optional<Procedure> findActiveById(String id);

    Procedure findTopByIdStartingWithOrderByIdDesc(String prefix);
    
    
}