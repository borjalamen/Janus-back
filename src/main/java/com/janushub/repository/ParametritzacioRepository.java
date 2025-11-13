package com.janushub.repository;

import com.janushub.model.Parametritzacio;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametritzacioRepository extends MongoRepository<Parametritzacio, String> {
}
