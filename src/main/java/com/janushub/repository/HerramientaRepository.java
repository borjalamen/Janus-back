package com.janushub.repository;

import com.janushub.model.Herramienta;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HerramientaRepository extends MongoRepository<Herramienta, String> {
    List<Herramienta> findByVisibleTrue();
    java.util.Optional<Herramienta> findByNameIgnoreCase(String name);
}
