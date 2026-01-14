package com.janushub.controller;

import java.util.List;

import org.apache.catalina.connector.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
            return ResponseEntity.badRequest().body("El título del step ya existe.");
        }

        StepDocumentID savedStep = stepRepository.save(step);
        return ResponseEntity.ok(savedStep);

    }


        public ResponseEntity<?> getStepById(@PathVariable String id) {
        return stepRepository.findById(id)
                .map(ResponseEntity :: ok)
                .orElse(ResponseEntity.notFound().build());


    }

    @GetMapping
    public List<StepDocumentID> getAllSteps() {
        return stepRepository.findAll();
    }
}

