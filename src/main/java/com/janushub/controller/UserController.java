package com.janushub.controller;

import com.janushub.model.Users;
import com.janushub.repository.UserRepository;
import com.mongodb.MongoWriteException; 
import org.springframework.http.HttpStatus; 
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/users") // Ruta distinta a /api/auth
public class UserController {

    private final UserRepository repository;

    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    // --- GET ALL ---
    @GetMapping("/all")
    public List<Users> getAllUsers() {
        return repository.findAll();
    }

    // --- POST (Crear Usuario Manualmente) ---
    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestBody Users user) {
        try {
            Users savedUser = repository.save(user);
            // Si tiene éxito, devuelve 201 Created
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser); 
            
        } catch (MongoWriteException e) {
            // Código de error 11000 es el código estándar de MongoDB para clave duplicada
            if (e.getError().getCode() == 11000) { 
                String errorMessage = "Error 409: El nombre de usuario '" + user.getUsername() + "' ya existe y debe ser único.";
                // Devolvemos 409 Conflict con un mensaje de error claro
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
            }
            // Si es otro error de escritura, lo relanzamos
            throw e; 
        }
    }

    // --- PUT (Actualizar Usuario) ---
    @PutMapping("/update/{id}")
    public ResponseEntity<Users> updateUser(@PathVariable String id, @RequestBody Users userDetails) {
        return repository.findById(id)
                .map(users -> {
                    
                    users.setUsername(userDetails.getUsername());
                    users.setFullName(userDetails.getFullName());
                    users.setEmail(userDetails.getEmail()); 
                    users.setRoles(userDetails.getRoles());
                    
                    
                    if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                        users.setPassword(userDetails.getPassword());
                    }

                    Users updatedUser = repository.save(users);
                    return ResponseEntity.ok(updatedUser);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- DELETE (Borrar Usuario) ---
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        return repository.findById(id)
                .map(users -> {
                    repository.delete(users);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- CAMBIAR ESTADO (Activar/Inactivar) ---
    // URL: http://localhost:8080/api/users/status/{id}
    @PutMapping("/status/{id}")
    public ResponseEntity<Users> updateUserStatus(@PathVariable String id, @RequestBody Map<String, String> statusMap) {
        return repository.findById(id)
                .map(users -> {
                   
                    String newStatus = statusMap.get("status");
                    
                    if (newStatus != null && !newStatus.isEmpty()) {
                        users.setStatus(newStatus);
                        
                    }
                    
                    Users updatedUser = repository.save(users);
                    return ResponseEntity.ok(updatedUser);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- BUSCAR POR USERNAME ---
    // URL: http://localhost:8080/api/users/search/parteDelNombre
    @GetMapping("/search/{username}")
    public List<Users> getUsersByName(@PathVariable String username) {
        return repository.findByUsernameContainingIgnoreCase(username);
    }
}