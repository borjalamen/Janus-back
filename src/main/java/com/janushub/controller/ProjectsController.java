package com.janushub.controller;

import com.janushub.model.Project;
import com.janushub.service.ProjectService;
import com.janushub.service.ProjectService.ProjectStats;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:4200,http://localhost:8080", allowedHeaders = "*")
public class ProjectsController {
      private final ProjectService projectService;

    public ProjectsController(ProjectService projectService) {
        this.projectService = projectService;
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
    
}
