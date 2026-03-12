package com.janushub.controller;

import com.janushub.model.PeticionTarea;
import com.janushub.service.PeticionTareaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Value("${upload.root:uploads}")
    private String uploadRoot;

    @PostMapping(consumes = {"multipart/form-data", "application/x-www-form-urlencoded"})
    public ResponseEntity<?> crear(
            @RequestParam("requesterName") String requesterName,
            @RequestParam("requesterEmail") String requesterEmail,
            @RequestParam("projectName") String projectName,
            @RequestParam("projectCode") String projectCode,
            @RequestParam("jiraTask") String jiraTask,
            @RequestParam(value = "comments", required = false) String comments,
            @RequestParam(value = "devopsAssignee", required = false) String devopsAssignee,
            @RequestParam(value = "deadline", required = false) String deadline,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        try {
            if (requesterName == null || requesterName.isBlank()) {
                return ResponseEntity.badRequest().body("El nombre del peticionario es obligatorio.");
            }
            if (jiraTask == null || jiraTask.isBlank()) {
                return ResponseEntity.badRequest().body("La tarea JIRA es obligatoria.");
            }

            PeticionTarea p = new PeticionTarea();
            p.setRequesterName(requesterName);
            p.setRequesterEmail(requesterEmail);
            p.setProjectName(projectName);
            p.setProjectCode(projectCode);
            p.setJiraTask(jiraTask);
            p.setComments(comments);
            p.setDevopsAssignee(devopsAssignee != null ? devopsAssignee : "Cualquiera");
            if (deadline != null && !deadline.isBlank()) {
                try {
                    p.setDeadline(LocalDateTime.parse(deadline.replace("Z", "")));
                } catch (Exception ignored) {}
            }

            // Guardar adjuntos
            List<String> attachmentPaths = new ArrayList<>();
            if (files != null) {
                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) continue;
                    String safeName = file.getOriginalFilename() != null
                            ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                            : "file";
                    Path dir = Paths.get(uploadRoot).toAbsolutePath()
                            .resolve("peticiones")
                            .resolve(String.valueOf(System.currentTimeMillis()));
                    Files.createDirectories(dir);
                    Path dest = dir.resolve(safeName);
                    Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
                    attachmentPaths.add(dest.toString());
                }
            }
            p.setAttachments(attachmentPaths.isEmpty() ? null : attachmentPaths);

            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(p));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar adjuntos: " + e.getMessage());
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