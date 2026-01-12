package com.janushub.repository;

import com.janushub.model.Planificacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanificacionRepository extends MongoRepository<Planificacion, String> {
}
