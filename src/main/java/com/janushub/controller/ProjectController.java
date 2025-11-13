package com.janushub.controller;

import com.janushub.model.Project;
import com.janushub.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;

    @QueryMapping
    public List<Project> allProjects() {
        return projectRepository.findAll();
    }

    @QueryMapping
    public Optional<Project> projectByCode(@Argument String code) {
        return projectRepository.findByCode(code);
    }

    @MutationMapping
    public Project createProject(@Argument ProjectInput project) {
        Project newProject = new Project();
        newProject.setCode(project.code());
        newProject.setName(project.name());
        newProject.setDepartamentOrganisme(project.departamentOrganisme());
        newProject.setGestorResponsableSolucio(project.gestorResponsableSolucio());
        newProject.setResponsableProjecte(project.responsableProjecte());
        newProject.setEquipDesenvolupament(project.equipDesenvolupament());
        newProject.setEquipProjectesInfra(project.equipProjectesInfra());
        newProject.setEquipProves(project.equipProves());
        newProject.setEquipAdminExplotacioXarxes(project.equipAdminExplotacioXarxes());
        newProject.setOficinaSeguretat(project.oficinaSeguretat());
        newProject.setEquipQualitat(project.equipQualitat());
        newProject.setEquipAdminOperacions(project.equipAdminOperacions());
        newProject.setEquipAdminExplotacioSistemes(project.equipAdminExplotacioSistemes());
        newProject.setGestorIntegracioSolucions(project.gestorIntegracioSolucions());
        newProject.setCreatedAt(LocalDateTime.now());
        return projectRepository.save(newProject);
    }
}

record ProjectInput(
    String code,
    String name,
    String departamentOrganisme,
    String gestorResponsableSolucio,
    String responsableProjecte,
    String equipDesenvolupament,
    String equipProjectesInfra,
    String equipProves,
    String equipAdminExplotacioXarxes,
    String oficinaSeguretat,
    String equipQualitat,
    String equipAdminOperacions,
    String equipAdminExplotacioSistemes,
    String gestorIntegracioSolucions
) {}
