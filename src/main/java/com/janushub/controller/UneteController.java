package com.janushub.controller;

import com.janushub.model.Unete;
import com.janushub.service.UneteService;
import dto.UneteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestionar las peticiones de "únete".
 *
 * Endpoints públicos (formulario "únete"):
 *   POST /api/contact/unete  → registra una nueva solicitud
 *
 * Endpoints de administración:
 *   GET    /api/join-requests           → listar todas las peticiones
 *   GET    /api/join-requests/{id}      → obtener una petición por ID
 *   GET    /api/join-requests/estado/{estado} → filtrar por estado
 *   PUT    /api/join-requests/{id}/approve   → aprobar petición
 *   PUT    /api/join-requests/{id}/reject    → rechazar petición
 *   DELETE /api/join-requests/{id}      → eliminar petición
 */
@RestController
@RequiredArgsConstructor
public class UneteController {
    private final UneteService uneteService;

    // =========================================================
    // ENDPOINT PÚBLICO — recibe la solicitud desde "únete"
    // =========================================================

    /**
     * El frontend envía POST /api/contact/unete con el formulario.
     * Se registra la petición con estado PENDIENTE.
     */
    @PostMapping("/api/contact/unete")
    public ResponseEntity<?> submitJoinRequest(@RequestBody UneteDTO dto) {
        try {
            // Validaciones básicas
            if (dto.getFullName() == null || dto.getFullName().isBlank()) {
                return ResponseEntity.badRequest().body("El nombre completo es obligatorio.");
            }
            if (dto.getEmail() == null || dto.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body("El email es obligatorio.");
            }

            Unete saved = uneteService.createRequest(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al registrar la solicitud: " + e.getMessage());
        }
    }

    // =========================================================
    // ENDPOINTS DE ADMINISTRACIÓN
    // =========================================================

    /** Lista todas las peticiones de "únete". */
    @GetMapping("/api/join-requests")
    public List<Unete> getAllRequests() {
        return uneteService.getAllRequests();
    }

    /** Obtiene una petición por su ID. */
    @GetMapping("/api/join-requests/{id}")
    public ResponseEntity<Unete> getRequestById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(uneteService.getRequestById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Filtra peticiones por estado (PENDIENTE, APROBADA, RECHAZADA). */
    @GetMapping("/api/join-requests/estado/{estado}")
    public List<Unete> getByEstado(@PathVariable String estado) {
        return uneteService.getRequestsByEstado(estado.toUpperCase());
    }

    /**
     * Aprueba una petición.
     * Body opcional: { "adminComment": "Aprobado por el responsable" }
     */
    @PutMapping("/api/join-requests/{id}/approve")
    public ResponseEntity<Unete> approveRequest(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String comment = (body != null) ? body.get("adminComment") : null;
            Unete updated = uneteService.approveRequest(id, comment);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Rechaza una petición.
     * Body opcional: { "adminComment": "No cumple los requisitos" }
     */
    @PutMapping("/api/join-requests/{id}/reject")
    public ResponseEntity<Unete> rejectRequest(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String comment = (body != null) ? body.get("adminComment") : null;
            Unete updated = uneteService.rejectRequest(id, comment);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Elimina una petición existente. */
    @DeleteMapping("/api/join-requests/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable String id) {
        try {
            uneteService.deleteRequest(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
