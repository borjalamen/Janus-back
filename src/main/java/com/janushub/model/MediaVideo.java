package com.janushub.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "media_videos")
public class MediaVideo {

    @Id
    private String id;

    // ── Campos originales (compatibilidad hacia atrás) ─────────────────────
    private String title;         // legacy display name
    private String file;          // legacy: ruta asset "assets/multimedia/..."
    private String thumbnail;     // URL/ruta de la miniatura
    private String duration;      // "MM:SS"

    // ── Campos nuevos (volumen) ────────────────────────────────────────────
    private String fileName;      // nombre real del fichero en disco
    private String displayName;   // nombre de presentación al usuario
    private String description;
    private String category;
    private Long   sizeBytes;
    private String mimeType;
    private String filePath;      // ruta relativa dentro del volumenDocumentos
    private String uploadedBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
