package com.janushub.controller;

import com.janushub.config.JwUtil;
import com.janushub.model.Users;
import com.janushub.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwUtil jwUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwUtil jwUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwUtil = jwUtil;
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> loginRequest) {

        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.status(401).body("Error: Credenciales inválidas.");
        }

        Users user = userRepository.findByUsername(username).orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body("Error: Credenciales inválidas.");
        }

        String status = user.getStatus() != null ? user.getStatus().toUpperCase() : "ACTIVE";
        if ("INACTIVE".equals(status) || "DISABLED".equals(status)) {
            return ResponseEntity.status(403).body("Error: Usuario inhabilitado.");
        }

        String token = jwUtil.generateToken(user.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("roles", user.getRoles());

        return ResponseEntity.ok(response);
    }
}