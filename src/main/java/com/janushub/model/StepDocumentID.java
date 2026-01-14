package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representa un Step com a document independent
 * a la col·lecció "steps".
 */
@Document(collection = "steps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)

public class StepDocumentID {

     @Id
    private String id; // _id MongoDB (únic)


     @Field("stepId")
    @Indexed(unique = true)
    private String stepId; // ID funcional únic del step

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

    // Camps d'auditoria
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    @Builder.Default
    private boolean isDeleted = false;
    @Builder.Default
    private boolean isVisible = true;
}