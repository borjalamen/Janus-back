package com.janushub.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.janushub.model.Formacion;
import com.janushub.repository.FormacionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class FormacionService {

     private final FormacionRepository repository;

    public Formacion create(Formacion f) {
        f.setId(null);
        f.setDeleted(false);
        f.setVisible(true);
        return repository.save(f);
    }

    public List<Formacion> getAll() {
        return repository.findByDeletedFalseAndVisibleTrue();
    }

    public List<Formacion> searchByName(String name) {
        return repository.findByNameContainingIgnoreCaseAndDeletedFalse(name);
    }

    public Formacion deleteLogical(String id) {
        Formacion f = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formacion no encontrada"));

        f.setDeleted(true);
        f.setDeletedAt(LocalDateTime.now());
        return repository.save(f);
    }
    
}
