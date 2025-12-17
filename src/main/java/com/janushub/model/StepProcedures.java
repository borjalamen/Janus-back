package com.janushub.model;

import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

/**
 * Representa un paso dentro de un procedimiento.
 */
public class StepProcedures {
    
    @Field("id")
    private String id;
    
    @Field("titulo")
    private String titulo;
    
    @Field("descripcion")
    private String descripcion;
    
    @Field("responsable")
    private String responsable;
    
    @Field("metodo")
    private String metodo;
    
    @Field("orden")
    private Integer orden;
    
    @Field("tags")
    private List<String> tags;

    // Constructor vacío
    public StepProcedures() {}

    // Constructor completo
    public StepProcedures(String id, String titulo, String descripcion, String responsable, String metodo, Integer orden, List<String> tags) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.responsable = responsable;
        this.metodo = metodo;
        this.orden = orden;
        this.tags = tags;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
