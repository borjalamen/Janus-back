package com.janushub.controller;

import com.janushub.model.ObsolescenciaEntry;
import com.janushub.repository.ObsolescenciaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/obsolescencia")
public class ObsolescenciaController {

    private final ObsolescenciaRepository repository;

    public ObsolescenciaController(ObsolescenciaRepository repository) {
        this.repository = repository;
    }

    /** GET /api/obsolescencia — todas las entradas, más recientes primero */
    @GetMapping
    public List<ObsolescenciaEntry> getAll() {
        return repository.findAllByOrderByMarkedAtDesc();
    }

    /** GET /api/obsolescencia/project/{projectId} — entradas de un proyecto */
    @GetMapping("/project/{projectId}")
    public List<ObsolescenciaEntry> getByProject(@PathVariable String projectId) {
        return repository.findByProjectId(projectId);
    }

    /** DELETE /api/obsolescencia/{id} — solo ADMIN (seguridad en frontend) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
