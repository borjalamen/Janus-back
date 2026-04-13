package com.janushub.controller;

import com.janushub.model.Estimacion;
import com.janushub.repository.EstimacionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/estimaciones")
public class EstimacionController {

    private final EstimacionRepository repository;

    public EstimacionController(EstimacionRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/all")
    public List<Estimacion> getAll() {
        return repository.findByVisibleTrueOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Estimacion> getById(@PathVariable String id) {
        return repository.findById(id)
                .filter(e -> Boolean.TRUE.equals(e.getVisible()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    public Estimacion create(@RequestBody Estimacion estimacion) {
        // Always let Mongo generate the id to avoid client-side hardcoded identifiers.
        estimacion.setId(null);
        if (estimacion.getVisible() == null) {
            estimacion.setVisible(true);
        }
        if (estimacion.getCreatedAt() == null || estimacion.getCreatedAt().isBlank()) {
            estimacion.setCreatedAt(Instant.now().toString());
        }
        return repository.save(estimacion);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Estimacion> update(@PathVariable String id, @RequestBody Estimacion details) {
        return repository.findById(id)
                .map(existing -> {
                    details.setId(id);
                    if (details.getCreatedAt() == null || details.getCreatedAt().isBlank()) {
                        details.setCreatedAt(existing.getCreatedAt());
                    }
                    if (details.getVisible() == null) {
                        details.setVisible(true);
                    }
                    return ResponseEntity.ok(repository.save(details));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setVisible(false);
                    repository.save(existing);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
