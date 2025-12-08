package com.janushub.controller;

import java.util.Map;
import com.janushub.model.Jenkins;
import com.janushub.repository.JenkinsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mongodb.MongoWriteException; 
import org.springframework.http.HttpStatus; 
import java.util.Optional;

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

    // --- PUT (Cambiar Estado de Activo/Inactivo) ---
    @PutMapping("/status/{id}")
    public ResponseEntity<Jenkins> updateJenkinsStatus(@PathVariable String id, @RequestBody Map<String, String> statusMap) {
        // 1. Buscamos el Jenkins por ID
        Optional<Jenkins> jenkinsOptional = repository.findById(id);

        // 2. Comprobamos si el objeto existe
        if (jenkinsOptional.isPresent()) {
            Jenkins jenkins = jenkinsOptional.get();
            
            // 3. Aplicamos la lógica de actualización
            String newStatus = statusMap.get("status");
            
            if (newStatus != null && !newStatus.isEmpty()) {
                jenkins.setStatus(newStatus.toUpperCase()); 
            }
            
            // 4. Guardamos y devolvemos 200 OK
            Jenkins updatedJenkins = repository.save(jenkins);
            return ResponseEntity.ok(updatedJenkins);
            
        } else {
            // 5. Si no se encuentra, devolvemos 404 Not Found
            return ResponseEntity.notFound().build();
        }
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

    // --- BUSCAR POR NOMBRE ---
    // URL: http://localhost:8080/api/jenkins/search/NombreDelPipeline
    @GetMapping("/search/{nombre}")
    public List<Jenkins> getJenkinsByName(@PathVariable String nombre) {
        return repository.findByNombreContainingIgnoreCase(nombre);
    }
}