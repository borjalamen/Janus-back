package com.janushub.controller;

import com.janushub.model.PeticionTarea;
import com.janushub.service.PeticionTareaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints para peticiones de tarea DevOps.
 *
 * Publico:
 *   POST /api/peticiones-tareas          - el usuario envia su peticion de tarea
 *
 * Administracion:
 *   GET  /api/peticiones-tareas          - listar todas
 *   PUT  /api/peticiones-tareas/{id}/approve  - aprobar
 *   PUT  /api/peticiones-tareas/{id}/reject   - rechazar
 */
@RestController
@RequestMapping("/api/peticiones-tareas")
@RequiredArgsConstructor
public class PeticionTareaController {

    private final PeticionTareaService service;

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody PeticionTarea p) {
        try {
            if (p.getRequesterName() == null || p.getRequesterName().isBlank()) {
                return ResponseEntity.badRequest().body("El nombre del peticionario es obligatorio.");
            }
            if (p.getJiraTask() == null || p.getJiraTask().isBlank()) {
                return ResponseEntity.badRequest().body("La tarea JIRA es obligatoria.");
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(p));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al registrar la peticion: " + e.getMessage());
        }
    }

    @GetMapping
    public List<PeticionTarea> getAll() {
        return service.getAll();
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<PeticionTarea> approve(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String comment = body != null ? body.get("adminComment") : null;
            return ResponseEntity.ok(service.approve(id, comment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<PeticionTarea> reject(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String comment = body != null ? body.get("adminComment") : null;
            return ResponseEntity.ok(service.reject(id, comment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}