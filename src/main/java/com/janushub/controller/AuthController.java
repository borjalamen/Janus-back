package com.janushub.controller;

import com.janushub.model.Users; // <-- ¡CAMBIO A SINGULAR!
import com.janushub.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
// ¡CORREGIDO! La variable debe empezar con minúscula
    private final UserRepository userRepository; 

    // ¡CORREGIDO! El constructor debe usar la variable (esta era tu línea 18)
    public AuthController(UserRepository userRepository) { 
        this.userRepository = userRepository;
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> loginRequest) {

        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        // ¡CORREGIDO! Usamos la variable (minúscula)
        Users user = userRepository.findByUsername(username); // (Esta era tu línea 27)

        // Verificación en texto plano
        if (user == null || !Objects.equals(password, user.getPassword())) { // (Esta era tu línea 30)
            return ResponseEntity.status(401).body("Error: Credenciales inválidas (demo).");
        }

        // ¡ÉXITO!
        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("roles", user.getRoles());

        return ResponseEntity.ok(response);
    }
}