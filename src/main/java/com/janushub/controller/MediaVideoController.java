package com.janushub.controller;

import com.janushub.model.MediaVideo;
import com.janushub.repository.MediaVideoRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * API de Multimedia (vÃ­deos y otros ficheros media)
 *
 * Los ficheros se guardan en:   volumenDocumentos/media-videos/<mongoId>/<fileName>
 * Los metadatos se persisten en MongoDB (colecciÃ³n media_videos).
 *
 * Endpoints:
 *   GET    /api/media/videos              â€“ listar todos (ordenados por fecha desc)
 *   GET    /api/media/videos/{id}         â€“ obtener uno
 *   GET    /api/media/videos/{id}/stream  â€“ streamar fichero (soporta Range)
 *   GET    /api/media/videos/{id}/thumbnail â€“ servir miniatura
 *   POST   /api/media/videos              â€“ subir nuevo fichero (multipart)
 *   PUT    /api/media/videos/{id}         â€“ actualizar metadatos
 *   DELETE /api/media/videos/{id}         â€“ eliminar metadatos + fichero
 */
@RestController
@RequestMapping("/api/media/videos")
public class MediaVideoController {

    private static final String SUBFOLDER = "media-videos";

    private final MediaVideoRepository repo;
    private final String volumen;

    public MediaVideoController(MediaVideoRepository repo) {
        this.repo = repo;
        this.volumen = findVolumenPath();
    }

    // â”€â”€ Listado â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping
    public List<MediaVideo> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaVideo> getOne(@PathVariable String id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // â”€â”€ Stream (soporta Range requests para <video>) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/{id}/stream")
    public ResponseEntity<?> stream(
            @PathVariable String id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {

        return repo.findById(id).map(vid -> {
            try {
                Path file = resolveFilePath(vid);
                if (file == null || !Files.exists(file)) {
                    return ResponseEntity.notFound().build();
                }

                long fileSize  = Files.size(file);
                String mimeStr = resolveMime(vid.getMimeType(), vid.getFileName());
                MediaType mime;
                try { mime = MediaType.parseMediaType(mimeStr); }
                catch (Exception e) { mime = MediaType.APPLICATION_OCTET_STREAM; }

                String encoded = URLEncoder.encode(
                        vid.getFileName() != null ? vid.getFileName() : "file",
                        StandardCharsets.UTF_8).replace("+", "%20");

                if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                    String rangeVal = rangeHeader.substring("bytes=".length());
                    String[] parts  = rangeVal.split("-");
                    long start = Long.parseLong(parts[0]);
                    long end   = parts.length > 1 && !parts[1].isEmpty()
                            ? Long.parseLong(parts[1])
                            : Math.min(start + 2L * 1024 * 1024 - 1, fileSize - 1);
                    if (end >= fileSize) end = fileSize - 1;
                    long length = end - start + 1;

                    byte[] data = new byte[(int) length];
                    try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
                        raf.seek(start);
                        raf.readFully(data);
                    }

                    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                            .header(HttpHeaders.CONTENT_RANGE,
                                    "bytes " + start + "-" + end + "/" + fileSize)
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .contentType(mime)
                            .contentLength(length)
                            .body((Object) new InputStreamResource(new ByteArrayInputStream(data)));
                }

                return ResponseEntity.ok()
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .contentType(mime)
                        .contentLength(fileSize)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename*=UTF-8''" + encoded)
                        .body((Object) new InputStreamResource(new FileInputStream(file.toFile())));

            } catch (IOException e) {
                return ResponseEntity.internalServerError().build();
            }
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // â”€â”€ Thumbnail â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<?> thumbnail(@PathVariable String id) {
        return repo.findById(id).map(vid -> {
            if (vid.getThumbnail() == null) return ResponseEntity.notFound().build();
            try {
                Path file = Paths.get(volumen, vid.getThumbnail());
                if (!Files.exists(file)) return ResponseEntity.notFound().build();
                String probe = Files.probeContentType(file);
                MediaType mime = (probe != null) ? MediaType.parseMediaType(probe) : MediaType.IMAGE_JPEG;
                return ResponseEntity.ok()
                        .contentType(mime)
                        .contentLength(Files.size(file))
                        .body((Object) new InputStreamResource(new FileInputStream(file.toFile())));
            } catch (IOException e) {
                return ResponseEntity.internalServerError().build();
            }
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // â”€â”€ Subida multipart â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "displayName", required = false) String displayName,
            @RequestParam(value = "title",       required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category",    required = false) String category,
            @RequestParam(value = "duration",    required = false) String duration,
            @RequestParam(value = "uploadedBy",  required = false) String uploadedBy) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Fichero vacÃ­o");
        }

        String originalName = Paths.get(
                Objects.requireNonNullElse(file.getOriginalFilename(), "file")).getFileName().toString();
        originalName = fixEncoding(originalName);

        String lower = originalName.toLowerCase();
        String ext = lower.contains(".") ? lower.substring(lower.lastIndexOf('.') + 1) : "";
        List<String> blocked = List.of("zip", "rar", "7z", "tar", "gz", "tgz", "bz2");
        if (blocked.contains(ext)) {
            return ResponseEntity.badRequest().body("Tipo de fichero no permitido: " + ext);
        }

        try {
            MediaVideo rec = new MediaVideo();
            rec.setFileName(originalName);
            String effectiveName = displayName != null && !displayName.isBlank()
                    ? fixEncoding(displayName)
                    : (title != null && !title.isBlank() ? fixEncoding(title) : originalName);
            rec.setDisplayName(effectiveName);
            rec.setTitle(effectiveName);
            rec.setDescription(description != null ? fixEncoding(description) : null);
            rec.setCategory(category != null ? fixEncoding(category) : null);
            rec.setDuration(duration);
            rec.setSizeBytes(file.getSize());
            rec.setMimeType(file.getContentType());
            rec.setUploadedBy(uploadedBy);
            rec.setCreatedAt(LocalDateTime.now());
            rec.setUpdatedAt(LocalDateTime.now());
            rec = repo.save(rec);

            Path carpeta = Paths.get(volumen, SUBFOLDER, rec.getId());
            Files.createDirectories(carpeta);
            Path dest = carpeta.resolve(originalName);
            file.transferTo(dest.toFile());

            rec.setFilePath(SUBFOLDER + "/" + rec.getId() + "/" + originalName);
            rec.setFile(rec.getFilePath());

            if (thumbnail != null && !thumbnail.isEmpty()) {
                String thumbName = Paths.get(
                        Objects.requireNonNullElse(thumbnail.getOriginalFilename(), "thumb")).getFileName().toString();
                thumbName = fixEncoding(thumbName);
                Path thumbDest = carpeta.resolve("thumb_" + thumbName);
                thumbnail.transferTo(thumbDest.toFile());
                rec.setThumbnail(SUBFOLDER + "/" + rec.getId() + "/thumb_" + thumbName);
            }

            rec = repo.save(rec);
            return ResponseEntity.ok(rec);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error guardando fichero: " + e.getMessage());
        }
    }

    // â”€â”€ Actualizar metadatos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMeta(
            @PathVariable String id,
            @RequestBody MediaVideo body) {
        return repo.findById(id).map(rec -> {
            if (body.getDisplayName() != null) {
                rec.setDisplayName(body.getDisplayName());
                rec.setTitle(body.getDisplayName());
            }
            if (body.getDescription() != null) rec.setDescription(body.getDescription());
            if (body.getCategory()    != null) rec.setCategory(body.getCategory());
            if (body.getDuration()    != null) rec.setDuration(body.getDuration());
            rec.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(repo.save(rec));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // â”€â”€ Eliminar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        return repo.findById(id).map(rec -> {
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
                System.err.println("No se pudo eliminar carpeta multimedia del volumen: " + e.getMessage());
            }
            repo.deleteById(id);
            return ResponseEntity.noContent().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // â”€â”€ Utilidades â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private Path resolveFilePath(MediaVideo vid) {
        if (vid.getFilePath() != null) {
            return Paths.get(volumen, vid.getFilePath());
        }
        return null; // registros legacy con asset path estÃ¡tico no se pueden servir desde aquÃ­
    }

    private static String resolveMime(String stored, String fileName) {
        // Si el tipo almacenado es específico, usarlo
        if (stored != null && !stored.isEmpty()
                && !stored.equalsIgnoreCase("application/octet-stream")) {
            return stored;
        }
        // Detectar de la extensión del nombre de fichero
        if (fileName != null) {
            String ext = fileName.toLowerCase();
            if (ext.endsWith(".mp4"))  return "video/mp4";
            if (ext.endsWith(".webm")) return "video/webm";
            if (ext.endsWith(".mov"))  return "video/quicktime";
            if (ext.endsWith(".avi"))  return "video/x-msvideo";
            if (ext.endsWith(".mkv"))  return "video/x-matroska";
            if (ext.endsWith(".ogv"))  return "video/ogg";
            if (ext.endsWith(".pdf"))  return "application/pdf";
        }
        return stored != null ? stored : "application/octet-stream";
    }

    private static String fixEncoding(String s) {
        if (s == null) return null;
        try {
            return new String(s.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

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
            if (Files.isDirectory(c)) {
                if (first == null) first = c;
                try {
                    if (nonEmpty == null && Files.list(c).findFirst().isPresent()) nonEmpty = c;
                } catch (IOException ignored) {}
            }
        }
        Path chosen = nonEmpty != null ? nonEmpty : first;
        if (chosen == null) {
            chosen = base.resolve("volumenDocumentos");
            try { Files.createDirectories(chosen); } catch (IOException ignored) {}
        }
        System.out.println("[MediaVideoController] volumen = " + chosen);
        return chosen.toString();
    }
}
