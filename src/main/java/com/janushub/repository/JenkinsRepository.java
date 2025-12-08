package com.janushub.repository;

import com.janushub.model.Jenkins;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JenkinsRepository extends MongoRepository<Jenkins, String> {
    List<Jenkins> findByNombreContainingIgnoreCase(String nombre);
}