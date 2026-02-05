package com.janushub.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import com.janushub.model.StepDocumentID;
import com.janushub.model.StepRepository;

@RestController
@RequestMapping("/api/steps") // ← ruta base correcta
public class StepController {

    private final StepRepository stepRepository;

    public StepController(StepRepository stepRepository) {
        this.stepRepository = stepRepository;
    }

     @PostMapping
    public ResponseEntity<?> createStep(@RequestBody StepDocumentID step) {

        step.setID(null); 


        if (stepRepository.existsByStepID(step.getStepById())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("ID duplicado" + step.getStepById());
        }

        
        step.setCreatedAt(LocalDateTime.now());
        step.setUpdatedAt(LocalDateTime.now());
        step.setDeleted(false);
        step.setVisible(true);

        StepDocumentID saved = StepRepository.save(step);


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(stepRepository.save(step));        

    }

    @GetMapping
    public ResponseEntity<List<StepDocumentID>> getAllSteps() {
        return ResponseEntity.ok(stepRepository.findAll());
    }


    @GetMapping("/{stepId}")


        public ResponseEntity<?> getStepById(@PathVariable String stepId) {
        return stepRepository.findByStepId(StepId)
                .map(ResponseEntity :: ok)
                .orElse(ResponseEntity.notFound().build());


    }

    @PutMapping("/{stepId}")
public ResponseEntity<?> update(
        @PathVariable String stepId,
        @RequestBody StepDocumentID step) {

    return stepRepository.findById(stepId)
            .map(existing -> {
                existing.setTitulo(step.getTitulo());
                existing.setDescripcion(step.getDescripcion());
                existing.setResponsable(step.getResponsable());
                existing.setMetodo(step.getMetodo());
                existing.setOrden(step.getOrden());
                existing.setTags(step.getTags());
                existing.setUpdatedAt(LocalDateTime.now());

                StepDocumentID updatedStep = stepRepository.save(existing);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
}

        @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStep(@PathVariable String stepId) {
        return stepRepository.findByStepId(stepId)
                .map(existing -> {
                    stepRepository.delete(existing);
                    return ResponseEntity.ok("Step eliminado con stepId: " + stepId);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No existe Step con stepId: " + stepId));
    }
   
}




