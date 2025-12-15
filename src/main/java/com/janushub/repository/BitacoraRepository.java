package com.janushub.repository;

import com.janushub.model.Bitacora;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BitacoraRepository extends MongoRepository<Bitacora, String> {

// 1. GET ALL (Solo las activas/visibles)
    List<Bitacora> findByVisibleTrue();
    
    // 2. Buscar por ID (Solo si está visible)
    Optional<Bitacora> findByIdAndVisibleTrue(String id);
    
    // 3. Buscar por ID de Proyecto (Solo las visibles)
    List<Bitacora> findByIdProyectoAndVisibleTrue(String idProyecto);
    
    // 4. Buscar por ID de Proyecto (Solo las NO visibles / Borradas Lógicamente)
    List<Bitacora> findByIdProyectoAndVisibleFalse(String idProyecto);
}