package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Representa el documento "Procedure" en la colección "procedures" de MongoDB.
 */
@Document(collection = "procedures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Procedure {

    @Id
    private String id;

    @Field("procedureId")   
    private String procedureId;

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

    
}