package com.janushub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.janushub.model.AppNotification;
import com.janushub.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio central de notificaciones push vía WebSocket.
 * Soporta broadcast global y broadcast filtrado por roles.
 * Las notificaciones se persisten en MongoDB (TTL 7 días) y se envían
 * a los usuarios que se conecten después del evento.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final NotificationRepository notificationRepository;

    // wsSessionId -> WebSocketSession
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // wsSessionId -> Set de roles del usuario (ej. ["ADMIN","DEVOPS"])
    private final ConcurrentHashMap<String, Set<String>> sessionRoles = new ConcurrentHashMap<>();

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Registra una sesión WS sin roles conocidos y envía notificaciones pendientes genéricas.
     */
    public void register(WebSocketSession session) {
        sessions.put(session.getId(), session);
        sendPendingNotifications(session, Collections.emptyList());
        log.debug("Notification WS registered (sin roles): {}", session.getId());
    }

    /**
     * Registra una sesión WS con los roles del usuario autenticado y envía notificaciones pendientes.
     */
    public void register(WebSocketSession session, Set<String> roles) {
        sessions.put(session.getId(), session);
        sessionRoles.put(session.getId(), roles);
        sendPendingNotifications(session, List.copyOf(roles));
        log.debug("Notification WS registered user roles={} session={}", roles, session.getId());
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
        sessionRoles.remove(sessionId);
        log.debug("Notification WS unregistered: {}", sessionId);
    }

    /**
     * Emite a TODOS los clientes conectados y persiste la notificación en DB.
     */
    public void broadcast(String type, String title, String body, String link) {
        persist(type, title, body, link, Collections.emptyList());
        sendToSessions(sessions.values(), type, title, body, link);
    }

    /**
     * Emite solo a sesiones cuyo usuario tenga al menos uno de los roles indicados y persiste en DB.
     * Las sesiones sin roles registrados NO reciben el mensaje.
     */
    public void broadcastToRoles(List<String> targetRoles, String type, String title, String body, String link) {
        persist(type, title, body, link, targetRoles);
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

    /** Persiste la notificación en MongoDB. */
    private void persist(String type, String title, String body, String link, List<String> targetRoles) {
        try {
            AppNotification n = new AppNotification();
            n.setType(type);
            n.setTitle(title);
            n.setBody(body != null ? body : "");
            n.setLink(link != null ? link : "");
            n.setTargetRoles(targetRoles);
            n.setTimestamp(Instant.now());
            notificationRepository.save(n);
        } catch (Exception e) {
            log.warn("No se pudo persistir notificación tipo {}: {}", type, e.getMessage());
        }
    }

    /**
     * Envía al cliente recién conectado las notificaciones de los últimos 7 días
     * que le correspondan según sus roles.
     */
    private void sendPendingNotifications(WebSocketSession session, List<String> roles) {
        try {
            Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
            List<AppNotification> pending = notificationRepository.findRecentForRoles(roles, since);
            // Enviar de más antigua a más reciente para que aparezcan en orden
            for (int i = pending.size() - 1; i >= 0; i--) {
                AppNotification n = pending.get(i);
                sendSingleNotification(session, n.getType(), n.getTitle(), n.getBody(), n.getLink(),
                        n.getTimestamp() != null ? n.getTimestamp().toString() : Instant.now().toString());
            }
        } catch (Exception e) {
            log.warn("Error enviando notificaciones pendientes a sesión {}: {}", session.getId(), e.getMessage());
        }
    }

    private void sendSingleNotification(WebSocketSession session, String type, String title, String body, String link, String timestamp) {
        if (!session.isOpen()) return;
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("title", title != null ? title : "");
        payload.put("body", body != null ? body : "");
        payload.put("link", link != null ? link : "");
        payload.put("timestamp", timestamp);
        try {
            String json = mapper.writeValueAsString(payload);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            log.warn("No se pudo enviar notificación pendiente a sesión {}: {}", session.getId(), e.getMessage());
        }
    }

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

    /** Elimina todas las notificaciones persistidas en MongoDB. */
    public void deleteAll() {
        try {
            notificationRepository.deleteAll();
            log.info("Todas las notificaciones eliminadas de MongoDB");
        } catch (Exception e) {
            log.warn("Error al eliminar notificaciones: {}", e.getMessage());
        }
    }
}

