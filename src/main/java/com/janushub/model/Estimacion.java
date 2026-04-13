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
@Document(collection = "estimaciones")
public class Estimacion {

    @Id
    private String id;

    private String estimationName;
    private String projectCode;
    private String projectName;
    private String requester;
    private String requesterEmail;
    private String notes;

    private List<EstimationComment> comments = new ArrayList<>();
    private List<String> weeks = new ArrayList<>();
    private List<EstimationTask> tasks = new ArrayList<>();

    private String createdAt;
    private Boolean visible = true;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstimationTask {
        private String id;
        private String title;
        private List<Double> estimates = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EstimationComment {
        private String id;
        private String author;
        private String text;
        private String createdAt;
    }
}
