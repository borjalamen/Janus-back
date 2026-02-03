package com.janushub.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
    private final String baseDir = "/app/assets/multimedia";

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
        // 1) esborrar CV antic si existeix
        String oldCvPath = userService.getCvPath(username);
        if (oldCvPath != null && !oldCvPath.isBlank()) {
            Files.deleteIfExists(Paths.get(oldCvPath));
        }

        // 2) carpeta de l'usuari
        Path userDir = Paths.get(baseDir, username); // /app/assets/multimedia/{username}
        Files.createDirectories(userDir);

        // 3) extensió
        String extension =
                contentType.equals(MediaType.APPLICATION_PDF_VALUE) ? ".pdf" :
                contentType.equals("application/msword") ? ".doc" : ".docx";

        // 4) NOM FIX → sempre el mateix fitxer
        String fileName = "cv" + extension;          // NO timestamp
        Path filePath = userDir.resolve(fileName);   // .../{username}/cv.pdf

        // 5) guardar nou CV (sobreescriu si cal)
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        userService.updateCv(username, filePath.toString());

        return ResponseEntity.ok("CV guardado correctamente");
    } catch (IOException e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError().body(e.getMessage());
    }
}

    @GetMapping
    public ResponseEntity<Resource> getCv(@RequestParam String username) throws IOException {
        String cvPath = userService.getCvPath(username);

        if (cvPath == null) {
            return ResponseEntity.notFound().build();
        }

        Path path = Paths.get(cvPath);

        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(path.toUri());
        String contentType = Files.probeContentType(path);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteCv(@RequestParam String username) throws IOException {
        String cvPath = userService.getCvPath(username);

        if (cvPath != null) {
            Files.deleteIfExists(Paths.get(cvPath));
            userService.removeCv(username);
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
