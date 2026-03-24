package com.janushub.service;
 
import com.janushub.model.Project;
import com.janushub.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
 
@Service
public class ProjectService {
 
     private final ProjectRepository repository;
 
    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }
 
    /**
     * Obtener todos los proyectos no eliminados
     */
    public List<Project> getAllProjects() {
        return repository.findByDeletedFalse();
    }
 
    /**
     * Obtener proyecto por ID
     */
    public Optional<Project> getProjectById(String id) {
        Optional<Project> project = repository.findById(id);
        if (project.isPresent() && !project.get().getDeleted()) {
            return project;
        }
        return Optional.empty();
    }
 
    /**
     * Obtener proyecto por código
     */
    public Optional<Project> getProjectByCode(String code) {
        return repository.findByCodigoProyecto(code);
    }
 
    /**
     * Buscar por nombre
     */
    public List<Project> searchByName(String name) {
        return repository.findByNombreContainingIgnoreCase(name);
    }
 
    /**
     * Crear nuevo proyecto
     * Valida que el campo codigoProyecto no esté vacío y no sea duplicado
     */
    public Project createProject(Project project) {
        // Validar que codigoProyecto sea requerido y no esté vacío
        if (project.getCodigoProyecto() == null || project.getCodigoProyecto().trim().isEmpty()) {
            throw new IllegalArgumentException("El código del proyecto es obligatorio y no puede estar vacío");
        }

        // Validar que codigoProyecto no sea duplicado
        Project existingProject = repository.findByCodigoProyecto(project.getCodigoProyecto()).orElse(null);
        if (existingProject != null) {
            throw new IllegalArgumentException("El proyecto con código '" + project.getCodigoProyecto() + "' ya está dado de alta en la base de datos");
        }
 
        // Generar ID interno si no existe
        if (project.getId() == null || project.getId().isEmpty()) {
            Project lastProject = repository.findTopByIdStartingWithOrderByIdDesc("project-");
            int nextNumber = 1;
           
            if (lastProject != null && lastProject.getId() != null) {
                String numberPart = lastProject.getId().replace("project-", "");
                try {
                    nextNumber = Integer.parseInt(numberPart) + 1;
                } catch (NumberFormatException e) {
                    nextNumber = 1;
                }
            }
           
            project.setId(String.format("project-%03d", nextNumber));
        }
 
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        project.setDeleted(false);
        project.setVisible(true);
 
        return repository.save(project);
    }
 
    /**
     * Actualizar proyecto
     * Valida que si se cambia el codigoProyecto, no sea duplicado
     */
    public Optional<Project> updateProject(String id, Project details) {
        Optional<Project> projectOpt = repository.findById(id);
       
        if (projectOpt.isPresent()) {
            Project existing = projectOpt.get();
           
            // Validar que si se cambia el codigoProyecto, no sea duplicado
            if (details.getCodigoProyecto() != null && 
                !details.getCodigoProyecto().isEmpty() && 
                !details.getCodigoProyecto().equals(existing.getCodigoProyecto())) {
                Project existingWithCode = repository.findByCodigoProyecto(details.getCodigoProyecto()).orElse(null);
                if (existingWithCode != null) {
                    throw new IllegalArgumentException("El proyecto con código '" + details.getCodigoProyecto() + "' ya está dado de alta en la base de datos");
                }
            }
           
            // Actualizar todos los campos
            existing.setCodigoProyecto(details.getCodigoProyecto());
            existing.setNombre(details.getNombre());
            existing.setCodigoImputacion(details.getCodigoImputacion());
            existing.setLote(details.getLote());
            existing.setDepartamento(details.getDepartamento());
           
            // URLs
            existing.setUrlEntornoDesarrollo(details.getUrlEntornoDesarrollo());
            existing.setUrlEntornoIntegracion(details.getUrlEntornoIntegracion());
            existing.setUrlEntornoPreproduccion(details.getUrlEntornoPreproduccion());
            existing.setUrlEntornoProduccion(details.getUrlEntornoProduccion());
           
            // Responsables
            existing.setResponsableProyecto(details.getResponsableProyecto());
            existing.setResponsableTecnico(details.getResponsableTecnico());
            existing.setHoraDaily(details.getHoraDaily());
           
            // Listas
            if (details.getIp() != null) existing.setIp(details.getIp());
            if (details.getTareas() != null) existing.setTareas(details.getTareas());
            if (details.getHerramientas() != null) existing.setHerramientas(details.getHerramientas());
            if (details.getJenkinsNodes() != null) existing.setJenkinsNodes(details.getJenkinsNodes());
            if (details.getDockerImages() != null) existing.setDockerImages(details.getDockerImages());
            if (details.getPipelines() != null) existing.setPipelines(details.getPipelines());
            if (details.getRepositorios() != null) existing.setRepositorios(details.getRepositorios());
            if (details.getBbdd() != null) existing.setBbdd(details.getBbdd());
            if (details.getOpenshift() != null) existing.setOpenshift(details.getOpenshift());
            if (details.getUsuarios() != null) existing.setUsuarios(details.getUsuarios());
           
            // Notas
            existing.setNotasGenerales(details.getNotasGenerales());
            existing.setEntornoNotas(details.getEntornoNotas());
           
            // Equipos
            if (details.getEquipoMinsait() != null) existing.setEquipoMinsait(details.getEquipoMinsait());
            if (details.getDevMachines() != null) existing.setDevMachines(details.getDevMachines());
           
            // Herramientas Mind
            if (details.getHerramientasMind() != null) existing.setHerramientasMind(details.getHerramientasMind());
 
            // Documentos
            if (details.getDocuments() != null) existing.setDocuments(details.getDocuments());
           
            existing.setUpdatedAt(LocalDateTime.now());
           
            return Optional.of(repository.save(existing));
        }
       
        return Optional.empty();
    }
 
    /**
     * Guardar proyecto completo (mantener todos los campos)
     */
    public Project saveProject(Project project) {
        project.setUpdatedAt(LocalDateTime.now());
        return repository.save(project);
    }
 
    /**
     * Eliminar proyecto (soft delete)
     */
    public boolean softDeleteProject(String id) {
        Optional<Project> projectOpt = repository.findById(id);
       
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            project.setDeleted(true);
            project.setUpdatedAt(LocalDateTime.now());
            repository.save(project);
            return true;
        }
       
        return false;
    }
 
    /**
     * Eliminar proyecto de forma permanente (hard delete)
     */
    public boolean deleteProject(String id) {
        Optional<Project> projectOpt = repository.findById(id);
       
        if (projectOpt.isPresent()) {
            repository.deleteById(id);
            return true;
        }
       
        return false;
    }
 
    /**
     * Obtener estadísticas de proyectos
     */
    public ProjectStats getStats() {
        List<Project> projects = getAllProjects();
        ProjectStats stats = new ProjectStats();
       
        stats.setTotalProjects(projects.size());
        stats.setUniqueDepartments((int) projects.stream()
            .map(Project::getDepartamento)
            .distinct()
            .count());
        stats.setUniqueLots((int) projects.stream()
            .map(Project::getLote)
            .distinct()
            .count());
        stats.setTotalTasks(projects.stream()
            .mapToInt(p -> p.getTareas() != null ? p.getTareas().size() : 0)
            .sum());
       
        return stats;
    }
 
    // ====== CLASE HELPER PARA ESTADÍSTICAS ======
   
    public static class ProjectStats {
        private int totalProjects;
        private int uniqueDepartments;
        private int uniqueLots;
        private int totalTasks;
 
        public int getTotalProjects() {
            return totalProjects;
        }
 
        public void setTotalProjects(int totalProjects) {
            this.totalProjects = totalProjects;
        }
 
        public int getUniqueDepartments() {
            return uniqueDepartments;
        }
 
        public void setUniqueDepartments(int uniqueDepartments) {
            this.uniqueDepartments = uniqueDepartments;
        }
 
        public int getUniqueLots() {
            return uniqueLots;
        }
 
        public void setUniqueLots(int uniqueLots) {
            this.uniqueLots = uniqueLots;
        }
 
        public int getTotalTasks() {
            return totalTasks;
        }
 
        public void setTotalTasks(int totalTasks) {
            this.totalTasks = totalTasks;
        }
    }
}