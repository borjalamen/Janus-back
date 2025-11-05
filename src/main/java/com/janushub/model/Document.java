package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
public class Document {
    @Id
    private String id;
    private String title;
    private String description;
    private String fileId;
    private String ruta;
    private String version;
    private String autor;
    private String validadoPor;
    private LocalDateTime fechaEntrega;
    private String plataforma;
    private LocalDateTime createdAt;
}
