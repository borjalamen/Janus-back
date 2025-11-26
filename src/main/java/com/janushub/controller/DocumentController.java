package com.janushub.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
public class DocumentController {

    private final String VOLUMEN = "C:\\Users\\fredb\\Downloads\\Janus-back-conectividad-version\\Janus-back-conectividad-version\\volumenDocumentos"; // Carpeta raíz

    // 1️⃣ Subir documento
    @PostMapping("/uploadDoc")
    public ResponseEntity<String> uploadDoc(@RequestParam String idProyecto,
                                            @RequestParam MultipartFile documento) {
        try {
            Path carpetaProyecto = Paths.get(VOLUMEN, idProyecto);
            if (!Files.exists(carpetaProyecto)) {
                Files.createDirectories(carpetaProyecto);
            }

            Path rutaArchivo = carpetaProyecto.resolve(documento.getOriginalFilename());
            documento.transferTo(rutaArchivo.toFile());

            return ResponseEntity.ok("Documento subido correctamente");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir el documento");
        }
    }

    // 2️⃣ Listar todas las carpetas (proyectos)
    @GetMapping("/getAllFolders")
    public ResponseEntity<List<String>> getAllFolders() {
        File carpetaVolumen = new File(VOLUMEN);
        String[] carpetas = carpetaVolumen.list((current, name) -> new File(current, name).isDirectory());
        List<String> lista = carpetas != null ? List.of(carpetas) : new ArrayList<>();
        return ResponseEntity.ok(lista);
    }

    // 3️⃣ Listar todos los archivos de un proyecto
    @GetMapping("/getAllFiles")
    public ResponseEntity<List<String>> getAllFiles(@RequestParam String idProyecto) {
        File carpetaProyecto = new File(VOLUMEN, idProyecto);
        String[] archivos = carpetaProyecto.list((current, name) -> new File(current, name).isFile());
        List<String> lista = archivos != null ? List.of(archivos) : new ArrayList<>();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/getFile")
    public ResponseEntity<byte[]> getFile(@RequestParam String idProyecto,
                                      @RequestParam String nombreArchivo) {
    try {
        Path rutaArchivo = Paths.get(VOLUMEN, idProyecto, nombreArchivo);
        if (!Files.exists(rutaArchivo)) {
            return ResponseEntity.notFound().build();
        }

        byte[] contenido = Files.readAllBytes(rutaArchivo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF); // Tipo MIME PDF
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"");

        return new ResponseEntity<>(contenido, headers, HttpStatus.OK);

    } catch (IOException e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

}
