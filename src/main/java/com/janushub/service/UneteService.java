package com.janushub.service;
 
import com.janushub.model.Unete;
import com.janushub.model.Users;
import com.janushub.repository.UneteRepository;
import com.janushub.repository.UserRepository;
import dto.ApprovalResponseDTO;
import dto.UneteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
 
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
 
@Service
@RequiredArgsConstructor
public class UneteService {
 
    private final UneteRepository uneteRepository;
    private final UserRepository userRepository;
    private final UserService userService;
 
    /**
     * Registra una nueva peticion de "unete" con estado PENDIENTE.
     * Valida que el email sea válido y no esté duplicado en usuarios existentes.
     */
    public Unete createRequest(UneteDTO dto) {
        // Validar email
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }
       
        // Validar que no exista un usuario con ese email
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta asociada a ese email.");
        }
       
        // Validar que no haya una solicitud pendiente con ese email
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
        request.setEstado("PENDIENTE");
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        return uneteRepository.save(request);
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
     * Aprueba una petición Y CREA EL USUARIO en la base de datos.
     * El username será el nombre completo y la contraseña será nombre + 1234
     *
     * @param id - ID de la petición
     * @param aRespuesta con la petición actualizada y las credenciales del usuario creado
     * @throws IllegalArgumentException si el email ya está registrado
     */
    public ApprovalResponseDTO approveRequest(String id, String adminComment) {
        Unete request = getRequestById(id);
       
        // Variables para las credenciales
        String username = null;
        String autoPassword = null;
       
        // CREAR EL USUARIO
        try {
            // El username será el nombre completo (sin espacios para evitar problemas)
            username = request.getFullName().replaceAll("\\s+", "").toLowerCase();
           
            // Generar contraseña: nombre completo + 1234
            autoPassword = request.getFullName() + "1234";
           
            // Determinar los roles basado en el campo "role" de la petición
            List<String> roles = determineRoles(request.getRole());
           
            // Crear el usuario
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
       
        // Actualizar la petición a APROBADA
        request.setEstado("APROBADA");
        request.setAdminComment(adminComment);
        request.setUpdatedAt(LocalDateTime.now());
        Unete updatedRequest = uneteRepository.save(request);
       
        // Crear y retornar la respuesta con las credenciales
        ApprovalResponseDTO.UserCredentials credentials =
            new ApprovalResponseDTO.UserCredentials(username, autoPassword, request.getEmail());
       
        return new ApprovalResponseDTO(updatedRequest, credentials);
    }
 
    /**
     * Rechaza una peticion.
     */
    public Unete rejectRequest(String id, String adminComment) {
        return updateEstado(id, "RECHAZADA", adminComment);
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
 