package com.janushub.service;

import com.janushub.model.Unete;
import com.janushub.model.Users;
import com.janushub.repository.UneteRepository;
import com.janushub.repository.UserRepository;
import com.janushub.service.NotificationService;
import dto.ApprovalResponseDTO;
import dto.UneteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UneteService {

    private final UneteRepository uneteRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailNotificationService emailNotificationService;
    private final NotificationService notificationService;

    @Value("${app.base-url:http://localhost:4200}")
    private String baseUrl;

    /**
     * Registra una nueva peticion de "unete" con estado PENDIENTE.
     * Valida que el email sea válido y no esté duplicado en usuarios existentes.
     */
    public Unete createRequest(UneteDTO dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta asociada a ese email.");
        }

        List<Unete> existingRequests = uneteRepository.findByEmailIgnoreCase(dto.getEmail());
        if (existingRequests.stream().anyMatch(r -> "PENDIENTE".equals(r.getEstado()))) {
            throw new IllegalArgumentException("Ya existe una solicitud pendiente para este email.");
        }

        Unete request = new Unete();
        request.setFullName(dto.getFullName() != null ? dto.getFullName() : "");
        request.setEmail(dto.getEmail());
        request.setRole(dto.getRole() != null ? dto.getRole() : "");
        request.setProjectCode(dto.getProjectCode() != null ? dto.getProjectCode() : "");
        request.setProjectName(dto.getProjectName() != null ? dto.getProjectName() : "");
        request.setComments(dto.getComments() != null ? dto.getComments() : "");
        request.setEstado("INICIADA");
        request.setEmailToken(UUID.randomUUID().toString());
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        Unete saved = uneteRepository.save(request);

        String verifyUrl = baseUrl + "/verificar?token=" + saved.getEmailToken();
        emailNotificationService.sendEmailVerification(saved, verifyUrl);

        return saved;
    }

    /**
     * Verifica el email de una solicitud usando el token recibido por correo.
     * Cambia el estado de INICIADA a PENDIENTE y notifica a los administradores.
     */
    public Unete verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token de verificación inválido.");
        }

        Unete request = uneteRepository.findByEmailToken(token)
                .orElseThrow(() -> new IllegalArgumentException("El enlace de verificación no es válido o ha expirado."));

        if ("PENDIENTE".equals(request.getEstado())
                || "APROBADA".equals(request.getEstado())
                || "RECHAZADA".equals(request.getEstado())) {
            // Ya fue verificado anteriormente — devolvemos el estado actual sin error
            return request;
        }

        if (!"INICIADA".equals(request.getEstado())) {
            throw new IllegalArgumentException("Esta solicitud no está en estado válido para verificación.");
        }

        request.setEstado("PENDIENTE");
        request.setEmailVerifiedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        Unete saved = uneteRepository.save(request);

        // Notificar a ADMIN y DEVOPS vía WebSocket
        notificationService.broadcastToRoles(
                java.util.List.of("ADMIN", "DEVOPS"),
                "JOIN_NUEVA",
                "Nueva solicitud de acceso",
                saved.getFullName() + " ha verificado su correo y quiere unirse al equipo",
                "/administracion"
        );

        return saved;
    }

    /**
     * Devuelve todas las peticiones (para el panel de administracion).
     */
    public List<Unete> getAllRequests() {
        return uneteRepository.findAll();
    }

    /**
     * Filtra peticiones por estado.
     */
    public List<Unete> getRequestsByEstado(String estado) {
        return uneteRepository.findByEstado(estado);
    }

    /**
     * Obtiene una peticion por su ID.
     */
    public Unete getRequestById(String id) {
        return uneteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Peticion no encontrada: " + id));
    }

    /**
     * Busca la última petición por email (para que el front de "únete" consulte el estado).
     */
    public Unete getByEmail(String email) {
        List<Unete> requests = uneteRepository.findByEmailIgnoreCase(email);
        if (requests.isEmpty()) {
            return null;
        }
        return requests.stream()
                .sorted(Comparator.comparing(Unete::getCreatedAt).reversed())
                .findFirst()
                .orElse(null);
    }

    /**
     * Aprueba una petición Y CREA EL USUARIO en la base de datos.
     * El username será el nombre completo y la contraseña será nombre + 1234
     */
    public ApprovalResponseDTO approveRequest(String id, String adminComment) {
        Unete request = getRequestById(id);

        String username;
        String autoPassword;

        try {
            username = request.getFullName().replaceAll("\\s+", "").toLowerCase();
            autoPassword = request.getFullName() + "1234";

            List<String> roles = determineRoles(request.getRole());

            userService.createUser(
                    username,
                    autoPassword,
                    request.getFullName(),
                    request.getEmail(),
                    roles
            );

            System.out.println("✅ Usuario creado correctamente: " + username + " | Contraseña: " + autoPassword);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error creando usuario: " + e.getMessage());
            throw new RuntimeException("No se pudo crear el usuario: " + e.getMessage(), e);
        }

        request.setEstado("APROBADA");
        request.setAdminComment(adminComment);
        request.setUpdatedAt(LocalDateTime.now());
        Unete updatedRequest = uneteRepository.save(request);

        emailNotificationService.sendJoinRequestApproved(updatedRequest, username, autoPassword);

        ApprovalResponseDTO.UserCredentials credentials =
                new ApprovalResponseDTO.UserCredentials(username, autoPassword, request.getEmail());

        return new ApprovalResponseDTO(updatedRequest, credentials);
    }

    /**
     * Rechaza una peticion.
     */
    public Unete rejectRequest(String id, String adminComment) {
        Unete updated = updateEstado(id, "RECHAZADA", adminComment);
        emailNotificationService.sendJoinRequestRejected(updated);
        return updated;
    }

    /**
     * Actualiza el estado de una peticion existente.
     */
    private Unete updateEstado(String id, String nuevoEstado, String adminComment) {
        Unete request = getRequestById(id);
        request.setEstado(nuevoEstado);
        request.setAdminComment(adminComment);
        request.setUpdatedAt(LocalDateTime.now());
        return uneteRepository.save(request);
    }

    /**
     * Elimina una peticion por su ID.
     */
    public void deleteRequest(String id) {
        if (!uneteRepository.existsById(id)) {
            throw new IllegalArgumentException("Peticion no encontrada: " + id);
        }
        uneteRepository.deleteById(id);
    }

    /**
     * Convierte el campo role de la petición a una lista de roles de usuario.
     */
    private List<String> determineRoles(String role) {
        if (role == null || role.isBlank()) {
            return Arrays.asList("CONSULTOR");
        }

        role = role.toUpperCase();

        switch (role) {
            case "ADMIN":
                return Arrays.asList("ADMIN", "CONSULTOR");
            case "DEVOPS":
            case "DEV":
                return Arrays.asList("DEV", "CONSULTOR");
            case "INVITADO":
            case "GUEST":
                return Arrays.asList("CONSULTOR");
            default:
                return Arrays.asList("CONSULTOR");
        }
    }
}
