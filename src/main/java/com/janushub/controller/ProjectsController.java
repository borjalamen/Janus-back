package com.janushub.controller;

import com.janushub.model.Project;
import com.janushub.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
public class ProjectsController {
     private final ProjectRepository repository;

    public ProjectsController(ProjectRepository repository) {
        this.repository = repository;
    }

    // =============================================================
    // 1. GET ALL
    // =============================================================

    // URL: /api/projects/all
    @GetMapping("/all")
    public List<Project> getAllProjects() {
        return repository.findAll();
    }

     // =============================================================
    // 2. GET BY ID
    // =============================================================

    // URL: /api/projects/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =============================================================
    // 3. SEARCH BY NAME
    // =============================================================

    // URL: /api/projects/search/name/{name}
    @GetMapping("/search/name/{name}")
    public List<Project> searchByName(@PathVariable String name) {
        return repository.findByNameContainingIgnoreCase(name);
    }

     // =============================================================
    // 4. CREATE (ID project-XXX)
    // =============================================================

    // URL: /api/projects/create
    @PostMapping("/create")
    public Project createProject(@RequestBody Project project) {

        Project last = repository.findTopByIdStartingWithOrderByIdDesc("project-");

        int nextNumber = 1;

        if (last != null && last.getId() != null) {
            String numberPart = last.getId().replace("project-", "");
            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        String newId = String.format("project-%03d", nextNumber);
        project.setId(newId);

        project.setCreatedAt(LocalDateTime.now());

        return repository.save(project);
    }

    // =============================================================
    // 5. UPDATE
    // =============================================================

    // URL: /api/projects/update/{id}
    @PutMapping("/update/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable String id,
            @RequestBody Project details) {

        Optional<Project> projectOpt = repository.findById(id);

        if (projectOpt.isPresent()) {

            Project existing = projectOpt.get();

            existing.setCode(details.getCode());
            existing.setName(details.getName());
            existing.setDepartamentOrganisme(details.getDepartamentOrganisme());
            existing.setGestorResponsableSolucio(details.getGestorResponsableSolucio());
            existing.setResponsableProjecte(details.getResponsableProjecte());
            existing.setEquipDesenvolupament(details.getEquipDesenvolupament());
            existing.setEquipProjectesInfra(details.getEquipProjectesInfra());
            existing.setEquipProves(details.getEquipProves());
            existing.setEquipAdminExplotacioXarxes(details.getEquipAdminExplotacioXarxes());
            existing.setOficinaSeguretat(details.getOficinaSeguretat());
            existing.setEquipQualitat(details.getEquipQualitat());
            existing.setEquipAdminOperacions(details.getEquipAdminOperacions());
            existing.setEquipAdminExplotacioSistemes(details.getEquipAdminExplotacioSistemes());
            existing.setGestorIntegracioSolucions(details.getGestorIntegracioSolucions());

            return ResponseEntity.ok(repository.save(existing));
        }

        return ResponseEntity.notFound().build();
    }

     // =============================================================
    // 6. DELETE (HARD DELETE)
    // =============================================================

    // URL: /api/projects/delete/{id}
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable String id) {

        Optional<Project> projectOpt = repository.findById(id);

        if (projectOpt.isPresent()) {
            repository.delete(projectOpt.get());
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.notFound().build();
    }
    
}
