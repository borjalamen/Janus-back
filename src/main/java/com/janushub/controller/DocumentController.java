package com.janushub.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documentos")
public class DocumentController {

    private String VOLUMEN;

    public DocumentController() {
        this.VOLUMEN = findVolumenPath();
    }

    /**
     * Busca dinámicamente la carpeta 'volumenDocumentos' en la ruta de ejecución,
     * o en el directorio actual. Si no existe, la crea en la raíz del proyecto.
     */
    private String findVolumenPath() {
        // 1. Buscar en la ruta de ejecución (user.dir)
        String userDir = System.getProperty("user.dir");
        Path volumenPath = Paths.get(userDir, "volumenDocumentos");
        if (Files.exists(volumenPath) && Files.isDirectory(volumenPath)) {
            return volumenPath.toString();
        }

        // 2. Buscar en directorios padres (hasta 3 niveles)
        Path currentPath = Paths.get(userDir);
        for (int i = 0; i < 3; i++) {
            volumenPath = currentPath.resolve("volumenDocumentos");
            if (Files.exists(volumenPath) && Files.isDirectory(volumenPath)) {
                return volumenPath.toString();
            }
            currentPath = currentPath.getParent();
            if (currentPath == null) break;
        }

        // 3. Si no existe, crearla en el directorio actual
        volumenPath = Paths.get(userDir, "volumenDocumentos");
        try {
            Files.createDirectories(volumenPath);
        } catch (IOException e) {
            System.err.println("No se pudo crear volumenDocumentos: " + e.getMessage());
        }
        return volumenPath.toString();
    }

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

            // Bloquear archivos comprimidos por extensión y por content-type
            List<String> blockedExt = List.of("zip", "rar", "7z", "tar", "gz", "tgz", "bz2");
            String lower = original.toLowerCase();
            String ext = null;
            int i = lower.lastIndexOf('.');
            if (i > -1 && i < lower.length() - 1) {
                ext = lower.substring(i + 1);
            }

            String contentType = documento.getContentType();
            boolean isBlockedByMime = false;
            if (contentType != null) {
                String ct = contentType.toLowerCase();
                if (ct.contains("zip") || ct.contains("compressed") || ct.contains("gzip") || ct.contains("x-7z") || ct.contains("x-rar")) {
                    isBlockedByMime = true;
                }
            }

            if ((ext != null && blockedExt.contains(ext)) || isBlockedByMime) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No se permiten archivos comprimidos (zip, rar, 7z, tar, gz, bz2)");
            }

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

    // ----------------- GET FILE INFO -----------------
    @GetMapping("/getFileInfo")
    public ResponseEntity<?> getFileInfo(@RequestParam String idProyecto,
                                         @RequestParam String nombreArchivo) {
        try {
            Path rutaArchivo = Paths.get(VOLUMEN, idProyecto, nombreArchivo);
            if (!Files.exists(rutaArchivo) || !Files.isRegularFile(rutaArchivo)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No se encuentra el archivo: " + nombreArchivo);
            }

            String contentType = Files.probeContentType(rutaArchivo);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            Map<String, Object> info = new HashMap<>();
            info.put("name", nombreArchivo);
            info.put("size", Files.size(rutaArchivo));
            info.put("contentType", contentType);
            info.put("lastModified", Files.getLastModifiedTime(rutaArchivo).toString());

            return ResponseEntity.ok(info);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener info del archivo: " + e.getMessage());
        }
    }

    // ----------------- GET FOLDER INFO -----------------
    @GetMapping("/getFolderInfo")
    public ResponseEntity<List<Map<String, Object>>> getFolderInfo(@RequestParam String idProyecto) {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            Path carpetaProyecto = Paths.get(VOLUMEN, idProyecto);
            if (!Files.exists(carpetaProyecto) || !Files.isDirectory(carpetaProyecto)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(lista);
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(carpetaProyecto)) {
                for (Path entry : stream) {
                    if (Files.isRegularFile(entry)) {
                        Map<String, Object> info = new HashMap<>();
                        info.put("name", entry.getFileName().toString());
                        info.put("size", Files.size(entry));
                        String contentType = Files.probeContentType(entry);
                        info.put("contentType", contentType == null ? "application/octet-stream" : contentType);
                        info.put("lastModified", Files.getLastModifiedTime(entry).toString());
                        lista.add(info);
                    }
                }
            }

            return ResponseEntity.ok(lista);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(lista);
        }
    }

    // ----------------- DELETE FILE -----------------
    @DeleteMapping("/deleteFile")
    public ResponseEntity<String> deleteFile(@RequestParam String idProyecto,
                                             @RequestParam String nombreArchivo) {
        try {
            Path rutaArchivo = Paths.get(VOLUMEN, idProyecto, nombreArchivo);
            if (!Files.exists(rutaArchivo) || !Files.isRegularFile(rutaArchivo)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No se encuentra el archivo: " + nombreArchivo);
            }

            Files.delete(rutaArchivo);
            return ResponseEntity.ok("Archivo eliminado correctamente");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el archivo: " + e.getMessage());
        }
    }

    // ----------------- UPDATE / OVERWRITE FILE -----------------
    @PutMapping("/updateFile")
    public ResponseEntity<String> updateFile(@RequestParam String idProyecto,
                                             @RequestParam String nombreArchivo,
                                             @RequestParam MultipartFile documento) {
        try {
            Path carpetaProyecto = Paths.get(VOLUMEN, idProyecto);
            if (!Files.exists(carpetaProyecto)) {
                Files.createDirectories(carpetaProyecto);
            }

            // Validación: same blocked compressed extensions as upload
            List<String> blockedExt = List.of("zip", "rar", "7z", "tar", "gz", "tgz", "bz2");
            String lower = nombreArchivo.toLowerCase();
            String ext = null;
            int i = lower.lastIndexOf('.');
            if (i > -1 && i < lower.length() - 1) {
                ext = lower.substring(i + 1);
            }

            String contentType = documento.getContentType();
            boolean isBlockedByMime = false;
            if (contentType != null) {
                String ct = contentType.toLowerCase();
                if (ct.contains("zip") || ct.contains("compressed") || ct.contains("gzip") || ct.contains("x-7z") || ct.contains("x-rar")) {
                    isBlockedByMime = true;
                }
            }

            if ((ext != null && blockedExt.contains(ext)) || isBlockedByMime) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No se permiten archivos comprimidos (zip, rar, 7z, tar, gz, bz2)");
            }

            Path rutaArchivo = carpetaProyecto.resolve(nombreArchivo);
            try (InputStream in = documento.getInputStream()) {
                Files.copy(in, rutaArchivo, StandardCopyOption.REPLACE_EXISTING);
            }

            return ResponseEntity.ok("Documento sobrescrito correctamente");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar el documento: " + e.getMessage());
        }
    }
}
