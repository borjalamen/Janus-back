package com.janushub.controller;

import com.janushub.model.PeticionTarea;
import com.janushub.service.PeticionTareaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/peticiones-tareas")
@RequiredArgsConstructor
public class PeticionTareaController {

    private final PeticionTareaService service;

    // ─────────────────────────────────────────────
    //  GET /peticiones-tareas
    //  Retorna totes les peticions
    // ─────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<PeticionTarea>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    // ─────────────────────────────────────────────
    //  POST /peticiones-tareas
    //  Crea una nova petició
    // ─────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<PeticionTarea> crear(@RequestBody PeticionTarea peticion) {
        PeticionTarea creada = service.crear(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // ─────────────────────────────────────────────
    //  PUT /peticiones-tareas/{id}/approve
    //  Aprova una petició
    // ─────────────────────────────────────────────
    @PutMapping("/{id}/approve")
    public ResponseEntity<PeticionTarea> approve(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {

        String adminComment = (body != null) ? body.getOrDefault("adminComment", "") : "";
        PeticionTarea updated = service.approve(id, adminComment);
        return ResponseEntity.ok(updated);
    }

    // ─────────────────────────────────────────────
    //  PUT /peticiones-tareas/{id}/reject
    //  Rebutja una petició
    // ─────────────────────────────────────────────
    @PutMapping("/{id}/reject")
    public ResponseEntity<PeticionTarea> reject(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {

        String adminComment = (body != null) ? body.getOrDefault("adminComment", "") : "";
        PeticionTarea updated = service.reject(id, adminComment);
        return ResponseEntity.ok(updated);
    }

    // ─────────────────────────────────────────────
    //  GET /peticiones-tareas/{id}/pdf
    //  Genera i retorna el PDF de la petició
    // ─────────────────────────────────────────────
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPDF(@PathVariable String id) {
        try {
            byte[] pdf = service.generarPDF(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "peticion_tarea_" + id + ".pdf");
            headers.setContentLength(pdf.length);

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}