package com.janushub.controller;

import com.janushub.model.ScrumTask;
import com.janushub.repository.ScrumTaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/scrum")
public class ScrumTaskController {

    private final ScrumTaskRepository repository;

    public ScrumTaskController(ScrumTaskRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/all")
    public List<ScrumTask> getAll() {
        return repository.findByVisibleTrue();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScrumTask> getById(@PathVariable String id) {
        return repository.findById(id)
                .filter(t -> Boolean.TRUE.equals(t.getVisible()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    public ScrumTask create(@RequestBody ScrumTask task) {
        if (task.getId() != null && task.getId().isBlank()) {
            task.setId(null);
        }
        if (task.getVisible() == null) {
            task.setVisible(true);
        }
        if (task.getCreatedAt() == null || task.getCreatedAt().isBlank()) {
            task.setCreatedAt(Instant.now().toString());
        }
        task.setUpdatedAt(Instant.now().toString());
        return repository.save(task);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ScrumTask> update(@PathVariable String id, @RequestBody ScrumTask details) {
        return repository.findById(id)
                .map(existing -> {
                    details.setId(id);
                    if (details.getCreatedAt() == null || details.getCreatedAt().isBlank()) {
                        details.setCreatedAt(existing.getCreatedAt());
                    }
                    if (details.getVisible() == null) {
                        details.setVisible(true);
                    }
                    details.setUpdatedAt(Instant.now().toString());
                    return ResponseEntity.ok(repository.save(details));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return repository.findById(id)
                .map(existing -> {
                    existing.setVisible(false);
                    existing.setUpdatedAt(Instant.now().toString());
                    repository.save(existing);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
