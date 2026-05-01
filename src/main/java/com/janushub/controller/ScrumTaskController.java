package com.janushub.controller;

import com.janushub.model.ScrumTask;
import com.janushub.repository.ScrumSprintRepository;
import com.janushub.repository.ScrumTaskRepository;
import com.janushub.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/scrum")
public class ScrumTaskController {

    private final ScrumTaskRepository repository;
    private final ScrumSprintRepository sprintRepository;
    private final NotificationService notificationService;

    public ScrumTaskController(ScrumTaskRepository repository, ScrumSprintRepository sprintRepository,
                               NotificationService notificationService) {
        this.repository = repository;
        this.sprintRepository = sprintRepository;
        this.notificationService = notificationService;
    }

    /** Returns tasks for the active sprint, or all visible tasks if no sprint is active */
    @GetMapping("/all")
    public List<ScrumTask> getAll() {
        return sprintRepository.findByActiveTrue()
                .map(sprint -> repository.findBySprintIdAndVisibleTrue(sprint.getId()))
                .orElseGet(() -> repository.findByVisibleTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScrumTask> getById(@PathVariable String id) {
        return repository.findById(id)
                .filter(t -> Boolean.TRUE.equals(t.getVisible()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    public ScrumTask create(@RequestBody ScrumTask task) {
        if (task.getId() != null && task.getId().isBlank()) {
            task.setId(null);
        }
        if (task.getVisible() == null) {
            task.setVisible(true);
        }
        if (task.getCreatedAt() == null || task.getCreatedAt().isBlank()) {
            task.setCreatedAt(Instant.now().toString());
        }
        task.setUpdatedAt(Instant.now().toString());

        // Assign to active sprint if the task has no sprint yet
        if (task.getSprintId() == null || task.getSprintId().isBlank()) {
            sprintRepository.findByActiveTrue()
                    .ifPresent(sprint -> task.setSprintId(sprint.getId()));
        }

        ScrumTask saved = repository.save(task);
        notificationService.broadcast(
                "SCRUM_TAREA_NUEVA",
                "Nueva tarea Scrum",
                "Tarea creada: " + saved.getTitle(),
                "/scrum"
        );
        return saved;
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ScrumTask> update(@PathVariable String id, @RequestBody ScrumTask details) {
        return repository.findById(id)
                .map(existing -> {
                    details.setId(id);
                    if (details.getCreatedAt() == null || details.getCreatedAt().isBlank()) {
                        details.setCreatedAt(existing.getCreatedAt());
                    }
                    if (details.getVisible() == null) {
                        details.setVisible(true);
                    }
                    // Preserve existing sprintId if frontend doesn't send one
                    if ((details.getSprintId() == null || details.getSprintId().isBlank())
                            && existing.getSprintId() != null) {
                        details.setSprintId(existing.getSprintId());
                    }
                    details.setUpdatedAt(Instant.now().toString());
                    ScrumTask saved = repository.save(details);
                    notificationService.broadcast(
                            "SCRUM_TAREA_ACTUALIZADA",
                            "Tarea Scrum actualizada",
                            "Tarea actualizada: " + saved.getTitle() + " [" + saved.getStatus() + "]",
                            "/scrum"
                    );
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        notificationService.broadcast(
                "SCRUM_TAREA_ELIMINADA",
                "Tarea Scrum eliminada",
                "Una tarea del sprint ha sido eliminada",
                "/scrum"
        );
        return ResponseEntity.noContent().build();
    }
}

