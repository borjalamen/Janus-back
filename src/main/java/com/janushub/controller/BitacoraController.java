package com.janushub.controller;

import com.janushub.model.Bitacora;
import com.janushub.repository.BitacoraRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Optional;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bitacora")
public class BitacoraController {

    private final BitacoraRepository repository;

    public BitacoraController(BitacoraRepository repository) {
        this.repository = repository;
    }

    // =============================================================
    //                      1. CONSULTA (GET)
    // =============================================================

    // --- GET ALL (Vista principal: Solo activos/visibles) ---
    // URL: /api/bitacora/all
    @GetMapping("/all")
    public List<Bitacora> getAllBitacoras() {
        // Usa el filtro para mostrar solo los no borrados lógicamente
        return repository.findAllVisible(); 
    }
    
    // --- GET ALL RAW (Vista administrativa: Incluye borrados lógicos) ---
    // URL: /api/bitacora/all-raw
    @GetMapping("/all-raw")
    public List<Bitacora> getAllBitacorasRaw() {
        return repository.findAll();
    }

    // --- GET BY ID de Documento (Solo si está visible) ---
    // URL: /api/bitacora/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Bitacora> getBitacoraById(@PathVariable String id) {
        // Busca por la ID de documento Y que visible sea true
        return repository.findVisibleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- GET BY ID de Documento RAW (Ignora el filtro de visibilidad) ---
    // URL: /api/bitacora/raw/{id}
    @GetMapping("/raw/{id}")
    public ResponseEntity<Bitacora> getBitacoraByIdRaw(@PathVariable String id) {
        // Búsqueda sin filtros para diagnóstico
        return repository.findById(id) 
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // --- GET BY ID DE PROYECTO (Solo visibles) ---
    // URL: /api/bitacora/project/{id}
    @GetMapping("/project/{id}")
    public List<Bitacora> getByProyecto(@PathVariable String id) {
        return repository.findByProyectoVisible(id);
    }
    
    // --- GET HIDDEN (Logs de un proyecto que están borrados lógicamente) ---
    // URL: /api/bitacora/project/hidden/{id}
    @GetMapping("/project/hidden/{id}")
    public List<Bitacora> getByProyectoHidden(@PathVariable String id) {
        return repository.findByProyectoHidden(id);
    }

    // =============================================================
    //                       2. CREAR (POST)
    // =============================================================

    // URL: /api/bitacora/create
    @PostMapping("/create")
    public Bitacora createBitacora(@RequestBody Bitacora bitacora) {
      Optional <Bitacora> last = repository.findTopByOrderByIdDesc();
    int nextNumber = 1;

    if (last != null && last.get().getId() != null && last.get().getId().startsWith("bitacora-")) {

    
            String numberPart = last.get().getId().replace("bitacora-", "");
            nextNumber = Integer.parseInt(numberPart) + 1;

    }

    String newId = String.format("bitacora-%03d", nextNumber);
    bitacora.setId(newId);

    
    bitacora.setFecha(LocalDateTime.now());

    bitacora.setVisible(true);

    return repository.save(bitacora);
    }

    // =============================================================
    //                       3. ACTUALIZAR (PUT)
    // =============================================================

    // URL: /api/bitacora/update/{id}
    @PutMapping("/update/{id}")
    public ResponseEntity<Bitacora> updateBitacora(@PathVariable String id, @RequestBody Bitacora details) {
        Optional<Bitacora> optional = repository.findById(id);

    if (optional.isEmpty()) {
        return ResponseEntity.notFound().build();
    }

    Bitacora existing = optional.get();

    existing.setContexto(details.getContexto());
    existing.setError(details.getError());
    existing.setSoluciones(details.getSoluciones());
    existing.setEntorno(details.getEntorno());
    existing.setFecha(details.getFecha());
    existing.setTags(details.getTags());
    existing.setVisible(details.isVisible());

    Bitacora saved = repository.save(existing);

    return ResponseEntity.ok(saved);
}

    // =============================================================
    //                       4. BORRAR (DELETE)
    // =============================================================

    // --- DELETE LÓGICO (Soft Delete: Poner visible=false) ---
    // URL: /api/bitacora/delete/{id}
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> softDeleteBitacora(@PathVariable String id) {
        return repository.findById(id) // Usamos findById sin filtro para poder ocultar algo aunque ya esté invisible
                .map(bitacora -> {
                    bitacora.setVisible(false); // ¡Ocultar el documento!
                    repository.save(bitacora);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // --- DELETE FÍSICO (Hard Delete: Eliminación permanente) ---
    // URL: /api/bitacora/hard-delete/{id}
    @DeleteMapping("/hard-delete/{id}")
    public ResponseEntity<?> hardDeleteBitacora(@PathVariable String id) {
        return repository.findById(id)
                .map(bitacora -> {
                    repository.delete(bitacora); // Borrado definitivo de MongoDB
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}