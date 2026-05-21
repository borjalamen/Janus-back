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
@Document(collection = "obsolescencia")
public class ObsolescenciaEntry {

    @Id
    private String id;

    private String projectId;
    private String projectCode;
    private String projectName;
    private String techName;
    private String techVersion;
    private String techComment;
    private LocalDateTime markedAt;
}
