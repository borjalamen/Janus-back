package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

/**
 * Representa un paso dentro de un procedimiento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
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
}
