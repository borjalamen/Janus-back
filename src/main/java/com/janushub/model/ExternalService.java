package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "external_services")
public class ExternalService {

    @Id
    private String id;

    /** Nombre descriptivo del servicio externo */
    private String name;

    /** Código o identificador corto */
    private String code;

    /** Descripción del servicio */
    private String description;

    /** URL principal del servicio */
    private String url;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
