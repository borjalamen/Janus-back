package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "peticiones_tareas")
public class PeticionTarea {
  @Id
    private String id;

    private String requesterName;
    private String requesterEmail;
    private String projectName;
    private String projectCode;
    private String jiraTask;
    private String devopsAssignee;
    private LocalDateTime deadline;
    private String comments;
    private List<String> attachments;

    private String estado; // PENDIENTE | APROBADA | RECHAZADA
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String adminComment;
} 