package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "projects")
public class Project {
    @Id
    private String id;
    
    // Información básica
    private String codigoProyecto;
    private String nombre;
    private String codigoImputacion;
    private String lote;
    private String departamento;
    
    // URLs de entorno
    private String urlEntornoDesarrollo;
    private String urlEntornoIntegracion;
    private String urlEntornoPreproduccion;
    private String urlEntornoProduccion;
    
    // Responsables
    private String responsableProyecto;
    private String responsableTecnico;
    private String horaDaily;
    
    // Listas
    private List<String> ip;
    private List<Task> tareas;
    private List<String> herramientas;
    private List<String> jenkinsNodes;
    private List<DockerImage> dockerImages;
    private List<String> pipelines;
    private List<String> repositorios;
    private List<DatabaseInfo> bbdd;
    private List<OpenShiftInfo> openshift;
    private List<String> usuarios;
    
    // Información adicional
    private String notasGenerales;
    private String entornoNotas;
    
    // Equipos
    private List<MinsaitMember> equipoMinsait;
    private List<DevMachine> devMachines;
    
    // Herramientas MIND
    @org.springframework.data.mongodb.core.mapping.Field("herramientasMind")
    private HerramientasMind herramientasMind;
    
    // Información de auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted = false;
    private Boolean visible = true;
    
    // ====== CLASES ANIDADAS ======
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Task {
        private String titulo;
        private String prioridad;
        private String estado;
        private Integer completadoPercent;
        private String notas;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseInfo {
        private String tipo;
        private String uri;
        private String usuario;
        private String password;
        private String notas;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DockerImage {
        private String image;
        private String purpose;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenShiftInfo {
        private String usuario;
        private String url;
        private String token;
        private String kibana;
        private String grafana;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MinsaitMember {
        private String nombre;
        private String rol;
        private String email;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DevMachine {
        private String identifier;
        private String ip;
        private String user;
        private String password;
        private String ram;
        private String cpu;
        private String disk;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HerramientasMind {
        private List<RepositorioSimple> codeRepos;
        private List<RepositorioSimple> artifactRepos;
        private List<RepositorioSimple> jenkins;
        private List<SonarConfig> sonarList;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepositorioSimple {
        private String name;
        private String url;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SonarConfig {
        private String prefix;
        private String url;
        private String tokenUser;
        private String tokenValue;
    }
}