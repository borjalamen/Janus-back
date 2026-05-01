package com.janushub.service;

import com.janushub.model.PeticionTarea;
import com.janushub.model.Unete;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.from:no-reply@janushub.local}")
    private String mailFrom;

    public boolean sendJoinRequestSubmitted(Unete request) {
        if (request == null) {
            return false;
        }

        String subject = "[JanusHub] Solicitud recibida";
        String body = String.join("\n",
                "Hola " + safe(request.getFullName()) + ",",
                "",
                "Hemos recibido tu solicitud de alta en JanusHub.",
                "",
                "Resumen:",
                "- Rol sugerido: " + safeOrDefault(request.getRole(), "No informado"),
                "- Proyecto: " + safeOrDefault(request.getProjectName(), "No informado"),
                "- Código proyecto: " + safeOrDefault(request.getProjectCode(), "No informado"),
                "- Estado: " + safeOrDefault(request.getEstado(), "PENDIENTE"),
                "",
                "Te avisaremos por este mismo correo cuando tu solicitud sea revisada.",
                "",
                "Equipo JanusHub"
        );

        return sendEmail(request.getEmail(), subject, body);
    }

    public boolean sendJoinRequestApproved(Unete request, String username, String password) {
        if (request == null) {
            return false;
        }

        String subject = "[JanusHub] Solicitud aprobada";
        String body = String.join("\n",
                "Hola " + safe(request.getFullName()) + ",",
                "",
                "Tu solicitud en JanusHub ha sido APROBADA.",
                "",
                "Credenciales de acceso:",
                "- Usuario: " + safe(username),
                "- Contraseña temporal: " + safe(password),
                "",
                "Te recomendamos cambiar la contraseña tras el primer inicio de sesión.",
                "",
                "Equipo JanusHub"
        );

        return sendEmail(request.getEmail(), subject, body);
    }

    public boolean sendJoinRequestRejected(Unete request) {
        if (request == null) {
            return false;
        }

        String subject = "[JanusHub] Solicitud rechazada";
        String body = String.join("\n",
                "Hola " + safe(request.getFullName()) + ",",
                "",
                "Tu solicitud en JanusHub ha sido RECHAZADA.",
                "",
                "Comentario del administrador:",
                safeOrDefault(request.getAdminComment(), "Sin comentarios adicionales."),
                "",
                "Si lo necesitas, puedes volver a enviar una solicitud actualizada.",
                "",
                "Equipo JanusHub"
        );

        return sendEmail(request.getEmail(), subject, body);
    }

    public boolean sendTaskRequestSubmitted(PeticionTarea task, boolean isResend) {
        if (task == null) {
            return false;
        }

        String prefix = isResend ? "[REENVIO] " : "";
        String subject = prefix + "[JanusHub] Petición DevOps registrada";
        String body = String.join("\n",
                "Hola " + safe(task.getRequesterName()) + ",",
                "",
                "Tu petición DevOps se ha registrado correctamente en JanusHub.",
                "",
                "Resumen:",
                "- Proyecto: " + safeOrDefault(task.getProjectName(), "No informado") + " (" + safeOrDefault(task.getProjectCode(), "-") + ")",
                "- JIRA: " + safeOrDefault(task.getJiraTask(), "No informado"),
                "- Asignación: " + safeOrDefault(task.getDevopsAssignee(), "Cualquiera"),
                "- Fecha límite: " + formatDate(task.getDeadline()),
                "- Estado: " + safeOrDefault(task.getEstado(), "PENDIENTE"),
                "",
                "Te notificaremos por correo cuando cambie el estado.",
                "",
                "Equipo JanusHub"
        );

        return sendEmail(task.getRequesterEmail(), subject, body);
    }

    public boolean sendTaskRequestStatusUpdate(PeticionTarea task) {
        if (task == null) {
            return false;
        }

        String status = safeOrDefault(task.getEstado(), "PENDIENTE").toUpperCase();
        String subject = "[JanusHub] Petición DevOps " + status;
        String body = String.join("\n",
                "Hola " + safe(task.getRequesterName()) + ",",
                "",
                "La petición DevOps para el proyecto "
                        + safeOrDefault(task.getProjectName(), "No informado")
                        + " ha cambiado de estado.",
                "",
                "- Estado actual: " + status,
                "- Comentario administración: " + safeOrDefault(task.getAdminComment(), "Sin comentarios"),
                "",
                "Equipo JanusHub"
        );

        return sendEmail(task.getRequesterEmail(), subject, body);
    }

    public boolean sendEmailVerification(Unete request, String verifyUrl) {
        if (request == null) {
            return false;
        }

        String subject = "[JanusHub] Verifica tu dirección de correo";
        String body = String.join("\n",
                "Hola " + safe(request.getFullName()) + ",",
                "",
                "Hemos recibido tu solicitud de acceso a JanusHub.",
                "Para completarla, verifica tu dirección de correo haciendo clic en el siguiente enlace:",
                "",
                verifyUrl,
                "",
                "Este enlace es válido durante 48 horas.",
                "Si no solicitaste acceso, puedes ignorar este mensaje.",
                "",
                "Equipo JanusHub"
        );

        return sendEmail(request.getEmail(), subject, body);
    }

    private boolean sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            LOGGER.warn("No se envía email porque el destinatario está vacío. Asunto: {}", subject);
            return false;
        }

        if (!mailEnabled) {
            LOGGER.info("Email desactivado por configuración (app.mail.enabled=false). Asunto: {}", subject);
            return false;
        }

        if (mailHost == null || mailHost.isBlank()) {
            LOGGER.warn("Email no configurado: spring.mail.host vacío. No se envía correo a {}.", to);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            LOGGER.info("Correo enviado a {} con asunto '{}'", to, subject);
            return true;
        } catch (Exception ex) {
            LOGGER.error("Error enviando correo a {}: {}", to, ex.getMessage(), ex);
            return false;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeOrDefault(String value, String fallback) {
        String trimmed = safe(value);
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) {
            return "No definida";
        }
        return value.format(DATE_TIME_FORMAT);
    }
}
