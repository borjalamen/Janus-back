package com.janushub.repository;

import com.janushub.model.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends MongoRepository<Project, String> {

    Optional<Project> findByCode(String code);

    List<Project> findByNameContainingIgnoreCase(String name);

    List<Project> findByDepartamentOrganismeContainingIgnoreCase(String departamentOrganisme);
}
