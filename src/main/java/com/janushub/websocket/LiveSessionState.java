package com.janushub.websocket;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LiveSessionState {

    private String id;
    private String ownerId;
    private String ownerName;

    // Estimation metadata (from the form)
    private String estimationName;
    private String projectCode;
    private String projectName;
    private String requester;
    private String requesterEmail;
    private String notes;

    private List<ParticipantState> participants = new ArrayList<>();

    private String currentTask;
    // LOBBY | VOTING | REVEALED | FINISHED
    private String phase = "LOBBY";
    // epoch ms when voting started (for timer sync)
    private Long votingStart;

    private List<AcceptedTask> acceptedTasks = new ArrayList<>();

    @Data
    public static class ParticipantState {
        private String id;
        private String name;
        private Double vote; // null until voted

        public ParticipantState() {}

        public ParticipantState(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Data
    public static class AcceptedTask {
        private String task;
        private double result;

        public AcceptedTask() {}

        public AcceptedTask(String task, double result) {
            this.task = task;
            this.result = result;
        }
    }
}
