package com.janushub.service;

import com.janushub.model.PeticionTarea;
import com.janushub.repository.PeticionTareaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PeticionTareaService {
    
private final PeticionTareaRepository repository;

    public PeticionTarea crear(PeticionTarea p) {
        p.setId(null);
        p.setEstado("PENDIENTE");
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        p.setAdminComment(null);
        return repository.save(p);
    }

    public List<PeticionTarea> getAll() {
        return repository.findAll();
    }

    public PeticionTarea approve(String id, String adminComment) {
        PeticionTarea p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PeticionTarea no encontrada: " + id));
        p.setEstado("APROBADA");
        p.setAdminComment(adminComment);
        p.setUpdatedAt(LocalDateTime.now());
        return repository.save(p);
    }

    public PeticionTarea reject(String id, String adminComment) {
        PeticionTarea p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PeticionTarea no encontrada: " + id));
        p.setEstado("RECHAZADA");
        p.setAdminComment(adminComment);
        p.setUpdatedAt(LocalDateTime.now());
        return repository.save(p);
    }
} 