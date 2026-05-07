package com.janushub.controller;

import com.janushub.model.PeticionTarea;
import com.janushub.service.PeticionTareaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    //  POST /peticiones-tareas (amb adjunts)
    //  Crea una nova petició + guarda noms de fitxers
    // ─────────────────────────────────────────────
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PeticionTarea> crear(
            @RequestPart("requesterName") String requesterName,
            @RequestPart("requesterEmail") String requesterEmail,
            @RequestPart("projectName") String projectName,
            @RequestPart("projectCode") String projectCode,
            @RequestPart("jiraTask") String jiraTask,
            @RequestPart(value = "comments", required = false) String comments,
            @RequestPart("devopsAssignee") String devopsAssignee,
            @RequestPart(value = "prioridad", required = false) String prioridad,
            @RequestPart(value = "deadline", required = false) String deadline,
            @RequestPart(value = "deadlineTime", required = false) String deadlineTime,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        PeticionTarea creada = service.crearConAdjuntos(
                requesterName,
                requesterEmail,
                projectName,
                projectCode,
                jiraTask,
                comments,
                devopsAssignee,
                prioridad,
                deadline,
                deadlineTime,
                files
        );
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

    @PutMapping("/{id}/start")
    public ResponseEntity<?> start(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {

        try {
            String adminComment = (body != null) ? body.getOrDefault("adminComment", "") : "";
            PeticionTarea updated = service.start(id, adminComment);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/finish")
    public ResponseEntity<?> finish(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {

        try {
            String adminComment = (body != null) ? body.getOrDefault("adminComment", "") : "";
            PeticionTarea updated = service.finish(id, adminComment);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/resend-confirmation")
    public ResponseEntity<?> resendConfirmation(@PathVariable String id) {
        try {
            service.resendConfirmation(id);
            return ResponseEntity.ok(Map.of("message", "Correo de confirmación reenviado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
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
    @GetMapping("/{id}/attachments/{filename}")
public ResponseEntity<Resource> descargarAdjunto(
        @PathVariable String id,
        @PathVariable String filename) {

    PeticionTarea p = service.getById(id);

    if (p.getAttachments() == null || !p.getAttachments().contains(filename)) {
        return ResponseEntity.notFound().build();
    }

    Path filePath = Paths.get("uploads/peticiones-tareas").resolve(filename);
    if (!Files.exists(filePath)) {
        return ResponseEntity.notFound().build();
    }

    Resource resource = new FileSystemResource(filePath.toFile());
    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
            .body(resource);
}
}
