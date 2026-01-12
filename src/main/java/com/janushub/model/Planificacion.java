package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "planning")
public class Planificacion {

    @Id
    private String id;

    private String env;
    private String project;
    private String date;
    private String startTime;
    private String endTime;
    private String devOps;
    private String notes;
    private String jiraUrl;
    private String responsable;
    private Integer periodDays;
    private String eventType;
}
