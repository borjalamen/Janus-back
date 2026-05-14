package com.janushub.repository;

import com.janushub.model.ExternalService;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExternalServiceRepository extends MongoRepository<ExternalService, String> {

    List<ExternalService> findAllByOrderByNameAsc();

    boolean existsByCode(String code);
}
