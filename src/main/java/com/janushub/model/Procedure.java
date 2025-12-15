package com.janushub.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Representa el documento "Procedure" en la colección "procedures" de MongoDB.
 */
@Document(collection = "procedures")
public class Procedure {

    // El campo @Id es el que Spring Data usa para mapear el _id de MongoDB.
    @Id
    private String id;
    
    private String title;
    private String description;
    private String department;
    private List<String> steps;
    
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
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }

    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { isVisible = visible; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; } 

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}