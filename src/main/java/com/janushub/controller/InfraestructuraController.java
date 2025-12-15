package com.janushub.controller;

import com.janushub.model.Infraestructura;
import com.janushub.repository.InfraestructuraRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/infraestructura")
public class InfraestructuraController {
private final InfraestructuraRepository repository;

    public InfraestructuraController(InfraestructuraRepository repository) {
        this.repository = repository;
    }

    // --- GET ALL (Solo activos) ---
    @GetMapping("/all")
    public List<Infraestructura> getAllInfra() {
        // Usamos el método personalizado que filtra los borrados
        return repository.findByDeletedFalse();
    }

    // --- SEARCH BY HOST (Solo activos) ---
    @GetMapping("/search/{host}")
    public List<Infraestructura> searchByHost(@PathVariable String host) {
        return repository.findByHostContainingIgnoreCaseAndDeletedFalse(host);
    }

    // --- POST (Create) ---
    @PostMapping("/create")
    public Infraestructura createInfra(@RequestBody Infraestructura infra) {
        infra.setDeleted(false); // Nos aseguramos que nazca vivo
        return repository.save(infra);
    }

    // --- PUT (Update) ---
    @PutMapping("/update/{id}")
    public ResponseEntity<Infraestructura> updateInfra(@PathVariable String id, @RequestBody Infraestructura infraDetails) {
        // Buscamos solo si no está borrado
        return repository.findByIdAndDeletedFalse(id)
                .map(existingInfra -> {
                    // Actualizamos campos (excepto 'deleted')
                    existingInfra.setCodProyecto(infraDetails.getCodProyecto());
                    existingInfra.setIp(infraDetails.getIp());
                    existingInfra.setEstado(infraDetails.getEstado());
                    existingInfra.setSo(infraDetails.getSo());
                    existingInfra.setHost(infraDetails.getHost());
                    existingInfra.setTags(infraDetails.getTags());
                    
                    existingInfra.setAuth(infraDetails.getAuth());
                    existingInfra.setJdk(infraDetails.getJdk());
                    existingInfra.setServices(infraDetails.getServices());
                    existingInfra.setCrc(infraDetails.getCrc());

                    return ResponseEntity.ok(repository.save(existingInfra));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- DELETE LÓGICO (Soft Delete - Papelera) ---
    // URL: http://localhost:8080/api/infraestructura/delete/{id}
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> softDeleteInfra(@PathVariable String id) {
        // Buscamos por ID (incluso si ya está borrado, para no dar 404)
        return repository.findById(id)
                .map(infra -> {
                    infra.setDeleted(true); // Solo marcamos como borrado
                    repository.save(infra);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- DELETE FÍSICO (Hard Delete - Destruir para siempre) ---
    // URL: http://localhost:8080/api/infraestructura/hard-delete/{id}
    @DeleteMapping("/hard-delete/{id}")
    public ResponseEntity<?> hardDeleteInfra(@PathVariable String id) {
        // Usamos findById normal para poder borrar incluso los que ya tienen deleted=true
        return repository.findById(id)
                .map(infra -> {
                    repository.delete(infra); // ¡ESTO LO BORRA DE MONGODB!
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}