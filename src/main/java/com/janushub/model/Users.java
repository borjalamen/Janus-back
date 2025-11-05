package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime; // Necesitas java.time para las fechas
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class Users {
    @Id
    private String id;
    private String username;
    private String password; // Almacena el hash de la contraseña (Ej. $argon2id$...)
    private List<String> roles; // Ej. ["ADMIN", "DEVOPS"]
    private String fullName;
    private String email;
    private String phone;
    private String status; // Ej. "ACTIVE"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
