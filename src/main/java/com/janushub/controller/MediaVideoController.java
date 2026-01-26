package com.janushub.controller;

import com.janushub.model.MediaVideo;
import com.janushub.repository.MediaVideoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.mp4parser.IsoFile;

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
                try {
                    // Esborrar fitxer de vídeo
                    if (existing.getFile() != null) {
                        Path videoPath = Paths.get("/app/" + existing.getFile());
                        Files.deleteIfExists(videoPath);
                    }

                    // Esborrar thumbnail
                    if (existing.getThumbnail() != null) {
                        Path thumbPath = Paths.get("/app/" + existing.getThumbnail());
                        Files.deleteIfExists(thumbPath);
                    }
                } catch (IOException e) {
                    e.printStackTrace(); // opcional: log proper
                }

                repository.delete(existing);
                return ResponseEntity.ok("Video eliminat");
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
}

    @PostMapping(value = "/videos/upload", consumes = "multipart/form-data")
    public ResponseEntity<MediaVideo> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("thumbnail") MultipartFile thumbnail,
            @RequestPart("title") String title,
            @RequestPart("description") String description) throws IOException {

        // Ruta dins del contenidor (muntada amb el volum)
        Path uploadDir = Paths.get("/app/assets/multimedia");
        Files.createDirectories(uploadDir);

        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String thumbName = UUID.randomUUID() + "-" + thumbnail.getOriginalFilename();

        Path videoPath = uploadDir.resolve(fileName);
        Path thumbPath = uploadDir.resolve(thumbName);

        file.transferTo(videoPath.toFile());
        thumbnail.transferTo(thumbPath.toFile());

        // Duració del vídeo
        long durationSeconds = 0;
        try (IsoFile isoFile = new IsoFile(videoPath.toString())) {
            double lengthInSeconds =
                    (double) isoFile.getMovieBox().getMovieHeaderBox().getDuration()
                            / isoFile.getMovieBox().getMovieHeaderBox().getTimescale();
            durationSeconds = Math.round(lengthInSeconds);
        }

        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        String durationFormatted = String.format("%02d:%02d", minutes, seconds);

        MediaVideo media = new MediaVideo();
        media.setTitle(title);
        media.setDescription(description);
        media.setFile("assets/multimedia/" + fileName);
        media.setThumbnail("assets/multimedia/" + thumbName);
        media.setDuration(durationFormatted);
        media.setCreatedAt(LocalDateTime.now());
        media.setUpdatedAt(LocalDateTime.now());

        MediaVideo saved = repository.save(media);
        return ResponseEntity.ok(saved);
    }

}
