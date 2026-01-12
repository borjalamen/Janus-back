package com.janushub.service;

import com.janushub.model.Planificacion;
import com.janushub.repository.PlanificacionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlanificacionService {

    private final PlanificacionRepository planificacionRepository;

    @Value("${spring.data.mongodb.database}")
    private String dbName;

    public PlanificacionService(PlanificacionRepository planificacionRepository) {
        this.planificacionRepository = planificacionRepository;
    }

    @PostConstruct
    public void init() {
        System.out.println(">>> Mongo DB name = " + dbName);
        System.out.println(">>> Planificacion count = " + planificacionRepository.count());
    }

    public List<Planificacion> findAll() {
        return planificacionRepository.findAll();
    }

    public Optional<Planificacion> findById(String id) {
        return planificacionRepository.findById(id);
    }

    public Planificacion save(Planificacion planificacion) {
        return planificacionRepository.save(planificacion);
    }

    public void deleteById(String id) {
        planificacionRepository.deleteById(id);
    }
}
