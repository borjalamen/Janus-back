package com.janushub.repository;

import com.janushub.model.PeticionTarea;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
public interface PeticionTareaRepository extends MongoRepository<PeticionTarea, String> {
     List<PeticionTarea> findByEstado(String estado);
}