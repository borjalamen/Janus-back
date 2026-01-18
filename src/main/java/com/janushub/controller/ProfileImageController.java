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

    private final String uploadDir = "uploads/avatars/";

    @PostMapping
     public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        String username = authentication.getName();

        // Validar formato
        if (!file.getContentType().equals("image/jpeg") &&
            !file.getContentType().equals("image/png")) {
            return ResponseEntity.badRequest()
                    .body("Formato no permitido. Solo JPG o PNG");
        }


        try {

            Files.createDirectories(Paths.get(uploadDir));
            String fileExtension = file.getContentType().equals("image/png") ? ".png" : ".jpg"; 
            String fileName = username + fileExtension;
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
    public ResponseEntity<Resource> getImage(Authentication authentication) throws IOException {
        String username = authentication.getName();
        String avatarPath = userService.getAvatarPath(username);

        if (avatarPath == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(avatarPath);
        Resource resource = new UrlResource(filePath.toUri());
       

       return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
}

        @DeleteMapping("/image")
    public ResponseEntity<?> deleteImage(Authentication authentication) throws IOException {
        String username = authentication.getName();
        String avatarPath = userService.getAvatarPath(username);

        if (avatarPath == null) {
            Files.deleteIfExists(Paths.get(avatarPath));
            userService.removeAvatar(username);
        }

        
        return ResponseEntity.ok("Imagen eliminada correctamente");
    }


}