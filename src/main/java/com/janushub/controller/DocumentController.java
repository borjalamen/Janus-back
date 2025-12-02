package com.janushub.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
public class DocumentController {

    private final String VOLUMEN = "C:\\Users\\fredb\\Downloads\\Janus-back-conectividad-version\\Janus-back-conectividad-version\\volumenDocumentos"; // Cambia por tu ruta

    // ----------------- UPLOAD -----------------
    @PostMapping("/uploadDoc")
    public ResponseEntity<String> uploadDoc(@RequestParam String idProyecto,
                                            @RequestParam MultipartFile documento) {
        try {
            Path carpetaProyecto = Paths.get(VOLUMEN, idProyecto);
            if (!Files.exists(carpetaProyecto)) {
                Files.createDirectories(carpetaProyecto);
            }

            // Sanear nombre de fichero (opcional)
            String original = Paths.get(documento.getOriginalFilename()).getFileName().toString();
            Path rutaArchivo = carpetaProyecto.resolve(original);

            // Guardar archivo
            try (InputStream in = documento.getInputStream()) {
                Files.copy(in, rutaArchivo, StandardCopyOption.REPLACE_EXISTING);
            }

            return ResponseEntity.ok("Documento subido correctamente");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir el documento: " + e.getMessage());
        }
    }

    // ----------------- LIST FOLDERS -----------------
    @GetMapping("/getAllFolders")
    public ResponseEntity<List<String>> getAllFolders() {
        File carpetaVolumen = new File(VOLUMEN);
        String[] carpetas = carpetaVolumen.list((current, name) -> new File(current, name).isDirectory());
        List<String> lista = carpetas != null ? List.of(carpetas) : new ArrayList<>();
        return ResponseEntity.ok(lista);
    }

    // ----------------- LIST FILES IN PROJECT -----------------
    @GetMapping("/getAllFiles")
    public ResponseEntity<List<String>> getAllFiles(@RequestParam String idProyecto) {
        File carpetaProyecto = new File(VOLUMEN, idProyecto);
        if (!carpetaProyecto.exists() || !carpetaProyecto.isDirectory()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ArrayList<>());
        }
        String[] archivos = carpetaProyecto.list((current, name) -> new File(current, name).isFile());
        List<String> lista = archivos != null ? List.of(archivos) : new ArrayList<>();
        return ResponseEntity.ok(lista);
    }

    // ----------------- DOWNLOAD / PREVIEW FILE -----------------
    @GetMapping("/getFile")
    public ResponseEntity<?> getFile(@RequestParam String idProyecto,
                                     @RequestParam String nombreArchivo) {
        try {
            Path rutaArchivo = Paths.get(VOLUMEN, idProyecto, nombreArchivo);
            if (!Files.exists(rutaArchivo) || !Files.isRegularFile(rutaArchivo)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No se encuentra el archivo: " + nombreArchivo);
            }

            // Detectar content-type (puede devolver null)
            String contentType = Files.probeContentType(rutaArchivo);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Decidir inline (preview) o attachment (descarga)
            boolean preferInline = contentType.startsWith("image/")
                                  || contentType.startsWith("video/")
                                  || contentType.equals("application/pdf")
                                  || contentType.startsWith("text/");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            // Asegurar nombre con encoding para cabeceras
            String encodedFilename = URLEncoder.encode(nombreArchivo, "UTF-8").replaceAll("\\+", "%20");
            if (preferInline) {
                // Inline permite Preview en Postman y navegador
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + encodedFilename + "\"");
            } else {
                // Attachment fuerza descarga
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"");
            }

            long size = Files.size(rutaArchivo);
            headers.setContentLength(size);

            InputStreamResource resource = new InputStreamResource(Files.newInputStream(rutaArchivo, StandardOpenOption.READ));

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al leer el archivo: " + e.getMessage());
        }
    }
}
