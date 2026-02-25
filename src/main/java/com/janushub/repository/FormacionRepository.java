package com.janushub.repository;

import com.janushub.model.Formacion; // Asumiendo que has corregido el nombre de la entidad a 'Formation'
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FormacionRepository extends MongoRepository<Formacion, String> {
    
    // Buscar por nombre (todas las formaciones, incluyendo inactivas)
    List<Formacion> findByDeletedFalseAndVisibleTrue();
    
    // Buscar solo formaciones activas (no eliminadas) - Query explícita para usar el campo de MongoDB
    
   Optional<Formacion> findByIdAndDeletedFalse(String id);
    // Buscar por nombre solo formaciones activas - Query explícita
    
    List<Formacion> findByNameContainingIgnoreCaseAndDeletedFalse(String name);

    Formacion findTopByIdStartingWithOrderByIdDesc(String prefix);
    
    // (Opcional, si lo quieres mantener)
    List<Formacion> findByLocation(String location);
}