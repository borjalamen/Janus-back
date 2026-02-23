package com.janushub.controller;

import com.janushub.model.Parametritzacio;
import com.janushub.repository.ParametritzacioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/parametrization/version")
    public ResponseEntity<String> getVersion() {
        return repository.findAll()
            .stream()
            .findFirst()
            .map(Parametritzacio::getVersion)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // PUT /api/config/version - Actualizar versión
    @PutMapping("/version")
    public ResponseEntity<?> updateVersion(@RequestBody Map<String, String> body) {
        String newVersion = body.get("version");
        if (newVersion == null || newVersion.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "El campo 'version' es obligatorio"));
        }

        Parametritzacio config = repository.findAll()
            .stream()
            .findFirst()
            .orElseGet(() -> {
                Parametritzacio nuevo = new Parametritzacio();
                return nuevo;
            });

        config.setVersion(newVersion);
        repository.save(config);

        return ResponseEntity.ok(Map.of(
            "mensaje", "Versión actualizada correctamente",
            "version", newVersion
        ));
    }
}
