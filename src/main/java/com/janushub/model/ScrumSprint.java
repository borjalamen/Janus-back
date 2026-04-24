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
@Document(collection = "scrum_sprints")
public class ScrumSprint {

    @Id
    private String id;

    /** Human-readable identifier, e.g. SPRINT-20260424-AB12 */
    private String sprintKey;

    private String startDate;
    private String endDate;
    private Boolean active = true;

    private Integer totalTasks;
    private Integer doneTasks;
    private Double totalHours;
    private Double doneHours;

    private List<SprintSnapshot> snapshots = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SprintSnapshot {
        private String date;
        private Integer remainingTasks;
        private Integer doneTasks;
        private Integer totalTasks;
        private Double remainingHours;
        private Double doneHours;
        private Double totalHours;
    }
}
