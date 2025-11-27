package com.janushub.controller;

import com.janushub.model.Jenkins;
import com.janushub.repository.JenkinsRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jenkins") // Ruta base para esta entidad
public class JenkinsController {

    private final JenkinsRepository repository;

    public JenkinsController(JenkinsRepository repository) {
        this.repository = repository;
    }

    // 1. getAllJenkins (GET)
    // URL: http://localhost:8080/api/jenkins/all
    @GetMapping("/all")
    public List<Jenkins> getAllJenkins() {
        return repository.findAll();
    }

    // 2. createJenkins (POST)
    // URL: http://localhost:8080/api/jenkins/create
    @PostMapping("/create")
    public Jenkins createJenkins(@RequestBody Jenkins jenkins) {
        // Al guardar, Mongo generará el ID automáticamente si viene null
        return repository.save(jenkins);
    }
}
