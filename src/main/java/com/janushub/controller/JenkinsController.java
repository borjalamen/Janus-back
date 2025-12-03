package com.janushub.controller;

import com.janushub.model.Jenkins;
import com.janushub.repository.JenkinsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jenkins") // Ruta base para esta entidad
public class JenkinsController {

   private final JenkinsRepository repository;

    public JenkinsController(JenkinsRepository repository) {
        this.repository = repository;
    }

    // --- GET ALL ---
    @GetMapping("/all")
    public List<Jenkins> getAllJenkins() {
        return repository.findAll();
    }

    // --- POST (Crear) ---
    @PostMapping("/create")
    public Jenkins createJenkins(@RequestBody Jenkins jenkins) {
        return repository.save(jenkins);
    }

    // --- PUT (Actualizar) ---
    @PutMapping("/update/{id}")
    public ResponseEntity<Jenkins> updateJenkins(@PathVariable String id, @RequestBody Jenkins jenkinsDetails) {
        // Buscamos si existe
        return repository.findById(id)
                .map(jenkins -> {
                    // Actualizamos los campos
                    jenkins.setNombre(jenkinsDetails.getNombre());
                    jenkins.setUrl(jenkinsDetails.getUrl());
                    jenkins.setIdProyecto(jenkinsDetails.getIdProyecto());
                    
                    // Guardamos los cambios
                    Jenkins updatedJenkins = repository.save(jenkins);
                    return ResponseEntity.ok(updatedJenkins);
                })
                .orElse(ResponseEntity.notFound().build()); // Si no existe, devuelve 404
    }

    // --- DELETE (Borrar) ---
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteJenkins(@PathVariable String id) {
        return repository.findById(id)
                .map(jenkins -> {
                    repository.delete(jenkins);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}