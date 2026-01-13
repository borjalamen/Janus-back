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

/**
 * ═══════════════════════════════════════════════════════════════════════════════════
 * CONTROLADOR DE DOCUMENTOS (Document Management API)
 * ═══════════════════════════════════════════════════════════════════════════════════
 * 
 * Este controlador gestiona la subida, descarga, listado y eliminación de archivos.
 * Los archivos se almacenan en el sistema de archivos local en la carpeta 'volumenDocumentos'.
 * Cada proyecto tiene su propia carpeta identificada por 'idProyecto'.
 * 
 * BASE URL: http://localhost:8080/api/documentos
 * 
 * ESTRUCTURA DE CARPETAS:
 * -----------------------
 * volumenDocumentos/
 *   ├── proyecto123/
 *   │   ├── documento1.pdf
 *   │   └── imagen.jpg
 *   └── proyecto456/
 *       └── archivo.docx
 * 
 * ENDPOINTS DISPONIBLES:
 * ----------------------
 * 1. POST   /api/documentos/uploadDoc          - Subir documento a un proyecto
 * 2. GET    /api/documentos/getAllFolders      - Listar todos los proyectos (carpetas)
 * 3. GET    /api/documentos/getAllFiles        - Listar archivos de un proyecto
 * 4. GET    /api/documentos/getFile            - Descargar/previsualizar archivo
 * 5. GET    /api/documentos/getFileInfo        - Obtener información de un archivo
 * 6. GET    /api/documentos/getFolderInfo      - Obtener información de todos los archivos
 * 7. DELETE /api/documentos/deleteFile         - Eliminar un archivo específico
 * 8. DELETE /api/documentos/deleteAllFiles     - Eliminar todos los archivos de un proyecto
 * 9. PUT    /api/documentos/updateFile         - Actualizar/reemplazar un archivo
 * 
 * RESTRICCIONES:
 * --------------
 * - NO se permiten archivos comprimidos (zip, rar, 7z, tar, gz, bz2)
 * - Los archivos se validan tanto por extensión como por content-type
 * 
 * ═══════════════════════════════════════════════════════════════════════════════════
 */
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

/**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 1: SUBIR DOCUMENTO A UN PROYECTO
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: POST
     * URL: http://localhost:8080/api/documentos/uploadDoc
     * 
     * Descripción:
     * - Sube un archivo a la carpeta del proyecto especificado
     * - Crea la carpeta del proyecto si no existe
     * - Valida que el archivo NO sea comprimido (zip, rar, 7z, tar, gz, bz2)
     * - Reemplaza el archivo si ya existe con el mismo nombre
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: POST
     * URL: {{baseUrl}}/api/documentos/uploadDoc
     * Headers: (Content-Type se configura automáticamente como multipart/form-data)
     * 
     * Body (form-data):
     * -----------------
     * KEY              TYPE    VALUE
     * idProyecto       Text    proyecto123
     * documento        File    (seleccionar archivo desde tu computadora)
     * 
     * PASOS EN POSTMAN:
     * 1. Selecciona Body → form-data
     * 2. Añade una key "idProyecto" (tipo Text) con valor "proyecto123"
     * 3. Añade una key "documento" (tipo File) y selecciona el archivo
     * 4. Click en Send
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * "Documento subido correctamente"
     * 
     * RESPUESTA SI ES ARCHIVO COMPRIMIDO (400 BAD REQUEST):
     * ------------------------------------------------------
     * "No se permiten archivos comprimidos (zip, rar, 7z, tar, gz, bz2)"
     * 
     * RESPUESTA SI HAY ERROR (500 INTERNAL SERVER ERROR):
     * ----------------------------------------------------
     * "Error al subir el documento: [mensaje de error]"
     * 
     * ARCHIVOS BLOQUEADOS:
     * - Extensiones: .zip, .rar, .7z, .tar, .gz, .tgz, .bz2
     * - Content-Types: application/zip, application/x-rar, etc.
     * ═══════════════════════════════════════════════════════════════════════════════
     */
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

/**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 2: LISTAR TODOS LOS PROYECTOS (CARPETAS)
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: GET
     * URL: http://localhost:8080/api/documentos/getAllFolders
     * 
     * Descripción:
     * - Devuelve una lista con los nombres de todos los proyectos (carpetas)
     * - Solo lista carpetas, no archivos
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: GET
     * URL: {{baseUrl}}/api/documentos/getAllFolders
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * [
     *   "proyecto123",
     *   "proyecto456",
     *   "proyecto789"
     * ]
     * 
     * RESPUESTA SI NO HAY CARPETAS: []
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @GetMapping("/getAllFolders")
    public ResponseEntity<List<String>> getAllFolders() {
        File carpetaVolumen = new File(VOLUMEN);
        String[] carpetas = carpetaVolumen.list((current, name) -> new File(current, name).isDirectory());
        List<String> lista = carpetas != null ? List.of(carpetas) : new ArrayList<>();
        return ResponseEntity.ok(lista);
    }

/**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 3: LISTAR ARCHIVOS DE UN PROYECTO
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: GET
     * URL: http://localhost:8080/api/documentos/getAllFiles
     * 
     * Descripción:
     * - Devuelve una lista con los nombres de todos los archivos en un proyecto
     * - Solo lista archivos, no carpetas
     * - Retorna 404 si el proyecto no existe
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: GET
     * URL: {{baseUrl}}/api/documentos/getAllFiles?idProyecto=proyecto123
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * Parámetros Query (añadir a la URL):
     * - idProyecto: Nombre del proyecto (carpeta)
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * [
     *   "documento1.pdf",
     *   "imagen.jpg",
     *   "informe.docx"
     * ]
     * 
     * RESPUESTA SI NO HAY ARCHIVOS: []
     * RESPUESTA SI EL PROYECTO NO EXISTE (404 NOT FOUND): []
     * ═══════════════════════════════════════════════════════════════════════════════
     */
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

/**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 4: DESCARGAR O PREVISUALIZAR ARCHIVO
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: GET
     * URL: http://localhost:8080/api/documentos/getFile
     * 
     * Descripción:
     * - Descarga o previsualiza un archivo específico
     * - Archivos de imagen, video, PDF y texto se muestran inline (preview)
     * - Otros archivos se descargan como attachment
     * - Detecta automáticamente el content-type del archivo
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: GET
     * URL: {{baseUrl}}/api/documentos/getFile?idProyecto=proyecto123&nombreArchivo=documento.pdf
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * Parámetros Query (añadir a la URL):
     * - idProyecto: Nombre del proyecto (carpeta)
     * - nombreArchivo: Nombre completo del archivo (con extensión)
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * [Contenido binario del archivo]
     * 
     * Headers de respuesta:
     * - Content-Type: image/jpeg, application/pdf, etc.
     * - Content-Disposition: inline o attachment
     * - Content-Length: tamaño en bytes
     * 
     * TIPOS DE ARCHIVO:
     * -----------------
     * Inline (preview):
     * - Imágenes: .jpg, .png, .gif, .svg, etc.
     * - Videos: .mp4, .webm, etc.
     * - PDF: .pdf
     * - Texto: .txt, .json, .xml, etc.
     * 
     * Attachment (descarga):
     * - Documentos: .docx, .xlsx, .pptx
     * - Otros archivos no listados arriba
     * 
     * VISUALIZAR EN POSTMAN:
     * ----------------------
     * 1. En Postman, después de enviar la petición
     * 2. Click en "Visualize" o "Preview" para ver el archivo
     * 3. Para imágenes/PDFs verás el contenido directamente
     * 4. Para otros archivos, usa "Save Response" → "Save to a file"
     * 
     * RESPUESTA SI NO EXISTE (404 NOT FOUND):
     * ----------------------------------------
     * "No se encuentra el archivo: [nombreArchivo]"
     * ═══════════════════════════════════════════════════════════════════════════════
     */
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

/**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 5: OBTENER INFORMACIÓN DE UN ARCHIVO
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: GET
     * URL: http://localhost:8080/api/documentos/getFileInfo
     * 
     * Descripción:
     * - Devuelve metadatos de un archivo específico sin descargarlo
     * - Información: nombre, tamaño, tipo de contenido, fecha de modificación
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: GET
     * URL: {{baseUrl}}/api/documentos/getFileInfo?idProyecto=proyecto123&nombreArchivo=documento.pdf
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * Parámetros Query (añadir a la URL):
     * - idProyecto: Nombre del proyecto (carpeta)
     * - nombreArchivo: Nombre completo del archivo (con extensión)
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * {
     *   "name": "documento.pdf",
     *   "size": 1048576,
     *   "contentType": "application/pdf",
     *   "lastModified": "2025-12-15T14:30:00Z"
     * }
     * 
     * CAMPOS DE RESPUESTA:
     * - name: Nombre del archivo
     * - size: Tamaño en bytes (1048576 bytes = 1 MB)
     * - contentType: Tipo MIME del archivo
     * - lastModified: Fecha y hora de última modificación (ISO 8601)
     * 
     * RESPUESTA SI NO EXISTE (404 NOT FOUND):
     * ----------------------------------------
     * "No se encuentra el archivo: [nombreArchivo]"
     * ═══════════════════════════════════════════════════════════════════════════════
     */
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

/**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 6: OBTENER INFORMACIÓN DE TODOS LOS ARCHIVOS DE UN PROYECTO
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: GET
     * URL: http://localhost:8080/api/documentos/getFolderInfo
     * 
     * Descripción:
     * - Devuelve metadatos de TODOS los archivos en un proyecto
     * - Para cada archivo: nombre, tamaño, tipo de contenido, última modificación
     * - Útil para mostrar un listado completo con detalles
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: GET
     * URL: {{baseUrl}}/api/documentos/getFolderInfo?idProyecto=proyecto123
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * Parámetros Query (añadir a la URL):
     * - idProyecto: Nombre del proyecto (carpeta)
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * [
     *   {
     *     "name": "documento1.pdf",
     *     "size": 1048576,
     *     "contentType": "application/pdf",
     *     "lastModified": "2025-12-15T14:30:00Z"
     *   },
     *   {
     *     "name": "imagen.jpg",
     *     "size": 524288,
     *     "contentType": "image/jpeg",
     *     "lastModified": "2025-12-15T15:20:00Z"
     *   }
     * ]
     * 
     * RESPUESTA SI NO HAY ARCHIVOS: []
     * RESPUESTA SI EL PROYECTO NO EXISTE (404 NOT FOUND): []
     * ═══════════════════════════════════════════════════════════════════════════════
     */
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

/**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 7: ELIMINAR UN ARCHIVO ESPECÍFICO
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: DELETE
     * URL: http://localhost:8080/api/documentos/deleteFile
     * 
     * Descripción:
     * - Elimina un archivo específico de un proyecto
     * - Eliminación física (permanente)
     * - Retorna 404 si el archivo no existe
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: DELETE
     * URL: {{baseUrl}}/api/documentos/deleteFile?idProyecto=proyecto123&nombreArchivo=documento.pdf
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * Parámetros Query (añadir a la URL):
     * - idProyecto: Nombre del proyecto (carpeta)
     * - nombreArchivo: Nombre completo del archivo a eliminar (con extensión)
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * "Archivo eliminado correctamente"
     * 
     * RESPUESTA SI NO EXISTE (404 NOT FOUND):
     * ----------------------------------------
     * "No se encuentra el archivo: [nombreArchivo]"
     * 
     * RESPUESTA SI HAY ERROR (500 INTERNAL SERVER ERROR):
     * ----------------------------------------------------
     * "Error al eliminar el archivo: [mensaje de error]"
     * 
     * ⚠️ ADVERTENCIA: Esta operación elimina permanentemente el archivo
     * ═══════════════════════════════════════════════════════════════════════════════
     */
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

/**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 8: ELIMINAR TODOS LOS ARCHIVOS DE UN PROYECTO
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: DELETE
     * URL: http://localhost:8080/api/documentos/deleteAllFiles
     * 
     * Descripción:
     * - Elimina TODOS los archivos de un proyecto
     * - Eliminación física (permanente)
     * - NO elimina la carpeta del proyecto, solo su contenido
     * - Retorna el número de archivos eliminados
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: DELETE
     * URL: {{baseUrl}}/api/documentos/deleteAllFiles?idProyecto=proyecto123
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * Parámetros Query (añadir a la URL):
     * - idProyecto: Nombre del proyecto (carpeta)
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * "Archivos eliminados: 5"
     * 
     * RESPUESTA SI EL PROYECTO NO EXISTE (404 NOT FOUND):
     * ----------------------------------------------------
     * "No se encuentra la carpeta del proyecto: [idProyecto]"
     * 
     * RESPUESTA SI HAY ERROR (500 INTERNAL SERVER ERROR):
     * ----------------------------------------------------
     * "Error al eliminar archivos: [mensaje de error]"
     * 
     * ⚠️ ADVERTENCIA: Esta operación elimina permanentemente TODOS los archivos
     * ⚠️ La carpeta del proyecto permanece vacía
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @DeleteMapping("/deleteAllFiles")
    public ResponseEntity<String> deleteAllFiles(@RequestParam String idProyecto) {
        try {
            Path carpetaProyecto = Paths.get(VOLUMEN, idProyecto);
            if (!Files.exists(carpetaProyecto) || !Files.isDirectory(carpetaProyecto)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No se encuentra la carpeta del proyecto: " + idProyecto);
            }

            int deleted = 0;
            try (java.util.stream.Stream<Path> stream = Files.list(carpetaProyecto)) {
                for (Path entry : (Iterable<Path>) stream::iterator) {
                    try {
                        if (Files.isRegularFile(entry)) {
                            Files.delete(entry);
                            deleted++;
                        }
                    } catch (IOException ex) {
                        // Log and continue deleting other files
                        ex.printStackTrace();
                    }
                }
            }

            return ResponseEntity.ok("Archivos eliminados: " + deleted);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar archivos: " + e.getMessage());
        }
    }

/**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 9: ACTUALIZAR/REEMPLAZAR UN ARCHIVO
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: PUT
     * URL: http://localhost:8080/api/documentos/updateFile
     * 
     * Descripción:
     * - Reemplaza un archivo existente con uno nuevo
     * - Elimina el archivo antiguo y guarda el nuevo
     * - El nuevo archivo puede tener un nombre diferente
     * - Valida que el nuevo archivo NO sea comprimido
     * - Retorna 404 si el archivo original no existe
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: PUT
     * URL: {{baseUrl}}/api/documentos/updateFile
     * Headers: (Content-Type se configura automáticamente como multipart/form-data)
     * 
     * Body (form-data):
     * -----------------
     * KEY              TYPE    VALUE
     * idProyecto       Text    proyecto123
     * nombreArchivo    Text    documento_viejo.pdf (archivo a reemplazar)
     * documento        File    (seleccionar nuevo archivo desde tu computadora)
     * 
     * PASOS EN POSTMAN:
     * 1. Selecciona Body → form-data
     * 2. Añade key "idProyecto" (tipo Text) con valor "proyecto123"
     * 3. Añade key "nombreArchivo" (tipo Text) con el nombre del archivo a reemplazar
     * 4. Añade key "documento" (tipo File) y selecciona el nuevo archivo
     * 5. Click en Send
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * "Archivo reemplazado correctamente. Nuevo nombre: documento_nuevo.pdf"
     * 
     * RESPUESTA SI EL ARCHIVO ORIGINAL NO EXISTE (404 NOT FOUND):
     * ------------------------------------------------------------
     * "No se encuentra el archivo a reemplazar: [nombreArchivo]"
     * 
     * RESPUESTA SI ES ARCHIVO COMPRIMIDO (400 BAD REQUEST):
     * ------------------------------------------------------
     * "No se permiten archivos comprimidos (zip, rar, 7z, tar, gz, bz2)"
     * 
     * RESPUESTA SI HAY ERROR (500 INTERNAL SERVER ERROR):
     * ----------------------------------------------------
     * "Error al actualizar el documento: [mensaje de error]"
     * 
     * NOTAS IMPORTANTES:
     * ------------------
     * - El archivo antiguo se elimina completamente
     * - El nuevo archivo puede tener un nombre diferente al original
     * - Si subes "informe_v2.pdf" para reemplazar "informe_v1.pdf",
     *   el resultado será "informe_v2.pdf" (se usa el nombre del archivo subido)
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @PutMapping("/updateFile")
    public ResponseEntity<String> updateFile(@RequestParam String idProyecto,
                                             @RequestParam String nombreArchivo,
                                             @RequestParam MultipartFile documento) {
        try {
            Path carpetaProyecto = Paths.get(VOLUMEN, idProyecto);
            if (!Files.exists(carpetaProyecto)) {
                Files.createDirectories(carpetaProyecto);
            }
            // Replace behavior: delete the existing target file, then save the uploaded file
            Path rutaDestinoAntiguo = carpetaProyecto.resolve(nombreArchivo);
            if (!Files.exists(rutaDestinoAntiguo) || !Files.isRegularFile(rutaDestinoAntiguo)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No se encuentra el archivo a reemplazar: " + nombreArchivo);
            }

            // Validate uploaded file (block compressed types)
            List<String> blockedExt = List.of("zip", "rar", "7z", "tar", "gz", "tgz", "bz2");
            String uploadedOriginal = Paths.get(documento.getOriginalFilename()).getFileName().toString();
            String uploadedLower = uploadedOriginal.toLowerCase();
            String uploadedExt = null;
            int idx = uploadedLower.lastIndexOf('.');
            if (idx > -1 && idx < uploadedLower.length() - 1) {
                uploadedExt = uploadedLower.substring(idx + 1);
            }

            String uploadedContentType = documento.getContentType();
            boolean isBlockedByMime = false;
            if (uploadedContentType != null) {
                String ct = uploadedContentType.toLowerCase();
                if (ct.contains("zip") || ct.contains("compressed") || ct.contains("gzip") || ct.contains("x-7z") || ct.contains("x-rar")) {
                    isBlockedByMime = true;
                }
            }

            if ((uploadedExt != null && blockedExt.contains(uploadedExt)) || isBlockedByMime) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No se permiten archivos comprimidos (zip, rar, 7z, tar, gz, bz2)");
            }

            // Delete the old file first
            try {
                Files.delete(rutaDestinoAntiguo);
            } catch (IOException ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No se pudo eliminar el archivo antiguo: " + ex.getMessage());
            }

            // Save the uploaded file as a new file (keeping the uploaded file's original name)
            Path nuevaRuta = carpetaProyecto.resolve(uploadedOriginal);
            try (InputStream in = documento.getInputStream()) {
                Files.copy(in, nuevaRuta, StandardCopyOption.REPLACE_EXISTING);
            }

            return ResponseEntity.ok("Archivo reemplazado correctamente. Nuevo nombre: " + uploadedOriginal);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar el documento: " + e.getMessage());
        }
    }
}
