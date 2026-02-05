package com.janushub.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

        step.setId(null); 

         if (step.getStepId() == null || step.getStepId().isBlank()) {
            step.setStepId("STEP-" + UUID.randomUUID());
        }



        if (stepRepository.existsByStepId(step.getStepId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("ID duplicado" + step.getStepId());
        }

        
        step.setCreatedAt(LocalDateTime.now());
        step.setUpdatedAt(LocalDateTime.now());
        step.setDeleted(false);
        step.setVisible(true);

        StepDocumentID saved = stepRepository.save(step);


        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(saved);
    }
    public ResponseEntity<List<StepDocumentID>> getAllSteps() {
        return ResponseEntity.ok(stepRepository.findAll());
    }


    @GetMapping("/{stepId}")


        public ResponseEntity<?> getStepById(@PathVariable String stepId) {
              StepDocumentID step = stepRepository.findByStepId(stepId);

              
        if (step == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No existe Step con stepId: " + stepId);
        }

        return ResponseEntity.ok(step);

    }

    @PutMapping("/{stepId}")
public ResponseEntity<?> updateStep(
        @PathVariable String stepId,
        @RequestBody StepDocumentID step) {

            StepDocumentID existing = stepRepository.findByStepId(stepId);

        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No existe Step con stepId: " + stepId);
        }

    
                existing.setTitulo(step.getTitulo());
                existing.setDescripcion(step.getDescripcion());
                existing.setResponsable(step.getResponsable());
                existing.setMetodo(step.getMetodo());
                existing.setOrden(step.getOrden());
                existing.setTags(step.getTags());
                existing.setUpdatedAt(LocalDateTime.now());

                StepDocumentID updatedStep = stepRepository.save(existing);
                return ResponseEntity.ok(updatedStep);
          
}

        @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStep(@PathVariable String stepId) {

        StepDocumentID existing = stepRepository.findByStepId(stepId);

         if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No existe Step con stepId: " + stepId);
        }

         stepRepository.delete(existing);  return ResponseEntity.ok("Step eliminado con stepId: " + stepId);


    }
   
}




