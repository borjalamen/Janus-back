package com.janushub.repository;

import com.janushub.model.ScrumSprint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScrumSprintRepository extends MongoRepository<ScrumSprint, String> {
    Optional<ScrumSprint> findByActiveTrue();
    List<ScrumSprint> findByActiveFalse();
}
