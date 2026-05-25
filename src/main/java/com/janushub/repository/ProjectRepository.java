package com.janushub.repository;

import com.janushub.model.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends MongoRepository<Project, String> {

    Optional<Project> findByCodigoProyecto(String codigoProyecto);

    Optional<Project> findByCodigoProyectoAndDeletedFalse(String codigoProyecto);

    List<Project> findByNombreContainingIgnoreCase(String nombre);

    List<Project> findByDepartamentoContainingIgnoreCase(String departamento);

    Project findTopByIdStartingWithOrderByIdDesc(String prefix);

    List<Project> findByDeletedFalse();
}