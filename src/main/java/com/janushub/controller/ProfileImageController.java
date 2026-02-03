package com.janushub.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.janushub.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profile/image")
@RequiredArgsConstructor
public class ProfileImageController {

    private final UserService userService;
    private final String baseDir = "/app/assets/multimedia";

    // ---------- SUBIR AVATAR ----------
    @PostMapping
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile imageFile,
            @RequestParam("username") String username) {

        if (imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body("No se ha seleccionado ningún archivo");
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body("Formato no permitido. Solo imágenes");
        }

        String extension = FilenameUtils.getExtension(imageFile.getOriginalFilename());
        if (extension == null || extension.isBlank()) {
            extension = "png";
        }

        try {
            Path userDir = Paths.get(baseDir, username); // /app/assets/multimedia/{username}
            Files.createDirectories(userDir);

            String fileName = "avatar_" + System.currentTimeMillis() + "." + extension;
            Path path = userDir.resolve(fileName);

            Files.copy(imageFile.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            userService.updateAvatar(username, path.toString());

            return ResponseEntity.ok("Imagen de perfil guardada correctamente");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error al guardar la imagen de perfil");
        }
    }

    // ---------- OBTENER AVATAR ----------
    @GetMapping
    public ResponseEntity<Resource> getImage(@RequestParam("username") String username) throws IOException {
        String avatarPath = userService.getAvatarPath(username);
        if (avatarPath == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(avatarPath);
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());
        String detectedType = Files.probeContentType(filePath);
        MediaType mediaType = (detectedType != null)
                ? MediaType.parseMediaType(detectedType)
                : MediaType.IMAGE_PNG;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }

    // ---------- ELIMINAR AVATAR ----------
    @DeleteMapping
    public ResponseEntity<?> deleteImage(@RequestParam("username") String username) throws IOException {
        String avatarPath = userService.getAvatarPath(username);
        if (avatarPath != null) {
            Path filePath = Paths.get(avatarPath);
            Files.deleteIfExists(filePath);
            userService.removeAvatar(username);
        }

        return ResponseEntity.ok("Imagen eliminada correctamente");
    }
}
