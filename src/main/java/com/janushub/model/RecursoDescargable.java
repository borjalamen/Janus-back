package com.janushub.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "recursos_descargables")
public class RecursoDescargable {

    @Id
    private String id;

    /** Nombre original del fichero tal como se subió */
    private String fileName;

    /** Nombre legible para mostrar en la UI */
    private String displayName;

    /** Descripción o nota libre */
    private String description;

    /** Categoría/etiqueta libre (ej: "RRHH", "DevOps", "Seguretat"...) */
    private String category;

    /** Tamaño en bytes */
    private Long sizeBytes;

    /** Tipo MIME */
    private String mimeType;

    /** Ruta relativa dentro del volumen: recursos-generales/<id>/<fileName> */
    private String filePath;

    /** Usuario que subió el fichero */
    private String uploadedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
