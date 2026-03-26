package com.janushub.controller;

import com.janushub.model.Project;
import com.janushub.service.ProjectService;
import com.janushub.service.ProjectService.ProjectStats;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:4200,http://localhost:8080", allowedHeaders = "*")
public class ProjectsController {
      private final ProjectService projectService;
      private String VOLUMEN;

    public ProjectsController(ProjectService projectService) {
        this.projectService = projectService;
        this.VOLUMEN = findVolumenPath();
    }

    /**
     * Busca dinámicamente la carpeta 'volumenDocumentos'
     */
    private String findVolumenPath() {
        String userDir = System.getProperty("user.dir");
        String overridePath = System.getenv("JANUS_VOLUMEN_PATH");

        if (overridePath != null && !overridePath.trim().isEmpty()) {
            Path custom = Paths.get(overridePath.trim()).toAbsolutePath().normalize();
            try {
                Files.createDirectories(custom);
                return custom.toString();
            } catch (IOException e) {
                System.err.println("No se pudo usar JANUS_VOLUMEN_PATH=" + custom + ": " + e.getMessage());
            }
        }

        Path base = Paths.get(userDir).toAbsolutePath().normalize();
        List<Path> rawCandidates = new ArrayList<>();
        rawCandidates.add(base.resolve("volumenDocumentos"));
        rawCandidates.add(base.resolve("Janus-back").resolve("volumenDocumentos"));

        Path currentPath = base;
        for (int i = 0; i < 4 && currentPath != null; i++) {
            rawCandidates.add(currentPath.resolve("volumenDocumentos"));
            rawCandidates.add(currentPath.resolve("Janus-back").resolve("volumenDocumentos"));
            currentPath = currentPath.getParent();
        }

        Set<Path> candidates = new LinkedHashSet<>();
        for (Path candidate : rawCandidates) {
            candidates.add(candidate.normalize());
        }

        Path firstExisting = null;
        Path nonEmpty = null;

        for (Path candidate : candidates) {
            if (Files.exists(candidate) && Files.isDirectory(candidate)) {
                if (firstExisting == null) {
                    firstExisting = candidate;
                }
                try (Stream<Path> entries = Files.list(candidate)) {
                    if (entries.findAny().isPresent()) {
                        nonEmpty = candidate;
                        break;
                    }
                } catch (IOException ignored) {
                    // If folder cannot be listed, keep evaluating other candidates.
                }
            }
        }

        Path selected = nonEmpty != null ? nonEmpty : firstExisting;
        if (selected == null) {
            selected = base.resolve("volumenDocumentos");
            try {
                Files.createDirectories(selected);
            } catch (IOException e) {
                System.err.println("No se pudo crear volumenDocumentos: " + e.getMessage());
            }
        }

        return selected.toString();
    }

    // =============================================================
    // 1. GET ALL
    // =============================================================

    // URL: /api/projects/all
    @GetMapping("/all")
    public ResponseEntity<List<Project>> getAllProjects() {
        List<Project> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }


     // =============================================================
    // 2. GET BY ID
    // =============================================================

    // URL: /api/projects/{id}
    @GetMapping("/{id}")
     public ResponseEntity<Project> getProjectById(@PathVariable String id) {
        Optional<Project> project = projectService.getProjectById(id);
        return project.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET: Obtener proyecto por código
     * GET /api/projects/code/{code}
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Project> getProjectByCode(@PathVariable String code) {
        Optional<Project> project = projectService.getProjectByCode(code);
        return project.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }



    // =============================================================
    // 3. SEARCH BY NAME
    // =============================================================

    // URL: /api/projects/search/name/{name}
    @GetMapping("/search/name/{name}")
    public ResponseEntity<List<Project>> searchByName(@PathVariable String name) {
        List<Project> projects = projectService.searchByName(name);
        return ResponseEntity.ok(projects);
    }

    /**
     * GET: Obtener estadísticas
     * GET /api/projects/stats/summary
     */
    @GetMapping("/stats/summary")
    public ResponseEntity<ProjectStats> getStats() {
        ProjectStats stats = projectService.getStats();
        return ResponseEntity.ok(stats);
    }


     // =============================================================
    // 4. CREATE (ID project-XXX)
    // =============================================================

    // URL: /api/projects/create
    @PostMapping("/create")
    public ResponseEntity<?> createProject(@RequestBody Project project) {
        try {
            Project createdProject = projectService.createProject(project);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            // Detectar error de duplicado de MongoDB
            if (errorMessage != null && errorMessage.contains("non unique result")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No se puede crear el proyecto. El código del proyecto o el nombre ya existen en la base de datos.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al crear el proyecto: " + errorMessage);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }


    // =============================================================
    // 5. UPDATE
    // =============================================================

    // URL: /api/projects/update/{id}
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateProject(
            @PathVariable String id,
            @RequestBody Project details) {
        try {
            Optional<Project> updatedProject = projectService.updateProject(id, details);
            return updatedProject.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            // Detectar error de duplicado de MongoDB
            if (errorMessage != null && errorMessage.contains("non unique result")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "No se puede actualizar el proyecto. El código del proyecto o el nombre ya existen en la base de datos.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            Map<String, String> error = new HashMap<>();
            error.put("error", "Error al actualizar el proyecto: " + errorMessage);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * DELETE: Soft delete - marcar como eliminado
     * DELETE /api/projects/soft-delete/{id}
     */
    @DeleteMapping("/soft-delete/{id}")
    public ResponseEntity<?> softDeleteProject(@PathVariable String id) {
        boolean deleted = projectService.softDeleteProject(id);
        if (deleted) {
            return ResponseEntity.ok().body("Proyecto marcado como eliminado");
        }
        return ResponseEntity.notFound().build();
    }


     // =============================================================
    // 6. DELETE (HARD DELETE)
    // =============================================================

    // URL: /api/projects/delete/{id}
    @DeleteMapping("/delete/{id}")
   public ResponseEntity<?> deleteProject(@PathVariable String id) {
        boolean deleted = projectService.deleteProject(id);
        if (deleted) {
            return ResponseEntity.ok().body("Proyecto eliminado permanentemente");
        }
        return ResponseEntity.notFound().build();
    }

    // =============================================================
    // 7. DOCUMENTOS - SUBIR
    // =============================================================

    /**
     * POST: Subir documento a un proyecto
     * POST /api/projects/{id}/documents/upload
     */
    @PostMapping("/{id}/documents/upload")
    public ResponseEntity<?> uploadProjectDocument(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("descripcion") String descripcion) {
        try {
            // Obtener proyecto
            Optional<Project> optProject = projectService.getProjectById(id);
            if (!optProject.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Project project = optProject.get();
            
            // Crear directorio para el proyecto
            Path projectDir = Paths.get(VOLUMEN, id);
            Files.createDirectories(projectDir);

            // Guardar archivo
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = projectDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Crear documento en proyecto
            Project.ProjectDocument doc = new Project.ProjectDocument();
            doc.setNombre(file.getOriginalFilename());
            doc.setDescripcion(descripcion);
            doc.setTipo(file.getContentType());
            doc.setPath(id + "/" + fileName);  // Ruta relativa desde volumenDocumentos

            // Agregar documento al proyecto
            if (project.getDocuments() == null) {
                project.setDocuments(new java.util.ArrayList<>());
            }
            project.getDocuments().add(doc);

            // Guardar proyecto actualizado
            projectService.updateProject(id, project);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Documento subido correctamente");
            response.put("document", doc);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al subir archivo: " + e.getMessage()));
        }
    }

    // =============================================================
    // 8. DOCUMENTOS - DESCARGAR
    // =============================================================

    /**
     * GET: Descargar documento de un proyecto
     * GET /api/projects/{id}/documents/download
     */
    @GetMapping("/{id}/documents/download")
    public ResponseEntity<?> downloadProjectDocument(
            @PathVariable String id,
            @RequestParam String fileName) {
        try {
            Path filePath = Paths.get(VOLUMEN, id, fileName);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            File file = filePath.toFile();
            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + file.getName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()))
                    .body(resource);
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =============================================================
    // 9. DOCUMENTOS - ELIMINAR
    // =============================================================

    /**
     * DELETE: Eliminar documento de un proyecto
     * DELETE /api/projects/{id}/documents/delete
     */
    @DeleteMapping("/{id}/documents/delete")
    public ResponseEntity<?> deleteProjectDocument(
            @PathVariable String id,
            @RequestParam String fileName) {
        try {
            Optional<Project> optProject = projectService.getProjectById(id);
            if (!optProject.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Project project = optProject.get();

            // Eliminar archivo del sistema de archivos
            Path filePath = Paths.get(VOLUMEN, id, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }

            // Eliminar documento del proyecto
            if (project.getDocuments() != null) {
                project.getDocuments().removeIf(doc -> 
                    doc.getPath().endsWith(fileName));
                projectService.updateProject(id, project);
            }

            return ResponseEntity.ok(Map.of("message", "Documento eliminado correctamente"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar archivo: " + e.getMessage()));
        }
    }
    
}