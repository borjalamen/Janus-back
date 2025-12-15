package com.janushub.repository;

import com.janushub.model.Formacion; // Asumiendo que has corregido el nombre de la entidad a 'Formation'
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FormacionRepository extends MongoRepository<Formacion, String> {
    
    // El método debe usar 'Name' para coincidir con el controlador y la entidad en inglés.
    List<Formacion> findByNameContainingIgnoreCase(String name);
    
    // (Opcional, si lo quieres mantener)
    List<Formacion> findByLocation(String location);
}