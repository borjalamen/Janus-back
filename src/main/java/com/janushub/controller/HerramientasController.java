package com.janushub.controller;

import com.janushub.model.Herramienta;
import com.janushub.repository.HerramientaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/herramientas")
public class HerramientasController {

    private final HerramientaRepository repository;

    public HerramientasController(HerramientaRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/all")
    public List<Herramienta> getAll() {
        return repository.findByVisibleTrue();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Herramienta> getById(@PathVariable String id) {
        Optional<Herramienta> herramienta = repository.findById(id)
                .filter(h -> Boolean.TRUE.equals(h.getVisible()));
        return herramienta.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    public Herramienta create(@RequestBody Herramienta herramienta) {
        if (herramienta.getId() != null && herramienta.getId().isBlank()) {
            herramienta.setId(null);
        }
        if (herramienta.getVisible() == null) {
            herramienta.setVisible(true);
        }
        return repository.save(herramienta);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Herramienta> update(@PathVariable String id, @RequestBody Herramienta details) {
        return repository.findById(id)
                .map(existing -> {
                    details.setId(id);
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
