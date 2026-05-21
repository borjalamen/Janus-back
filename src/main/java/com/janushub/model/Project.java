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
    private ResponsableInfo responsableProyecto;
    private ResponsableInfo responsableTecnico;
    private String horaDaily;
    private List<Daily> dailies;
    
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

    // Herramientas de Monitorización
    private MonitoringTools monitoringTools;

    // Conectividades del proyecto
    private List<ConnectivityEntry> connectivities;
    
    // Documentos agregados durante creación
    private List<ProjectDocument> documents;

    // Metadatos extra clave-valor
    private List<ExtraMetadata> extras;

    // Tecnologías utilizadas en el proyecto
    private List<TechnologyEntry> technologies;
    
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
    public static class ResponsableInfo {
        private String nombre;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Daily {
        private String hora;
        private List<String> dias;
        private String notas;
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
        private Boolean openshiftEnabled;
        private List<OpenShiftDev> openshifts;
        private Boolean dbEnabled;
        private List<DBConfig> dbs;
        private Boolean otherToolEnabled;
        private List<OtherTool> otherTools;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenShiftDev {
        private String identifier;
        private String user;
        private String password;
        private String ram;
        private String cpu;
        private String disk;
        private List<VolumeConfig> volumes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolumeConfig {
        private String name;
        private String capacity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DBConfig {
        private String identifier;
        private String engine;
        private String instanceName;
        private String host;
        private String port;
        private String sid;
        private String user;
        private String password;
        private String description;
        private String properties;
        private String contactPerson;
        private String contactMail;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherTool {
        private String identifier;
        private String name;
        private String path;
        private Boolean running;
        private String contactPerson;
        private String contactMail;
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
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonitoringEnv {
        private String grafanaUrl;
        private String grafanaUser;
        private String grafanaPassword;
        private String kibanaUrl;
        private String kibanaUser;
        private String kibanaPassword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonitoringTools {
        private MonitoringEnv dev;
        private MonitoringEnv pre;
        private MonitoringEnv pro;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectDocument {
        private String nombre;
        private String descripcion;
        private String tipo;
        private String path;  // Ruta del archivo en volumenDocumentos
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectivityEntry {
        private String id;          // UUID para identificar la entrada
        private String role;        // PRODUCER | CONSUMER | MIXED
        private String type;        // INTERNAL | EXTERNAL | OTHER

        // Para INTERNAL (proyecto JanusHub)
        private String internalProjectId;
        private String internalProjectCode;
        private String internalProjectName;

        // Para EXTERNAL (servicio externo)
        private String externalServiceId;
        private String externalServiceName;

        // Para OTHER
        private String otherName;
        private String otherCode;
        private String otherNotes;

        // Común
        private List<String> environments;  // DEV, INT, PRE, PRO
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtraMetadata {
        private String key;
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnologyEntry {
        private String name;
        private String version;
        private String comment;
    }
}