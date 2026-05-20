package com.janushub.controller;

import com.janushub.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoint de utilidad para probar el sistema de notificaciones push.
 * Solo accesible internamente (no expuesto en producción con auth).
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Envía una notificación de prueba a los roles ADMIN y DEVOPS.
     * GET /api/notifications/test
     * Uso: abrir en el navegador o llamar desde el frontend.
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testNotification() {
        notificationService.broadcastToRoles(
                List.of("ADMIN", "DEVOPS"),
                "TEST",
                "🔔 Notificación de prueba",
                "El sistema de notificaciones push está funcionando correctamente",
                "/home"
        );
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Test enviado a ADMIN y DEVOPS"));
    }

    /**
     * Envía una notificación a TODOS los conectados (independientemente del rol).
     * GET /api/notifications/test-all
     */
    @GetMapping("/test-all")
    public ResponseEntity<Map<String, String>> testNotificationAll() {
        notificationService.broadcast(
                "TEST",
                "🔔 Notificación global de prueba",
                "El sistema de notificaciones push está funcionando correctamente",
                "/home"
        );
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Test enviado a todos los conectados"));
    }
    /**
     * Elimina todas las notificaciones persistidas en MongoDB.
     * DELETE /api/notifications
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteAllNotifications() {
        notificationService.deleteAll();
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Notificaciones eliminadas"));
    }}
