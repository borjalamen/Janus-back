package com.janushub.controller;

import com.janushub.model.Planificacion;
import com.janushub.service.PlanificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/planificacion")
@CrossOrigin(origins = "http://localhost:4200")
public class PlanificacionController {

    private final PlanificacionService planificacionService;

    public PlanificacionController(PlanificacionService planificacionService) {
        this.planificacionService = planificacionService;
    }

    @GetMapping
    public ResponseEntity<List<Planificacion>> getAll() {
        return ResponseEntity.ok(planificacionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Planificacion> getById(@PathVariable String id) {
        return planificacionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Planificacion> create(@RequestBody Planificacion planificacion) {
        Planificacion created = planificacionService.save(planificacion);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Planificacion> update(@PathVariable String id,
                                                @RequestBody Planificacion planificacion) {
        return planificacionService.findById(id)
                .map(existing -> {
                    planificacion.setId(id);
                    Planificacion updated = planificacionService.save(planificacion);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (planificacionService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        planificacionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
