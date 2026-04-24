package com.janushub.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;

@Component
public class LiveEstimationWsHandler extends TextWebSocketHandler {

    private static final int VOTE_TIMEOUT_SECONDS = 60;

    private final ObjectMapper mapper = new ObjectMapper();

    // sessionId -> LiveSessionState
    private final ConcurrentHashMap<String, LiveSessionState> sessions = new ConcurrentHashMap<>();
    // wsSessionId -> sessionId
    private final ConcurrentHashMap<String, String> wsToSession = new ConcurrentHashMap<>();
    // wsSessionId -> userId
    private final ConcurrentHashMap<String, String> wsToUser = new ConcurrentHashMap<>();
    // wsSessionId -> WebSocketSession
    private final ConcurrentHashMap<String, WebSocketSession> wsSessions = new ConcurrentHashMap<>();
    // sessionId -> (userId -> wsSessionId)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> sessionUserWs = new ConcurrentHashMap<>();
    // sessionId -> ScheduledFuture (auto-reveal timer)
    private final ConcurrentHashMap<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    // ── WebSocket lifecycle ────────────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        wsSessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root;
        try {
            root = mapper.readTree(message.getPayload());
        } catch (Exception e) {
            sendError(session, "Invalid JSON");
            return;
        }

        String type = root.path("type").asText();
        JsonNode payload = root.path("payload");

        switch (type) {
            case "CREATE"   -> handleCreate(session, payload);
            case "JOIN"     -> handleJoin(session, payload);
            case "SET_TASK" -> handleSetTask(session, payload);
            case "VOTE"     -> handleVote(session, payload);
            case "REVEAL"   -> handleReveal(session, payload);
            case "ACCEPT"   -> handleAccept(session, payload);
            case "FINISH"   -> handleFinish(session, payload);
            case "LEAVE"    -> handleLeave(session, payload);
            default         -> sendError(session, "Unknown message type: " + type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String wsId = session.getId();
        String sessionId = wsToSession.get(wsId);
        String userId    = wsToUser.get(wsId);

        wsSessions.remove(wsId);

        if (sessionId != null && userId != null) {
            removeParticipant(sessionId, userId, wsId);
            try { broadcastSession(sessionId); } catch (IOException ignored) {}
        }
    }

    // ── Message handlers ──────────────────────────────────────────────────────

    private void handleCreate(WebSocketSession wsSession, JsonNode payload) throws IOException {
        String userId         = payload.path("userId").asText();
        String userName       = payload.path("userName").asText("Usuario");
        String estimationName = payload.path("estimationName").asText();
        String projectCode    = payload.path("projectCode").asText();
        String projectName    = payload.path("projectName").asText();
        String requester      = payload.path("requester").asText();
        String requesterEmail = payload.path("requesterEmail").asText();
        String notes          = payload.path("notes").asText();

        // Generate a short, uppercase session ID
        String sessionId = UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();

        LiveSessionState state = new LiveSessionState();
        state.setId(sessionId);
        state.setOwnerId(userId);
        state.setOwnerName(userName);
        state.setEstimationName(estimationName);
        state.setProjectCode(projectCode);
        state.setProjectName(projectName);
        state.setRequester(requester);
        state.setRequesterEmail(requesterEmail);
        state.setNotes(notes);
        state.getParticipants().add(new LiveSessionState.ParticipantState(userId, userName));

        sessions.put(sessionId, state);
        register(wsSession.getId(), sessionId, userId);

        broadcastSession(sessionId);
    }

    private void handleJoin(WebSocketSession wsSession, JsonNode payload) throws IOException {
        String sessionId = payload.path("sessionId").asText("").toUpperCase().trim();
        String userId    = payload.path("userId").asText();
        String userName  = payload.path("userName").asText("Usuario");

        LiveSessionState state = sessions.get(sessionId);
        if (state == null) {
            sendError(wsSession, "SESSION_NOT_FOUND:" + sessionId);
            return;
        }

        // If this user already has a ws entry, update it
        String existingWs = sessionUserWs
                .computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .get(userId);
        if (existingWs != null && !existingWs.equals(wsSession.getId())) {
            wsToSession.remove(existingWs);
            wsToUser.remove(existingWs);
        }

        register(wsSession.getId(), sessionId, userId);

        boolean exists = state.getParticipants().stream().anyMatch(p -> p.getId().equals(userId));
        if (!exists) {
            state.getParticipants().add(new LiveSessionState.ParticipantState(userId, userName));
        }

        broadcastSession(sessionId);
    }

    private void handleSetTask(WebSocketSession wsSession, JsonNode payload) throws IOException {
        String sessionId = resolveSessionId(payload);
        String userId    = payload.path("userId").asText();
        String task      = payload.path("task").asText().trim();

        LiveSessionState state = sessions.get(sessionId);
        if (state == null || !state.getOwnerId().equals(userId)) {
            sendError(wsSession, "Not authorized or session not found");
            return;
        }
        if (task.isEmpty()) {
            sendError(wsSession, "Task description cannot be empty");
            return;
        }

        state.setCurrentTask(task);
        state.setPhase("VOTING");
        state.setVotingStart(System.currentTimeMillis());
        state.getParticipants().forEach(p -> p.setVote(null));

        cancelTimer(sessionId);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try { autoReveal(sessionId); } catch (Exception ignored) {}
        }, VOTE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        timers.put(sessionId, future);

        broadcastSession(sessionId);
    }

    private void handleVote(WebSocketSession wsSession, JsonNode payload) throws IOException {
        String sessionId = resolveSessionId(payload);
        String userId    = payload.path("userId").asText();
        double vote      = payload.path("vote").asDouble(0);

        LiveSessionState state = sessions.get(sessionId);
        if (state == null || !"VOTING".equals(state.getPhase())) return;

        state.getParticipants().stream()
                .filter(p -> p.getId().equals(userId))
                .findFirst()
                .ifPresent(p -> p.setVote(vote));

        // Auto-reveal when everyone has voted
        boolean allVoted = state.getParticipants().stream().allMatch(p -> p.getVote() != null);
        if (allVoted) {
            cancelTimer(sessionId);
            state.setPhase("REVEALED");
        }

        broadcastSession(sessionId);
    }

    private void handleReveal(WebSocketSession wsSession, JsonNode payload) throws IOException {
        String sessionId = resolveSessionId(payload);
        String userId    = payload.path("userId").asText();

        LiveSessionState state = sessions.get(sessionId);
        if (state == null || !state.getOwnerId().equals(userId)) return;

        cancelTimer(sessionId);
        state.setPhase("REVEALED");
        broadcastSession(sessionId);
    }

    private void handleAccept(WebSocketSession wsSession, JsonNode payload) throws IOException {
        String sessionId = resolveSessionId(payload);
        String userId    = payload.path("userId").asText();
        double result    = payload.path("result").asDouble(0);

        LiveSessionState state = sessions.get(sessionId);
        if (state == null || !state.getOwnerId().equals(userId)) {
            sendError(wsSession, "Not authorized");
            return;
        }

        state.getAcceptedTasks().add(
                new LiveSessionState.AcceptedTask(state.getCurrentTask(), result));
        state.setCurrentTask(null);
        state.setVotingStart(null);
        state.setPhase("LOBBY");
        state.getParticipants().forEach(p -> p.setVote(null));

        broadcastSession(sessionId);
    }

    private void handleFinish(WebSocketSession wsSession, JsonNode payload) throws IOException {
        String sessionId = resolveSessionId(payload);
        String userId    = payload.path("userId").asText();

        LiveSessionState state = sessions.get(sessionId);
        if (state == null || !state.getOwnerId().equals(userId)) {
            sendError(wsSession, "Not authorized");
            return;
        }

        cancelTimer(sessionId);
        state.setPhase("FINISHED");
        broadcastSession(sessionId);

        // Clean up after 10 minutes
        scheduler.schedule(() -> cleanSession(sessionId), 10, TimeUnit.MINUTES);
    }

    private void handleLeave(WebSocketSession wsSession, JsonNode payload) throws IOException {
        String sessionId = resolveSessionId(payload);
        String userId    = payload.path("userId").asText();

        removeParticipant(sessionId, userId, wsSession.getId());

        LiveSessionState state = sessions.get(sessionId);
        if (state != null) broadcastSession(sessionId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void autoReveal(String sessionId) throws IOException {
        LiveSessionState state = sessions.get(sessionId);
        if (state == null || !"VOTING".equals(state.getPhase())) return;
        state.setPhase("REVEALED");
        broadcastSession(sessionId);
    }

    private void register(String wsId, String sessionId, String userId) {
        wsToSession.put(wsId, sessionId);
        wsToUser.put(wsId, userId);
        sessionUserWs.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>()).put(userId, wsId);
    }

    private void removeParticipant(String sessionId, String userId, String wsId) {
        wsToSession.remove(wsId);
        wsToUser.remove(wsId);
        ConcurrentHashMap<String, String> uwMap = sessionUserWs.get(sessionId);
        if (uwMap != null) uwMap.remove(userId);

        LiveSessionState state = sessions.get(sessionId);
        if (state != null) {
            state.getParticipants().removeIf(p -> p.getId().equals(userId));
        }
    }

    private void cancelTimer(String sessionId) {
        ScheduledFuture<?> f = timers.remove(sessionId);
        if (f != null) f.cancel(false);
    }

    private void cleanSession(String sessionId) {
        sessions.remove(sessionId);
        sessionUserWs.remove(sessionId);
        cancelTimer(sessionId);
    }

    private String resolveSessionId(JsonNode payload) {
        return payload.path("sessionId").asText("").toUpperCase().trim();
    }

    private void broadcastSession(String sessionId) throws IOException {
        LiveSessionState state = sessions.get(sessionId);
        if (state == null) return;

        ObjectNode msg = mapper.createObjectNode();
        msg.put("type", "SESSION_UPDATE");
        msg.set("payload", mapper.valueToTree(state));
        String json = mapper.writeValueAsString(msg);

        ConcurrentHashMap<String, String> uwMap = sessionUserWs.get(sessionId);
        if (uwMap == null) return;

        for (String wsId : uwMap.values()) {
            WebSocketSession ws = wsSessions.get(wsId);
            if (ws != null && ws.isOpen()) {
                try {
                    synchronized (ws) {
                        ws.sendMessage(new TextMessage(json));
                    }
                } catch (IOException ignored) {}
            }
        }
    }

    private void sendError(WebSocketSession session, String message) {
        try {
            ObjectNode msg = mapper.createObjectNode();
            msg.put("type", "ERROR");
            ObjectNode p = mapper.createObjectNode();
            p.put("message", message);
            msg.set("payload", p);
            synchronized (session) {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
            }
        } catch (IOException ignored) {}
    }
}
