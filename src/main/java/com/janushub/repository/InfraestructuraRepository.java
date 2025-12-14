package com.janushub.repository;

import com.janushub.model.Infraestructura;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InfraestructuraRepository extends MongoRepository<Infraestructura, String> {
// 1. Sustituto de findAll(): Busca todos los que NO están borrados
    List<Infraestructura> findByDeletedFalse();

    // 2. Buscar por Host (y que no esté borrado)
    List<Infraestructura> findByHostContainingIgnoreCaseAndDeletedFalse(String host);
    
    // 3. Buscar por IP (y que no esté borrado)
    Optional<Infraestructura> findByIpAndDeletedFalse(String ip);

    // 4. Buscar por ID (y que no esté borrado) -> Útil para detalles y updates
    Optional<Infraestructura> findByIdAndDeletedFalse(String id);
}