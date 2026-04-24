package com.janushub.controller;

import com.janushub.model.ScrumSprint;
import com.janushub.model.ScrumTask;
import com.janushub.repository.ScrumSprintRepository;
import com.janushub.repository.ScrumTaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/scrum/sprint")
public class ScrumSprintController {

    private final ScrumSprintRepository sprintRepo;
    private final ScrumTaskRepository taskRepo;

    public ScrumSprintController(ScrumSprintRepository sprintRepo, ScrumTaskRepository taskRepo) {
        this.sprintRepo = sprintRepo;
        this.taskRepo = taskRepo;
    }

    // ── GET active sprint ────────────────────────────────────────────────────
    @GetMapping("/active")
    public ResponseEntity<ScrumSprint> getActive() {
        return sprintRepo.findByActiveTrue()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // ── GET all finished sprints (history) ───────────────────────────────────
    @GetMapping("/history")
    public List<ScrumSprint> getHistory() {
        return sprintRepo.findByActiveFalse();
    }

    // ── GET tasks for a sprint ───────────────────────────────────────────────
    @GetMapping("/{id}/tasks")
    public List<ScrumTask> getTasksBySprint(@PathVariable String id) {
        return taskRepo.findBySprintIdAndVisibleTrue(id);
    }

    // ── START a new sprint ───────────────────────────────────────────────────
    @PostMapping("/start")
    public ResponseEntity<ScrumSprint> start(@RequestBody ScrumSprint body) {
        // Only one active sprint at a time
        if (sprintRepo.findByActiveTrue().isPresent()) {
            return ResponseEntity.status(409).build();
        }
        body.setId(null);
        body.setActive(true);
        if (body.getStartDate() == null || body.getStartDate().isBlank()) {
            body.setStartDate(LocalDate.now().toString());
        }
        body.setEndDate(null);
        ScrumSprint saved = sprintRepo.save(body);

        // Associate all existing tasks without sprint to this new sprint
        List<ScrumTask> orphan = taskRepo.findBySprintIdIsNullAndVisibleTrue();
        for (ScrumTask t : orphan) {
            t.setSprintId(saved.getId());
            taskRepo.save(t);
        }
        return ResponseEntity.ok(saved);
    }

    // ── SAVE snapshot (called from frontend on each board change) ────────────
    @PutMapping("/{id}/snapshot")
    public ResponseEntity<ScrumSprint> saveSnapshot(
            @PathVariable String id,
            @RequestBody ScrumSprint.SprintSnapshot snapshot) {

        Optional<ScrumSprint> opt = sprintRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        ScrumSprint sprint = opt.get();
        List<ScrumSprint.SprintSnapshot> snaps = sprint.getSnapshots();
        if (snaps == null) { snaps = new java.util.ArrayList<>(); sprint.setSnapshots(snaps); }

        String today = snapshot.getDate() != null ? snapshot.getDate() : LocalDate.now().toString();
        snapshot.setDate(today);

        // Replace today's snapshot if exists
        boolean replaced = false;
        for (int i = 0; i < snaps.size(); i++) {
            if (today.equals(snaps.get(i).getDate())) {
                snaps.set(i, snapshot);
                replaced = true;
                break;
            }
        }
        if (!replaced) snaps.add(snapshot);

        sprintRepo.save(sprint);
        return ResponseEntity.ok(sprint);
    }

    // ── END sprint ────────────────────────────────────────────────────────────
    @PutMapping("/{id}/end")
    public ResponseEntity<ScrumSprint> end(
            @PathVariable String id,
            @RequestBody ScrumSprint summary) {

        Optional<ScrumSprint> opt = sprintRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        ScrumSprint sprint = opt.get();
        sprint.setActive(false);
        sprint.setEndDate(LocalDate.now().toString());
        sprint.setTotalTasks(summary.getTotalTasks());
        sprint.setDoneTasks(summary.getDoneTasks());
        sprint.setTotalHours(summary.getTotalHours());
        sprint.setDoneHours(summary.getDoneHours());

        sprintRepo.save(sprint);

        // Delete 'done' tasks from this sprint
        List<ScrumTask> done = taskRepo.findBySprintIdAndVisibleTrue(id)
                .stream().filter(t -> "done".equals(t.getStatus())).toList();
        for (ScrumTask t : done) {
            taskRepo.deleteById(t.getId());
        }

        return ResponseEntity.ok(sprint);
    }
}
