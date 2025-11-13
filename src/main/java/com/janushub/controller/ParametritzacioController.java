package com.janushub.controller;

import com.janushub.model.Parametritzacio;
import com.janushub.repository.ParametritzacioRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController // ¡La anotación clave para REST!
@RequestMapping("/api/config") // La URL base para este controlador
public class ParametritzacioController {
    private final ParametritzacioRepository repository;

    public ParametritzacioController(ParametritzacioRepository repository) {
        this.repository = repository;
    }

    // ¡Aquí está tu endpoint GET!
    @GetMapping("/all")
    public List<Parametritzacio> getAllConfig() {
        // Esto buscará todos los documentos en la colección 'parametritzacio'
        return repository.findAll();
    }
}
