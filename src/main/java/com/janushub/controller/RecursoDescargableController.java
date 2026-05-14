package com.janushub.controller;

import com.janushub.model.RecursoDescargable;
import com.janushub.repository.RecursoDescargableRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * API de Recursos Descargables Generales
 *
 * Los ficheros se guardan en:   volumenDocumentos/recursos-generales/<mongoId>/<fileName>
 * Los metadatos (nombre, desc, categoría, etc.) se persisten en MongoDB.
 *
 * Endpoints:
 *   GET    /api/recursos-descargables              – listar todos
 *   GET    /api/recursos-descargables/{id}/file    – descargar fichero
 *   POST   /api/recursos-descargables              – subir nuevo (multipart)
 *   PUT    /api/recursos-descargables/{id}         – actualizar metadatos
 *   DELETE /api/recursos-descargables/{id}         – eliminar metadatos + fichero
 */
@RestController
@RequestMapping("/api/recursos-descargables")
public class RecursoDescargableController {

    private static final String SUBFOLDER = "recursos-generales";

    private final RecursoDescargableRepository repo;
    private final String volumen;

    public RecursoDescargableController(RecursoDescargableRepository repo) {
        this.repo = repo;
        this.volumen = findVolumenPath();
    }

    // ── Listado ───────────────────────────────────────────────────────────────

    @GetMapping
    public List<RecursoDescargable> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    // ── Descarga ──────────────────────────────────────────────────────────────

    @GetMapping("/{id}/file")
    public ResponseEntity<?> download(@PathVariable String id) {
        return repo.findById(id).map(rec -> {
            try {
                Path file = Paths.get(volumen, rec.getFilePath());
                if (!Files.exists(file)) {
                    return ResponseEntity.notFound().build();
                }
                InputStreamResource resource = new InputStreamResource(new FileInputStream(file.toFile()));
                String encoded = URLEncoder.encode(rec.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
                return ResponseEntity.ok()
                        .contentType(rec.getMimeType() != null
                                ? MediaType.parseMediaType(rec.getMimeType())
                                : MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename*=UTF-8''" + encoded)
                        .contentLength(Files.size(file))
                        .body((Object) resource);
            } catch (IOException e) {
                return ResponseEntity.internalServerError().build();
            }
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ── Subida ────────────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "displayName", required = false) String displayName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Fichero vacío");
        }

        String originalName = Paths.get(
                Objects.requireNonNullElse(file.getOriginalFilename(), "file")).getFileName().toString();
        // Tomcat lee el filename del Content-Disposition como ISO-8859-1; re-codificamos a UTF-8
        originalName = new String(originalName.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);

        // Bloquear comprimidos
        String lower = originalName.toLowerCase();
        List<String> blocked = List.of("zip", "rar", "7z", "tar", "gz", "tgz", "bz2");
        String ext = lower.contains(".") ? lower.substring(lower.lastIndexOf('.') + 1) : "";
        if (blocked.contains(ext)) {
            return ResponseEntity.badRequest().body("Tipo de fichero no permitido: " + ext);
        }

        try {
            // Crear registro en Mongo primero para obtener el ID
            RecursoDescargable rec = new RecursoDescargable();
            rec.setFileName(originalName);
            rec.setDisplayName(displayName != null && !displayName.isBlank() ? displayName : originalName);
            rec.setDescription(description);
            rec.setCategory(category);
            rec.setSizeBytes(file.getSize());
            rec.setMimeType(file.getContentType());
            rec.setUploadedBy(uploadedBy);
            rec.setCreatedAt(LocalDateTime.now());
            rec.setUpdatedAt(LocalDateTime.now());
            rec = repo.save(rec); // obtemos el id

            // Guardar fichero en volumen
            Path carpeta = Paths.get(volumen, SUBFOLDER, rec.getId());
            Files.createDirectories(carpeta);
            Path dest = carpeta.resolve(originalName);
            file.transferTo(dest.toFile());

            // Guardar ruta relativa
            rec.setFilePath(SUBFOLDER + "/" + rec.getId() + "/" + originalName);
            rec = repo.save(rec);

            return ResponseEntity.ok(rec);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error guardando fichero: " + e.getMessage());
        }
    }

    // ── Actualizar metadatos ──────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMeta(
            @PathVariable String id,
            @RequestBody RecursoDescargable body) {
        return repo.findById(id).map(rec -> {
            if (body.getDisplayName() != null) rec.setDisplayName(body.getDisplayName());
            if (body.getDescription() != null) rec.setDescription(body.getDescription());
            if (body.getCategory() != null) rec.setCategory(body.getCategory());
            rec.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(repo.save(rec));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        return repo.findById(id).map(rec -> {
            // Eliminar fichero del volumen
            try {
                Path carpeta = Paths.get(volumen, SUBFOLDER, id);
                if (Files.exists(carpeta)) {
                    try (var stream = Files.walk(carpeta)) {
                        stream.sorted(Comparator.reverseOrder())
                              .map(Path::toFile)
                              .forEach(File::delete);
                    }
                }
            } catch (IOException e) {
                // Continuar aunque falle el borrado físico
                System.err.println("No se pudo eliminar carpeta del volumen: " + e.getMessage());
            }
            repo.deleteById(id);
            return ResponseEntity.noContent().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ── Resolución del volumen (igual que DocumentController) ─────────────────

    private String findVolumenPath() {
        String overridePath = System.getProperty("JANUS_VOLUMEN_PATH");
        if (overridePath == null || overridePath.trim().isEmpty()) {
            overridePath = System.getenv("JANUS_VOLUMEN_PATH");
        }
        if (overridePath != null && !overridePath.trim().isEmpty()) {
            Path custom = Paths.get(overridePath.trim()).toAbsolutePath().normalize();
            try { Files.createDirectories(custom); return custom.toString(); } catch (IOException ignored) {}
        }

        String userDir = System.getProperty("user.dir");
        Path base = Paths.get(userDir).toAbsolutePath().normalize();
        List<Path> candidates = new ArrayList<>();
        candidates.add(base.resolve("volumenDocumentos"));
        candidates.add(base.resolve("Janus-back").resolve("volumenDocumentos"));
        Path cur = base;
        for (int i = 0; i < 4 && cur != null; i++) {
            candidates.add(cur.resolve("volumenDocumentos"));
            candidates.add(cur.resolve("Janus-back").resolve("volumenDocumentos"));
            cur = cur.getParent();
        }

        Set<Path> unique = new LinkedHashSet<>();
        for (Path c : candidates) unique.add(c.normalize());

        Path first = null, nonEmpty = null;
        for (Path c : unique) {
            if (Files.exists(c) && Files.isDirectory(c)) {
                if (first == null) first = c;
                try (var s = Files.list(c)) { if (s.findAny().isPresent()) { nonEmpty = c; break; } }
                catch (IOException ignored) {}
            }
        }
        Path selected = nonEmpty != null ? nonEmpty : first;
        if (selected == null) {
            selected = base.resolve("volumenDocumentos");
            try { Files.createDirectories(selected); } catch (IOException ignored) {}
        }
        return selected.toString();
    }
}
