package com.janushub.controller;

import com.janushub.model.MediaVideo;
import com.janushub.repository.MediaVideoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/media")
public class MediaVideoController {

    private final MediaVideoRepository repository;

    public MediaVideoController(MediaVideoRepository repository) {
        this.repository = repository;
    }

    // Manifest: llistar tots els vídeos
    @GetMapping("/videos")
    public List<MediaVideo> getAll() {
        return repository.findAll();
    }

    // Consultar un vídeo per id
    @GetMapping("/videos/{id}")
    public ResponseEntity<MediaVideo> getOne(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Crear vídeo (afegir al manifest)
    @PostMapping("/videos")
    public ResponseEntity<MediaVideo> create(@RequestBody MediaVideo body) {
        if (body.getCreatedAt() == null) {
            body.setCreatedAt(LocalDateTime.now());
        }
        body.setUpdatedAt(LocalDateTime.now());
        MediaVideo saved = repository.save(body);
        return ResponseEntity.ok(saved);
    }

    // Actualitzar vídeo
    @PutMapping("/videos/{id}")
    public ResponseEntity<MediaVideo> update(
            @PathVariable String id,
            @RequestBody MediaVideo body) {

        return repository.findById(id)
                .map(existing -> {
                    existing.setTitle(body.getTitle());
                    existing.setDescription(body.getDescription());
                    existing.setFile(body.getFile());
                    existing.setThumbnail(body.getThumbnail());
                    existing.setDuration(body.getDuration());
                    existing.setUpdatedAt(LocalDateTime.now());
                    MediaVideo saved = repository.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Eliminar vídeo
    @DeleteMapping("/videos/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        return repository.findById(id)
                .map(existing -> {
                    repository.delete(existing);
                    return ResponseEntity.ok("Video eliminat");
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
