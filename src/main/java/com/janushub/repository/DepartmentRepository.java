package com.janushub.repository;

import com.janushub.model.Department;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DepartmentRepository extends MongoRepository<Department, String> {
    Optional<Department> findByNameIgnoreCase(String name);
}