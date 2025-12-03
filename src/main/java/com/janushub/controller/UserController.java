package com.janushub.controller;

import com.janushub.model.Users;
import com.janushub.repository.UserRepository;
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
    public Users createUser(@RequestBody Users user) {
        // Como estamos sin seguridad, guardamos la password tal cual viene
        return repository.save(user);
    }

    // --- PUT (Actualizar Usuario) ---
    @PutMapping("/update/{id}")
    public ResponseEntity<Users> updateUser(@PathVariable String id, @RequestBody Users userDetails) {
        return repository.findById(id)
                .map(users -> {
                    // Actualizamos los campos importantes
                    users.setUsername(userDetails.getUsername());
                    users.setFullName(userDetails.getFullName());
                    users.setEmail(userDetails.getEmail()); // Asegúrate de tener este campo en tu Modelo
                    users.setRoles(userDetails.getRoles());
                    
                    // Solo actualizamos la contraseña si nos envían una nueva
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
                    // Leemos el nuevo estado del JSON (ej: "INACTIVE")
                    String newStatus = statusMap.get("status");
                    
                    if (newStatus != null && !newStatus.isEmpty()) {
                        users.setStatus(newStatus);
                        // Aquí podrías añadir lógica extra, como guardar la fecha de baja
                    }
                    
                    Users updatedUser = repository.save(users);
                    return ResponseEntity.ok(updatedUser);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}