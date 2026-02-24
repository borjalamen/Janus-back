package com.janushub.repository;

import com.janushub.model.Bitacora;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BitacoraRepository extends MongoRepository<Bitacora, String> {

// 1. GET ALL (Solo las activas/visibles)
    @Query("{ 'visible': true }")
    List<Bitacora> findAllVisible();
    
    // 2. Buscar por ID (Solo si está visible)
     @Query("{ '_id': ?0, 'visible': true }")
    Optional<Bitacora> findVisibleById(String id);

    @Query("{ 'contexto': { $regex: ?0, $options: 'i' }, 'visible': true }")
List<Bitacora> searchByTexto(String contexto);

List<Bitacora> findByTituloContainingIgnoreCaseAndVisibleTrue(String titulo);


    // 3. Buscar por ID de Proyecto (Solo las visibles)
    @Query("{ 'idProyecto': ?0, 'visible': true }")
    List<Bitacora> findByProyectoVisible(String idProyecto);
    
    // 4. Buscar por ID de Proyecto (Solo las NO visibles / Borradas Lógicamente)
   @Query("{ 'idProyecto': ?0, 'visible': false }")
    List<Bitacora> findByProyectoHidden(String idProyecto);

    
    Optional<Bitacora> findTopByOrderByIdDesc();



}