package com.janushub.repository;

import com.janushub.model.ScrumTask;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScrumTaskRepository extends MongoRepository<ScrumTask, String> {
    List<ScrumTask> findByVisibleTrue();
}
