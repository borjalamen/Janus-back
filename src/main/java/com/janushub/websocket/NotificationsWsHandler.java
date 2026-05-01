package com.janushub.websocket;

import com.janushub.repository.UserRepository;
import com.janushub.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Handler WebSocket del endpoint /ws/notifications?username=xxx.
 * Al conectar lee el query param 'username', obtiene sus roles de MongoDB
 * y los registra en NotificationService para poder hacer broadcast por rol.
 */
@Component
public class NotificationsWsHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationsWsHandler.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = extractUsername(session);
        if (username != null && !username.isBlank()) {
            userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    Set<String> roles = user.getRoles() != null
                            ? new HashSet<>(user.getRoles())
                            : Collections.emptySet();
                    notificationService.register(session, roles);
                    log.debug("WS notif conectado: user={} roles={}", username, roles);
                },
                () -> {
                    // usuario no encontrado — registrar sin roles (no recibirá notificaciones de rol)
                    notificationService.register(session);
                    log.warn("WS notif: username '{}' no encontrado en BD", username);
                }
            );
        } else {
            notificationService.register(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        notificationService.unregister(session.getId());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private String extractUsername(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String query = uri.getQuery(); // "username=fsgil"
        if (query == null) return null;
        return Arrays.stream(query.split("&"))
                .filter(p -> p.startsWith("username="))
                .map(p -> p.substring("username=".length()))
                .findFirst()
                .orElse(null);
    }
}
