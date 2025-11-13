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
@Document(collection = "projects")
public class Project {
    @Id
    private String id;
    private String code;
    private String name;
    private String departamentOrganisme;
    private String gestorResponsableSolucio;
    private String responsableProjecte;
    private String equipDesenvolupament;
    private String equipProjectesInfra;
    private String equipProves;
    private String equipAdminExplotacioXarxes;
    private String oficinaSeguretat;
    private String equipQualitat;
    private String equipAdminOperacions;
    private String equipAdminExplotacioSistemes;
    private String gestorIntegracioSolucions;
    private LocalDateTime createdAt;
}
