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
import org.springframework.security.core.Authentication;

import com.janushub.service.UserService;

import lombok.RequiredArgsConstructor;




@RestController
@RequestMapping("/api/profile/cv")
@RequiredArgsConstructor
public class ProfileCVController {

    private final UserService userService;
    private final String uploadDir = "uploads/cv/";

    @PostMapping
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
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(Paths.get(uploadDir));

             String extension =
                    contentType.equals(MediaType.APPLICATION_PDF_VALUE) ? ".pdf" :
                    contentType.equals("application/msword") ? ".doc" : ".docx";
            String fileName = username + "_" + System.currentTimeMillis() + extension;
            Path filePath = Paths.get(uploadDir).resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            userService.updateCv(username, filePath.toString());

            return ResponseEntity.ok("CV guardado correctamente");

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Error al guardar el CV");
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
}
