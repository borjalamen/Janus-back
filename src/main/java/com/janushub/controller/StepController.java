package com.janushub.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.catalina.connector.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.janushub.model.StepDocumentID;
import com.janushub.model.StepRepository;

public class StepController {

     private final StepRepository stepRepository;

     public StepController(StepRepository stepRepository) {
            this.stepRepository = stepRepository;
        }

     @PostMapping
    public ResponseEntity<?> createStep(@RequestBody StepDocumentID step) {
        if (stepRepository.existsByTitulo(step.getTitulo())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("ID duplicado");
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(stepRepository.save(step));

        

    }


        public ResponseEntity<?> getStepById(@PathVariable String id) {
        return stepRepository.findById(id)
                .map(ResponseEntity :: ok)
                .orElse(ResponseEntity.notFound().build());


    }

    @PutMapping("/{id}")
public ResponseEntity<?> update(
        @PathVariable String id,
        @RequestBody StepDocumentID step) {

    return stepRepository.findById(id)
            .map(existing -> {
                existing.setTitulo(step.getTitulo());
                existing.setDescripcion(step.getDescripcion());
                existing.setResponsable(step.getResponsable());
                existing.setMetodo(step.getMetodo());
                existing.setOrden(step.getOrden());
                existing.setTags(step.getTags());
                existing.setUpdatedAt(LocalDateTime.now());
                return ResponseEntity.ok(stepRepository.save(existing));
            })
            .orElse(ResponseEntity.notFound().build());
}

        @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        if (!stepRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        stepRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<StepDocumentID>> getAllSteps() {
        List<StepDocumentID> steps = stepRepository.findAll();
        return ResponseEntity.ok(steps);
    }
}




