package com.janushub.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.janushub.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profile/cv")
@RequiredArgsConstructor
public class ProfileCVController {

    private final UserService userService;

    @Value("${upload.root:uploads}")
    private String uploadRoot;

    private static final List<String> CV_EXTENSIONS = List.of(".pdf", ".doc", ".docx");

    private Path resolveUserDir(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El parámetro 'username' es obligatorio");
        }

        Path baseDir = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path userDir = baseDir.resolve(username.trim()).normalize();

        if (!userDir.startsWith(baseDir)) {
            throw new IllegalArgumentException("Username inválido");
        }

        return userDir;
    }

    private Path resolveCvPath(Path userDir) throws IOException {
        for (String ext : CV_EXTENSIONS) {
            Path candidate = userDir.resolve("cv" + ext);
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        if (!Files.exists(userDir) || !Files.isDirectory(userDir)) {
            return null;
        }

        try (Stream<Path> files = Files.list(userDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.startsWith("cv.")
                                && (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx"));
                    })
                    .findFirst()
                    .orElse(null);
        }
    }

    private String resolveContentType(Path path) throws IOException {
        String contentType = Files.probeContentType(path);
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return contentType;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("username") String username) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No se ha seleccionado ningún archivo");
        }

        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals(MediaType.APPLICATION_PDF_VALUE) &&
                 !contentType.equals("application/msword") &&
                 !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {

            return ResponseEntity.badRequest()
                    .body("Formato no permitido. Solo PDF o DOC/DOCX");
        }

        try {
            Path userDir = resolveUserDir(username);
            Files.createDirectories(userDir);

            Path oldCvPath = resolveCvPath(userDir);
            if (oldCvPath != null) {
                Files.deleteIfExists(oldCvPath);
            }

            String extension =
                    contentType.equals(MediaType.APPLICATION_PDF_VALUE) ? ".pdf" :
                    contentType.equals("application/msword") ? ".doc" : ".docx";

            String fileName = "cv" + extension;
            Path filePath = userDir.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            userService.updateCv(username.trim(), filePath.toString());

            return ResponseEntity.ok("CV guardado correctamente");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Resource> getCv(@RequestParam String username) throws IOException {
        Path userDir = resolveUserDir(username);
        Path path = resolveCvPath(userDir);

        if (path == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(path.toUri());
        String contentType = resolveContentType(path);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + path.getFileName().toString() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteCv(@RequestParam String username) throws IOException {
        Path userDir = resolveUserDir(username);
        Path cvPath = resolveCvPath(userDir);

        if (cvPath != null) {
            Files.deleteIfExists(cvPath);
        }

        if (userService.existsByUsername(username.trim())) {
            userService.removeCv(username.trim());
        }

        return ResponseEntity.ok("CV eliminado correctamente");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        if (ex.getMessage() != null && ex.getMessage().startsWith("Usuario no encontrado")) {
            return ResponseEntity.status(404).body(ex.getMessage());
        }
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
