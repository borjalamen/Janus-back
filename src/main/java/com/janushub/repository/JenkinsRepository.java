package com.janushub.repository;

import com.janushub.model.Jenkins;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JenkinsRepository extends MongoRepository<Jenkins, String> {
    // No necesitamos añadir nada extra, MongoRepository ya trae findAll() y save()
}