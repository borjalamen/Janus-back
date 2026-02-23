package com.janushub.controller;

import com.janushub.model.Bitacora;
import com.janushub.repository.BitacoraRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

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
        return repository.findByVisibleTrue(); 
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
        return repository.findByIdAndVisibleTrue(id)
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
    // URL: /api/bitacora/project/{idProyecto}
    @GetMapping("/project/{idProyecto}")
    public List<Bitacora> getByProyecto(@PathVariable String idProyecto) {
        return repository.findByIdProyectoAndVisibleTrue(idProyecto);
    }
    
    // --- GET HIDDEN (Logs de un proyecto que están borrados lógicamente) ---
    // URL: /api/bitacora/project/hidden/{idProyecto}
    @GetMapping("/project/hidden/{idProyecto}")
    public List<Bitacora> getByProyectoHidden(@PathVariable String idProyecto) {
        return repository.findByIdProyectoAndVisibleFalse(idProyecto);
    }

    // =============================================================
    //                       2. CREAR (POST)
    // =============================================================

    // URL: /api/bitacora/create
    @PostMapping("/create")
    public Bitacora createBitacora(@RequestBody Bitacora bitacora) {
       Bitacora last = repository.findTopByIdStartingWithOrderByIdDesc("bitacora-");

    int nextNumber = 1;

    if (last != null && last.getId() != null && last.getId().startsWith("bitacora-")) {
        String numberPart = last.getId().replace("bitacora-", "");
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
        // Buscamos solo si está visible (para que no se pueda editar un borrado lógico)
        return repository.findByIdAndVisibleTrue(id) 
                .map(existingBitacora -> {
                    // Actualización de campos
                    existingBitacora.setIdProyecto(details.getIdProyecto());
                    existingBitacora.setContexto(details.getContexto());
                    existingBitacora.setError(details.getError());
                    existingBitacora.setSolucion(details.getSolucion());
                    existingBitacora.setFecha(details.getFecha());
                    existingBitacora.setTags(details.getTags());

                    // Solo actualizamos 'visible' si se envía explícitamente en el cuerpo
                    // Si no se envía, se mantiene el valor existente (true)
                    if (details.isVisible() != existingBitacora.isVisible()) {
                        existingBitacora.setVisible(details.isVisible());
                    }

                    return ResponseEntity.ok(repository.save(existingBitacora));
                })
                .orElse(ResponseEntity.notFound().build());
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