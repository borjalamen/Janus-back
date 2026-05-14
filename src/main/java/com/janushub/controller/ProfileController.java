package com.janushub.controller;

import com.janushub.model.Users;
import dto.ChangePasswordRequest;
import com.janushub.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- CONSULTAR PERFIL ---
    @GetMapping
    public ResponseEntity<Users> getProfile(@RequestParam String username) {
        Users user = userRepository.findByUsername(username).orElse(null  );
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setPassword(null); 
        return ResponseEntity.ok(user);
    }

    // --- ACTUALITZAR PERFIL (sin password) ---
    @PutMapping
    public ResponseEntity<Users> updateProfile(@RequestParam String username,
                                               @RequestBody Users profileData) {
        Users user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setFullName(profileData.getFullName());
        user.setEmail(profileData.getEmail());
        user.setPhone(profileData.getPhone());
        user.setUpdatedAt(LocalDateTime.now());

        Users updated = userRepository.save(user);
        updated.setPassword(null);
        return ResponseEntity.ok(updated);
    }

    // --- CAMBIAR CONTRASENYA ---
    
@PutMapping("/password")
public ResponseEntity<?> changePassword(@RequestParam String username,
                                        @RequestBody ChangePasswordRequest request) {
    Users user = userRepository.findByUsername(username).orElse(null);
    if (user == null) {
        return ResponseEntity.status(404).body("Usuari no trobat");
    }

    
    String newPassword = request.getNewPassword();
    if (newPassword == null || newPassword.isBlank() || newPassword.contains("{{")) {
        return ResponseEntity.badRequest().body("Contrasenya no vàlida");
    }

    
    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
        return ResponseEntity.status(400).body("Contrasenya actual incorrecta");
    }

   
    user.setPassword(passwordEncoder.encode(newPassword));
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);

    return ResponseEntity.ok("Contrasenya actualitzada correctament");
}


    // --- ELIMINAR PERFIL ---
    @DeleteMapping
    public ResponseEntity<?> deleteProfile(@RequestParam String username) {
        Users user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        userRepository.delete(user);
        return ResponseEntity.ok("Perfil eliminat");
    }
}