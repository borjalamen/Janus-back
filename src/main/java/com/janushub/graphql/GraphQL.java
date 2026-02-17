package com.janushub.graphql;

import com.janushub.model.Bitacora;
import com.janushub.model.Procedure;
import com.janushub.model.Project;
import com.janushub.model.Users;
import com.janushub.repository.BitacoraRepository;
import com.janushub.repository.ProceduresRepository;
import com.janushub.repository.ProjectRepository;
import com.janushub.repository.UserRepository;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.graphql.data.method.annotation.Argument;

import java.util.List;

@Controller
public class GraphQL {

    private final UserRepository userRepository;
    private final BitacoraRepository bitacoraRepository;
    private final ProceduresRepository proceduresRepository;
    private final ProjectRepository projectRepository;

    public GraphQL(UserRepository userRepository,
                                  BitacoraRepository bitacoraRepository,
                                  ProceduresRepository proceduresRepository,
                                  ProjectRepository projectRepository) {
        this.userRepository = userRepository;
        this.bitacoraRepository = bitacoraRepository;
        this.proceduresRepository = proceduresRepository;
        this.projectRepository = projectRepository;
    }

  


    // ==========================
    // BITACORA
    // ==========================
    @QueryMapping
    public List<Bitacora> allBitacoras() {
        return bitacoraRepository.findByVisibleTrue();
    }

    @QueryMapping
    public Bitacora bitacoraById(@Argument String id) {
        return bitacoraRepository.findByIdAndVisibleTrue(id).orElse(null);
    }

    @QueryMapping
    public List<Bitacora> bitacorasByProject(@Argument String idProyecto) {
        return bitacoraRepository.findByIdProyectoAndVisibleTrue(idProyecto);
    }

    // ==========================
    // PROCEDURES
    // ==========================
    @QueryMapping
    public List<Procedure> allProcedures() {
        return proceduresRepository.findByIsDeletedFalse();
    }

    @QueryMapping
    public Procedure procedureById(@Argument String id) {
        return proceduresRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElse(null);
    }

    @QueryMapping
    public List<Procedure> searchProceduresByTitulo(@Argument String titulo) {
        return proceduresRepository.findByTituloContainingIgnoreCaseAndIsDeletedFalse(titulo);
    }

    // ==========================
    // PROJECTS
    // ==========================
    @QueryMapping
    public List<Project> allProjects() {
        return projectRepository.findAll();
    }

    @QueryMapping
    public Project projectById(@Argument String id) {
        return projectRepository.findById(id).orElse(null);
    }

    @QueryMapping
    public List<Project> searchProjectsByName(@Argument String name) {
        return projectRepository.findByNameContainingIgnoreCase(name);
    }
}
