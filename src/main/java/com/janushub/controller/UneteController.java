package com.janushub.controller;

import com.janushub.model.Unete;
import com.janushub.service.UneteService;
import dto.ApprovalResponseDTO;
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
 *   POST /api/contact/unete              → registra una nueva solicitud
 *
 * Endpoints de administración:
 *   GET    /api/join-requests                      → listar todas las peticiones
 *   GET    /api/join-requests/{id}                 → obtener una petición por ID
 *   GET    /api/join-requests/estado/{estado}      → filtrar por estado
 *   GET    /api/join-requests/by-email/{email}     → obtener por email
 *   PUT    /api/join-requests/{id}/approve         → aprobar petición
 *   PUT    /api/join-requests/{id}/reject          → rechazar petición
 *   DELETE /api/join-requests/{id}                 → eliminar petición
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
     *
     * Validaciones:
     * - Email es obligatorio
     * - Email único (no existe en usuarios, ni solicitud pendiente)
     * - Nombre completo es obligatorio
     */
    @PostMapping("/api/contact/unete")
    public ResponseEntity<?> submitJoinRequest(@RequestBody UneteDTO dto) {
        try {
            if (dto.getFullName() == null || dto.getFullName().isBlank()) {
                return ResponseEntity.badRequest().body("El nombre completo es obligatorio.");
            }
            if (dto.getEmail() == null || dto.getEmail().isBlank()) {
                return ResponseEntity.badRequest().body("El email es obligatorio.");
            }
            Unete saved = uneteService.createRequest(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error al registrar solicitud de unete: " + e.getMessage());
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
     * Busca una petición por email (para que el front de "únete" consulte el estado).
     */
    @GetMapping("/api/join-requests/by-email/{email}")
    public ResponseEntity<Unete> getByEmail(@PathVariable String email) {
        Unete u = uneteService.getByEmail(email);
        if (u == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(u);
    }

    /**
     * Aprueba una petición y CREA EL USUARIO en la base de datos.
     * La contraseña se autogenera con el formato: [nombre]1234
     *
     * Body opcional: { "adminComment": "Aprobado por el responsable" }
     *
     * @return ApprovalResponseDTO con la petición actualizada y las credenciales del usuario creado
     */
    @PutMapping("/api/join-requests/{id}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String comment = (body != null) ? body.get("adminComment") : null;
            ApprovalResponseDTO response = uneteService.approveRequest(id, comment);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Petición no encontrada o validación fallida
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            // Error creando usuario (ej: email duplicado)
            System.err.println("❌ Error al aprobar y crear usuario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No se pudo crear el usuario: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error interno: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno al procesar la solicitud: " + e.getMessage());
        }
    }

    /**
     * Rechaza una petición.
     * Body opcional: { "adminComment": "No cumple los requisitos" }
     */
    @PutMapping("/api/join-requests/{id}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String comment = (body != null) ? body.get("adminComment") : null;
            Unete updated = uneteService.rejectRequest(id, comment);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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

    /**
     * El admin acepta manualmente una solicitud INICIADA, pasándola a PENDIENTE.
     * Esto equivale a hacer la verificación de correo en nombre del usuario.
     * PUT /api/join-requests/{id}/accept-initiated
     */
    @PutMapping("/api/join-requests/{id}/accept-initiated")
    public ResponseEntity<?> acceptInitiated(@PathVariable String id) {
        try {
            Unete updated = uneteService.acceptInitiatedByAdmin(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
