package com.janushub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio central de notificaciones push vía WebSocket.
 * Soporta broadcast global y broadcast filtrado por roles.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final ObjectMapper mapper = new ObjectMapper();

    // wsSessionId -> WebSocketSession
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // wsSessionId -> Set de roles del usuario (ej. ["ADMIN","DEVOPS"])
    private final ConcurrentHashMap<String, Set<String>> sessionRoles = new ConcurrentHashMap<>();

    /**
     * Registra una sesión WS sin roles conocidos (broadcast genérico).
     */
    public void register(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.debug("Notification WS registered (sin roles): {}", session.getId());
    }

    /**
     * Registra una sesión WS con los roles del usuario autenticado.
     */
    public void register(WebSocketSession session, Set<String> roles) {
        sessions.put(session.getId(), session);
        sessionRoles.put(session.getId(), roles);
        log.debug("Notification WS registered user roles={} session={}", roles, session.getId());
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
        sessionRoles.remove(sessionId);
        log.debug("Notification WS unregistered: {}", sessionId);
    }

    /**
     * Emite a TODOS los clientes conectados.
     */
    public void broadcast(String type, String title, String body, String link) {
        sendToSessions(sessions.values(), type, title, body, link);
    }

    /**
     * Emite solo a sesiones cuyo usuario tenga al menos uno de los roles indicados.
     * Las sesiones sin roles registrados NO reciben el mensaje.
     *
     * @param targetRoles Roles que deben tener los destinatarios (ej. ["ADMIN", "DEVOPS"])
     */
    public void broadcastToRoles(List<String> targetRoles, String type, String title, String body, String link) {
        List<WebSocketSession> targets = sessions.entrySet().stream()
                .filter(e -> {
                    Set<String> roles = sessionRoles.get(e.getKey());
                    if (roles == null || roles.isEmpty()) return false;
                    return targetRoles.stream().anyMatch(roles::contains);
                })
                .map(Map.Entry::getValue)
                .toList();
        sendToSessions(targets, type, title, body, link);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private void sendToSessions(Iterable<WebSocketSession> targets, String type, String title, String body, String link) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("title", title);
        payload.put("body", body != null ? body : "");
        payload.put("link", link != null ? link : "");
        payload.put("timestamp", Instant.now().toString());

        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Error serializando notificación tipo {}: {}", type, e.getMessage());
            return;
        }

        TextMessage msg = new TextMessage(json);
        int sent = 0;
        for (WebSocketSession session : targets) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(msg);
                    }
                    sent++;
                } catch (IOException e) {
                    log.warn("No se pudo enviar notificación a sesión {}: {}", session.getId(), e.getMessage());
                }
            }
        }
        log.debug("Notificación '{}' enviada a {} sesión(es)", type, sent);
    }
}
