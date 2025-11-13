package com.janushub.controller;

import com.janushub.model.Parametritzacio;
import com.janushub.repository.ParametritzacioRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/config")
public class ParametritzacioController {
private final ParametritzacioRepository repository;

    public ParametritzacioController(ParametritzacioRepository repository) {
        this.repository = repository;
    }

    // Este endpoint ahora es PÚBLICO porque no hay seguridad
    @GetMapping("/all")
    public List<Parametritzacio> getAllConfig() {
        return repository.findAll();
    }
}
