package com.janushub.controller;

import com.janushub.model.ExternalService;
import com.janushub.service.ExternalServiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/external-services")
public class ExternalServiceController {

    private final ExternalServiceService service;

    public ExternalServiceController(ExternalServiceService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ExternalService>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExternalService> getById(@PathVariable String id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ExternalService> create(@RequestBody ExternalService externalService) {
        ExternalService created = service.create(externalService);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExternalService> update(@PathVariable String id,
                                                   @RequestBody ExternalService details) {
        return service.update(id, details)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
