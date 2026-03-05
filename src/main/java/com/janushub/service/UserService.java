package com.janushub.service;
 
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
 
import org.springframework.stereotype.Service;
 
import com.janushub.model.Users;
import com.janushub.repository.UserRepository;
 
import lombok.RequiredArgsConstructor;
 
@Service
@RequiredArgsConstructor
public class UserService {
 
    private final UserRepository userRepository;
 
    private Users getUserOrThrow(String username) {
        System.out.println("DEBUG getUserOrThrow -> username = " + username);
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Usuario no encontrado: " + username));
    }
 
    // ---------- CREAR USUARIO ----------
    /**
     * Crea un nuevo usuario con los datos proporcionados.
     * Valida que no exista un usuario con el mismo username o email.
     *
     * @param username - identificador único del usuario
     * @param password - contraseña del usuario (se almacena como texto plano o hash según configuración)
     * @param fullName - nombre completo del usuario
     * @param email - email del usuario
     * @param roles - roles del usuario (ej. ["ADMIN", "CONSULTOR", "DEV"])
     * @return usuario creado
     * @throws IllegalArgumentException si el usuario o email ya existe
     */
    public Users createUser(String username, String password, String fullName, String email, List<String> roles) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }
       
        // Validar que no existe usuario con ese username
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("El usuario '" + username + "' ya existe.");
        }
       
        // Validar que no existe usuario con ese email
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta asociada al email '" + email + "'.");
        }
 
        Users newUser = new Users();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setFullName(fullName != null ? fullName : username);
        newUser.setEmail(email);
        newUser.setRoles(roles != null && !roles.isEmpty() ? roles : Arrays.asList("CONSULTOR"));
        newUser.setStatus("ACTIVE");
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
 
        return userRepository.save(newUser);
    }
 
    /**
     * Genera una contraseña autogenerada en el formato: [nombre]1234
     * Utiliza solo el primer nombre (word) del nombre completo.
     *
     * @param fullName - nombre completo del usuario
     * @return contraseña autogenerada
     */
    public String generateAutoPassword(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "user1234";
        }
       
        // Obtener el primer nombre (primera palabra antes del espacio)
        String firstName = fullName.trim().split("\\s+")[0].toLowerCase();
        return firstName + "1234";
    }
 
    // ---------- AVATAR ----------
    public void updateAvatar(String username, String path) {
        Users user = getUserOrThrow(username);
        user.setAvatarPath(path);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
 
    public String getAvatarPath(String username) {
        return userRepository.findByUsername(username)
                .map(Users::getAvatarPath)
                .orElse(null);
    }
 
    public void removeAvatar(String username) {
        Users user = getUserOrThrow(username);
        user.setAvatarPath(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
 
    // ---------- CV ----------
    public void updateCv(String username, String path) {
        Users user = getUserOrThrow(username);
        user.setCvPath(path);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
 
    public String getCvPath(String username) {
        return userRepository.findByUsername(username)
                .map(Users::getCvPath)
                .orElse(null);
    }
 
    public void removeCv(String username) {
        Users user = getUserOrThrow(username);
        user.setCvPath(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
 
    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }
}