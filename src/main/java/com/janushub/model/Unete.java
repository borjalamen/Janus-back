package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entidad que representa una petición de "únete" almacenada en MongoDB.
 * Cada solicitud queda registrada con estado PENDIENTE hasta que un
 * administrador la apruebe o rechace.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "join_requests")
public class Unete {

    @Id
    private String id;

    // Datos del solicitante (enviados desde el formulario "únete")
    private String fullName;
    private String email;
    private String role;          // invitado, consultor, devops, admin
    private String projectCode;
    private String projectName;
    private String comments;

    // Metadatos de gestión
    private String estado;        // PENDIENTE, APROBADA, RECHAZADA
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Comentario del administrador al aprobar/rechazar (opcional)
    private String adminComment;
}
