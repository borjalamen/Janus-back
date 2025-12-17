package com.janushub.repository;

import com.janushub.model.Formacion; // Asumiendo que has corregido el nombre de la entidad a 'Formation'
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FormacionRepository extends MongoRepository<Formacion, String> {
    
    // Buscar por nombre (todas las formaciones, incluyendo inactivas)
    List<Formacion> findByNameContainingIgnoreCase(String name);
    
    // Buscar solo formaciones activas (no eliminadas) - Query explícita para usar el campo de MongoDB
    @Query("{ 'deleted': false }")
    List<Formacion> findByDeletedFalse();
    
    // Buscar por nombre solo formaciones activas - Query explícita
    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'deleted': false }")
    List<Formacion> findByNameContainingIgnoreCaseAndDeletedFalse(String name);
    
    // (Opcional, si lo quieres mantener)
    List<Formacion> findByLocation(String location);
}