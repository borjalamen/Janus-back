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

    private final String uploadDir = "/app/assets/multimedia/avatars/";

    @PostMapping
     public ResponseEntity<?> uploadImage(
            @RequestParam("username") String username,
            @RequestParam("file") MultipartFile file) {

                if (file.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body("No se ha seleccionado ningún archivo");
                }

                if (!file.getContentType().equals("image/jpeg") && 
                    !file.getContentType().equals("image/png")) {
                        return ResponseEntity.badRequest()
                                .body("Formato no permitido. Solo JPG o PNG");                    
                }

                try {
                    Files.createDirectories(Paths.get(uploadDir));
                    String fileExtension = contentType.equals(MediaType.IMAGE_PNG_VALUE) ? ".png" : ".jpg";
                    String fileName = username + "_" + System.currentTimeMillis() + extension;
                    Path filePath = Paths.get(uploadDir).resolve(fileName);

                    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    userService.updateAvatar(username, filePath.toString());

                    return ResponseEntity.ok("Imagen guardada correctamente");

                } catch (IOException e) {
                    return ResponseEntity.internalServerError()
                            .body("Error al guardar la imagen");
                }
            }

            @GetMapping
    public ResponseEntity<Resource> getImage(@RequestParam String username) throws IOException {
        String avatarPath = userService.getAvatarPath(username);

        if (avatarPath == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(avatarPath);


        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());
         MediaType mediaType = Files.probeContentType(path).equals(MediaType.IMAGE_PNG_VALUE)
                ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);        

    }

    @DeleteMapping
    public ResponseEntity<?> deleteImage(@RequestParam String username) throws IOException {
        String avatarPath = userService.getAvatarPath(username);

        if (avatarPath != null) {
            Files.deleteIfExists(Paths.get(avatarPath));
            userService.removeAvatar(username);
        }

        return ResponseEntity.ok("Imagen eliminada correctamente");
      
    }
}
           


        

        