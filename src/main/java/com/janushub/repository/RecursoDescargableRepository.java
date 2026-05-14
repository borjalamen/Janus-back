package com.janushub.repository;

import com.janushub.model.RecursoDescargable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RecursoDescargableRepository extends MongoRepository<RecursoDescargable, String> {
    List<RecursoDescargable> findAllByOrderByCreatedAtDesc();
}
