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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        Path volumenPath = Paths.get(userDir, "volumenDocumentos");
        if (Files.exists(volumenPath) && Files.isDirectory(volumenPath)) {
            return volumenPath.toString();
        }

        Path currentPath = Paths.get(userDir);
        for (int i = 0; i < 3; i++) {
            volumenPath = currentPath.resolve("volumenDocumentos");
            if (Files.exists(volumenPath) && Files.isDirectory(volumenPath)) {
                return volumenPath.toString();
            }
            currentPath = currentPath.getParent();
            if (currentPath == null) break;
        }

        volumenPath = Paths.get(userDir, "volumenDocumentos");
        try {
            Files.createDirectories(volumenPath);
        } catch (IOException e) {
            System.err.println("No se pudo crear volumenDocumentos: " + e.getMessage());
        }
        return volumenPath.toString();
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
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        try {
            Project createdProject = projectService.createProject(project);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }


    // =============================================================
    // 5. UPDATE
    // =============================================================

    // URL: /api/projects/update/{id}
    @PutMapping("/update/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable String id,
            @RequestBody Project details) {

      Optional<Project> updatedProject = projectService.updateProject(id, details);
        return updatedProject.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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