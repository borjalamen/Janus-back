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

import org.springframework.beans.factory.annotation.Value;

import com.janushub.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/profile/image")
@RequiredArgsConstructor
public class ProfileImageController {

    private final UserService userService;

    @Value("${upload.root:uploads}")
    private String uploadRoot;

    // ---------- SUBIR AVATAR ----------
    @PostMapping
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile imageFile,
            @RequestParam("username") String username) {

        System.out.println("[ProfileImageController] POST /api/profile/image - username: " + username);
        System.out.println("[ProfileImageController] uploadRoot configurado: " + uploadRoot);

        if (username == null || username.isBlank()) {
            System.err.println("[ProfileImageController] ERROR: username vacío o nulo");
            return ResponseEntity.badRequest().body("Username requerido");
        }

        if (imageFile == null || imageFile.isEmpty()) {
            System.err.println("[ProfileImageController] ERROR: archivo vacío");
            return ResponseEntity.badRequest().body("No se ha seleccionado ningún archivo");
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            System.err.println("[ProfileImageController] ERROR: tipo no permitido - " + contentType);
            return ResponseEntity.badRequest().body("Formato no permitido. Solo imágenes");
        }

        String extension = FilenameUtils.getExtension(imageFile.getOriginalFilename());
        if (extension == null || extension.isBlank()) {
            extension = "png";
        }

        try {
            // 0) Asegurar que el directorio base existe
            Path baseDir = Paths.get(uploadRoot).toAbsolutePath();
            if (!Files.exists(baseDir)) {
                System.out.println("[ProfileImageController] Creando directorio base: " + baseDir);
                Files.createDirectories(baseDir);
            }

            // 1) esborrar avatar antic si existeix
            String oldRelativePath = userService.getAvatarPath(username);
            if (oldRelativePath != null && !oldRelativePath.isBlank()) {
                Path oldFile = resolveAvatarPath(oldRelativePath);
                if (Files.exists(oldFile)) {
                    System.out.println("[ProfileImageController] Eliminando avatar anterior: " + oldFile);
                    Files.deleteIfExists(oldFile);
                }
            }

            // 2) carpeta del usuario dentro de uploads
            Path userDir = baseDir.resolve(username);
            if (!Files.exists(userDir)) {
                System.out.println("[ProfileImageController] Creando directorio usuario: " + userDir);
                Files.createDirectories(userDir);
            }

            // 3) Nombre fijo
            String fileName = "avatar." + extension.toLowerCase();
            Path targetPath = userDir.resolve(fileName);
            System.out.println("[ProfileImageController] Guardando avatar en: " + targetPath);

            // 4) guardar nuevo avatar
            Files.copy(imageFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 5) Guardar ruta RELATIVA en BD (username/avatar.ext)
            String relativePath = username + "/" + fileName;
            userService.updateAvatar(username, relativePath);

            System.out.println("[ProfileImageController] Avatar guardado correctamente: " + relativePath);
            return ResponseEntity.ok("Imagen de perfil guardada correctamente");

        } catch (IOException e) {
            System.err.println("[ProfileImageController] ERROR IOException al guardar avatar:");
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error al guardar la imagen: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ProfileImageController] ERROR inesperado:");
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Error inesperado: " + e.getMessage());
        }
    }

    // ---------- OBTENER AVATAR ----------
    @GetMapping
    public ResponseEntity<Resource> getImage(@RequestParam("username") String username) throws IOException {
        if (username == null || username.isBlank()) {
            System.err.println("[ProfileImageController] GET: username vacío");
            return ResponseEntity.badRequest().build();
        }

        String storedPath = userService.getAvatarPath(username);
        if (storedPath == null || storedPath.isBlank()) {
            // No es un error: simplemente el usuario no tiene avatar
            System.out.println("[ProfileImageController] Usuario '" + username + "' no tiene avatar configurado");
            return ResponseEntity.notFound().build();
        }

        Path filePath = resolveAvatarPath(storedPath);
        if (!Files.exists(filePath)) {
            System.err.println("[ProfileImageController] Avatar registrado pero archivo no existe: " + filePath);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());
        String detectedType = Files.probeContentType(filePath);
        MediaType mediaType = (detectedType != null)
                ? MediaType.parseMediaType(detectedType)
                : MediaType.IMAGE_PNG;

        System.out.println("[ProfileImageController] Sirviendo avatar de '" + username + "': " + filePath);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }

    // ---------- ELIMINAR AVATAR ----------
    @DeleteMapping
    public ResponseEntity<?> deleteImage(@RequestParam("username") String username) throws IOException {
        String storedPath = userService.getAvatarPath(username);
        if (storedPath != null) {
            Path filePath = resolveAvatarPath(storedPath);
            Files.deleteIfExists(filePath);
            userService.removeAvatar(username);
        }

        return ResponseEntity.ok("Imagen eliminada correctamente");
    }

    /**
     * Resuelve la ruta almacenada en BD a una Path absoluta.
     * Soporta tanto rutas relativas nuevas ("username/avatar.png")
     * como rutas absolutas antiguas ("/app/shared-data/..." o "C:\...").
     */
    private Path resolveAvatarPath(String storedPath) {
        Path p = Paths.get(storedPath);
        if (p.isAbsolute()) {
            return p;
        }
        return Paths.get(uploadRoot).toAbsolutePath().resolve(storedPath);
    }
}
