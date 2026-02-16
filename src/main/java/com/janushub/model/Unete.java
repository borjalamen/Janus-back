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
@Document(collection = "join_requests")
public class Unete {

    @Id
    private String id;

    private String fullName;
    private String email;
    private String role;
    private String projectCode;
    private String projectName;
    private String comments;

    private String estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String adminComment;
}