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
import org.springframework.graphql.data.method.annotation.MutationMapping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


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
    // BASIC
    // ==========================
    @QueryMapping
    public String hello() {
        return "GraphQL funcionando correctamente";
    }


    // ==========================
    // BITACORA
    // ==========================
    @QueryMapping
    public List<Bitacora> allBitacoras() {
        return bitacoraRepository.findAllVisible();
    }

  @QueryMapping
public Bitacora bitacoraById(@Argument String id) {
    return bitacoraRepository.findVisibleById(id).orElse(null);
}

    @QueryMapping
    public List<Bitacora> bitacorasByProject(@Argument String idProyecto) {
        return bitacoraRepository.findByProyectoVisible(idProyecto);
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
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @QueryMapping
    public Project getProjectById(@Argument String id) {
        return projectRepository.findById(id).orElse(null);
    }

    @QueryMapping
    public List<Project> searchProjectsByName(@Argument String name) {
        return projectRepository.findByNameContainingIgnoreCase(name);
    }

     // ==========================================================
    // MUTATIONS
    // ==========================================================

    // ==========================
    // CREATE PROCEDURE (ID procedure-XXX)
    // ==========================

    @MutationMapping
    public Procedure createProcedure(@Argument Procedure procedure) {

        Procedure last = proceduresRepository.findTopByIdStartingWithOrderByIdDesc("procedure-");

        int nextNumber = 1;

        if (last != null && last.getId() != null && last.getId().startsWith("procedure-")) {
            String numberPart = last.getId().replace("procedure-", "");
            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        String newId = String.format("procedure-%03d", nextNumber);
        procedure.setId(newId);

        procedure.setDeleted(false);
        procedure.setVisible(true);
        procedure.setCreatedAt(LocalDateTime.now());
        procedure.setUpdatedAt(LocalDateTime.now());

        return proceduresRepository.save(procedure);
    }

    // ==========================
    // SOFT DELETE PROCEDURE
    // ==========================

     @MutationMapping
    public Boolean softDeleteProcedure(@Argument String id) {

        Optional<Procedure> procOpt = proceduresRepository.findById(id);

        if (procOpt.isPresent()) {
            Procedure proc = procOpt.get();
            proc.setDeleted(true);
            proc.setVisible(false);
            proc.setUpdatedAt(LocalDateTime.now());
            proceduresRepository.save(proc);
            return true;
        }

        return false;
    }

    // ==========================
    // PHYSICAL DELETE PROCEDURE
    // ==========================

     @MutationMapping
    public Boolean deleteProcedurePhysical(@Argument String id) {

        Optional<Procedure> procOpt = proceduresRepository.findById(id);

        if (procOpt.isPresent()) {
            proceduresRepository.delete(procOpt.get());
            return true;
        }

        return false;
    }

      // ==========================
    // CREATE PROJECT (ID project-XXX)
    // ==========================
    @MutationMapping
    public Project createProject(@Argument Project project) {

        Project last = projectRepository.findTopByIdStartingWithOrderByIdDesc("project-");

        int nextNumber = 1;

        if (last != null && last.getId() != null && last.getId().startsWith("project-")) {
            String numberPart = last.getId().replace("project-", "");
            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        String newId = String.format("project-%03d", nextNumber);
        project.setId(newId);

        project.setCreatedAt(LocalDateTime.now());

        return projectRepository.save(project);
    }

    @MutationMapping
public Project updateProject(@Argument String id, @Argument("project") Project project) {

    Optional<Project> projectOpt = projectRepository.findById(id);

    if (projectOpt.isPresent()) {
        Project existing = projectOpt.get();

        existing.setCode(project.getCode());
        existing.setName(project.getName());
        existing.setDepartamentOrganisme(project.getDepartamentOrganisme());
        existing.setGestorResponsableSolucio(project.getGestorResponsableSolucio());
        existing.setResponsableProjecte(project.getResponsableProjecte());
        existing.setEquipDesenvolupament(project.getEquipDesenvolupament());
        existing.setEquipProjectesInfra(project.getEquipProjectesInfra());
        existing.setEquipProves(project.getEquipProves());
        existing.setEquipAdminExplotacioXarxes(project.getEquipAdminExplotacioXarxes());
        existing.setOficinaSeguretat(project.getOficinaSeguretat());
        existing.setEquipQualitat(project.getEquipQualitat());
        existing.setEquipAdminOperacions(project.getEquipAdminOperacions());
        existing.setEquipAdminExplotacioSistemes(project.getEquipAdminExplotacioSistemes());
        existing.setGestorIntegracioSolucions(project.getGestorIntegracioSolucions());

        return projectRepository.save(existing);
    }

    return null;
}

@MutationMapping
public Boolean softDeleteProject(@Argument String id) {

    Optional<Project> projectOpt = projectRepository.findById(id);

    if (projectOpt.isPresent()) {

        Project p = projectOpt.get();
        p.setDeleted(true);
        p.setVisible(false);

        projectRepository.save(p);

        return true;
    }

    return false;
}

     // ==========================
    // CREATE BITACORA (ID bitacora-XXX)
    // ==========================
    @MutationMapping
    public Bitacora createBitacora(@Argument Bitacora bitacora) {

        Bitacora last = bitacoraRepository.findTopByOrderByIdDesc();

        int nextNumber = 1;

        if (last != null && last.getId() != null && last.getId().startsWith("bitacora-")) {
            String numberPart = last.getId().replace("bitacora-", "");
            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        String newId = String.format("bitacora-%03d", nextNumber);
        bitacora.setId(newId);

        bitacora.setVisible(true);

        return bitacoraRepository.save(bitacora);
    }

    // ==========================
    // SOFT DELETE BITACORA
    // ==========================
    @MutationMapping
    public Boolean softDeleteBitacora(@Argument String id) {

        Optional<Bitacora> bitOpt = bitacoraRepository.findById(id);

        if (bitOpt.isPresent()) {
            Bitacora b = bitOpt.get();
            b.setVisible(false);
            bitacoraRepository.save(b);
            return true;
        }

        return false;
    }

     // ==========================
    // PHYSICAL DELETE BITACORA
    // ==========================
    @MutationMapping
    public Boolean deleteBitacoraPhysical(@Argument String id) {

        Optional<Bitacora> bitOpt = bitacoraRepository.findById(id);

        if (bitOpt.isPresent()) {
            bitacoraRepository.delete(bitOpt.get());
            return true;
        }

        return false;
    }

    @MutationMapping
public Boolean deleteProjectPhysical(@Argument String id) {

    Optional<Project> projectOpt = projectRepository.findById(id);

    if (projectOpt.isPresent()) {
        projectRepository.delete(projectOpt.get());
        return true;
    }

    return false;
}






















}
