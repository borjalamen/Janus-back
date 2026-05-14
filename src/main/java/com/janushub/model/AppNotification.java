package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * Notificación persistida en MongoDB.
 * TTL: se eliminan automáticamente a los 7 días via índice de MongoDB.
 * targetRoles vacío o null significa que va dirigida a TODOS los roles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class AppNotification {

    @Id
    private String id;

    private String type;
    private String title;
    private String body;
    private String link;

    /** Roles destinatarios. Vacío/null = todos. */
    private List<String> targetRoles;

    /** TTL: MongoDB eliminará el documento 7 días después de este campo. */
    @Indexed(expireAfter = "7d")
    private Instant timestamp;
}
