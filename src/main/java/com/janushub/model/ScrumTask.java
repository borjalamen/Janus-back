package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "scrum_tasks")
public class ScrumTask {

    @Id
    private String id;

    private String title;
    private String estimate;
    private Integer priority;
    private String description;
    private String assignee;
    private String color;
    private String status;

    private List<ScrumComment> comments = new ArrayList<>();

    private String createdAt;
    private String updatedAt;
    private Boolean visible = true;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScrumComment {
        private String id;
        private String author;
        private String text;
        private String createdAt;
    }
}
