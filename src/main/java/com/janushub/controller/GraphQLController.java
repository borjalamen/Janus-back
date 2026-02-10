package com.janushub.controller;

import com.janushub.model.*;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
public class GraphQLController {

     private final StepRepository stepRepository;
    private final ProcedureRepository procedureRepository;

    public GraphQLController(StepRepository stepRepository, ProcedureRepository procedureRepository) {
        this.stepRepository = stepRepository;
        this.procedureRepository = procedureRepository;
    }

    // ------------------------
    // STEPS
    // ------------------------

    @QueryMapping
    public List<StepDocumentID> getAllSteps() {
        return stepRepository.findAll();
    }

      @QueryMapping
    public StepDocumentID getStepByStepID(@Argument String stepId) {
        return stepRepository.findByStepId(stepId);
    }

     @QueryMapping
    public List<StepDocumentID> searchStepsByResponsable(@Argument String responsable) {
        return stepRepository.findByResponsableContainingIgnoreCase(responsable);
    }

    @QueryMapping
    public List<StepDocumentID> searchStepsByTag(@Argument String tag) {
        return stepRepository.findByTagsContaining(tag);
    }

    // ------------------------
    // PROCEDURES
    // ------------------------

    @QueryMapping
    public List<ProcedureDocument> getAllProcedures() {
        return procedureRepository.findAll();
    }

     @QueryMapping
    public ProcedureDocument getProcedureByProcedureID(@Argument String procedureId) {
        return procedureRepository.findByProcedureId(procedureId);
    }

    @QueryMapping
    public List<ProcedureDocument> searchProceduresByTitulo(@Argument String titulo) {
        return procedureRepository.findByTituloContainingIgnoreCase(titulo);
    }

    // ------------------------
    // RESOLVER steps dins procedure
    // ------------------------

     @org.springframework.graphql.data.method.annotation.SchemaMapping(typeName = "Procedure", field = "steps")
    public List<StepDocumentID> steps(ProcedureDocument procedure) {

        if (procedure.getSteps() == null) {
            return new ArrayList<>();
        }

        List<StepDocumentID> result = new ArrayList<>();

        for (String stepId : procedure.getSteps()) {
            StepDocumentID step = stepRepository.findByStepId(stepId);
            if (step != null) {
                result.add(step);
            }
        }

        return result;
    }
    
}
