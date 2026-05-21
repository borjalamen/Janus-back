package com.janushub.repository;

import com.janushub.model.ObsolescenciaEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObsolescenciaRepository extends MongoRepository<ObsolescenciaEntry, String> {

    List<ObsolescenciaEntry> findByProjectId(String projectId);

    List<ObsolescenciaEntry> findAllByOrderByMarkedAtDesc();
}
