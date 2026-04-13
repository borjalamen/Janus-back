package com.janushub.repository;

import com.janushub.model.Estimacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstimacionRepository extends MongoRepository<Estimacion, String> {
    List<Estimacion> findByVisibleTrueOrderByCreatedAtDesc();
}
