package com.janushub.model;

<<<<<<< HEAD
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
=======
>>>>>>> CRUD-Procedimientos-Formacion
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Representa el documento "Procedure" en la colección "procedures" de MongoDB.
 */
@Document(collection = "procedures")
<<<<<<< HEAD
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Procedure {

    @Id
    private String id;

    @Field("titulo")
    private String titulo;

    @Field("descripcion")
    private String descripcion;

    @Field("departamento")
    private String departamento;

    @Field("tags")
    private List<String> tags;

    @Field("steps")
    private List<StepProcedures> steps;

    // Campos de auditoría
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Inicialización por defecto a través de inicializadores de campo
    @Builder.Default
    private boolean isDeleted = false;

    @Builder.Default
    private boolean isVisible = true;
=======
public class Procedure {

    // El campo @Id es el que Spring Data usa para mapear el _id de MongoDB.
    @Id
    private String id;
    
    @Field("titulo")
    private String titulo;
    
    @Field("descripcion")
    private String descripcion;
    
    @Field("departamento")
    private String departamento;
    
    @Field("tags")
    private List<String> tags;
    
    @Field("steps")
    private List<StepProcedures> steps;
    
    // Campo para borrado lógico (Soft Delete)
    private boolean isDeleted; 
    
    // Campo para mostrar u ocultar el procedimiento (Activo/Inactivo)
    private boolean isVisible; 
    
    // Campos de auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor vacío
    public Procedure() {
        // Inicialización por defecto al crear el objeto en Java
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isVisible = true;
        this.isDeleted = false;
    }

    // --- Getters y Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<StepProcedures> getSteps() { return steps; }
    public void setSteps(List<StepProcedures> steps) { this.steps = steps; }

    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { isVisible = visible; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; } 

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
>>>>>>> CRUD-Procedimientos-Formacion
}