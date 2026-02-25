package com.janushub.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.bson.Document;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/db")
public class DatabaseController {

   
    private final MongoTemplate mongoTemplate;

    // Directorio donde se almacenan los backups
    private static final String BACKUP_DIR = "backups";

    @Autowired
    public DatabaseController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        // Crear directorio de backups si no existe
        File dir = new File(BACKUP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Limita los backups de un tipo específico a un máximo de archivos.
     * Elimina los más antiguos si se supera el límite.
     */
    private void limitarBackups(String prefix, int maxBackups) {
        File dir = new File(BACKUP_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith(prefix) && name.endsWith(".json"));
        
        if (files == null || files.length <= maxBackups) {
            return;
        }

        // Ordenar por fecha de modificación (más antiguos primero)
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));

        // Eliminar los más antiguos hasta dejar solo maxBackups
        int toDelete = files.length - maxBackups;
        for (int i = 0; i < toDelete; i++) {
            files[i].delete();
        }
    }

    // =============================================================
    //           SECCIÓN: BORRADO FÍSICO
    // =============================================================

    /**
     * GET /api/db/buscar-coleccion/{nombre}
     * Busca una colección y devuelve info: existe, registros, inactivos
     */
    @GetMapping("/buscar-coleccion/{nombre}")
    public ResponseEntity<?> buscarColeccion(@PathVariable String nombre) {
        Set<String> collectionNames = mongoTemplate.getCollectionNames();
        boolean existe = collectionNames.contains(nombre);

        if (!existe) {
            return ResponseEntity.ok(Map.of(
                "existe", false,
                "registros", 0,
                "inactivos", Collections.emptyList()
            ));
        }

        // Contar registros totales
        long registros = mongoTemplate.getCollection(nombre).countDocuments();

        // Buscar registros inactivos (visible=false o activo=false o active=false)
        Query queryInactivos = new Query();
        queryInactivos.addCriteria(new Criteria().orOperator(
            Criteria.where("visible").is(false),
            Criteria.where("activo").is(false),
            Criteria.where("active").is(false)
        ));

        List<Map> inactivos = mongoTemplate.find(queryInactivos, Map.class, nombre);
        List<Map<String, Object>> inactivosList = inactivos.stream()
            .map(doc -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("_id", doc.get("_id") != null ? doc.get("_id").toString() : null);
                item.put("visible", doc.get("visible"));
                item.put("activo", doc.get("activo"));
                item.put("active", doc.get("active"));
                return item;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "existe", true,
            "registros", registros,
            "inactivos", inactivosList
        ));
    }

    /**
     * DELETE /api/db/coleccion/{nombre}
     * Elimina toda la colección
     */
    @DeleteMapping("/coleccion/{nombre}")
    public ResponseEntity<?> eliminarColeccion(@PathVariable String nombre) {
        Set<String> collectionNames = mongoTemplate.getCollectionNames();
        if (!collectionNames.contains(nombre)) {
            return ResponseEntity.notFound().build();
        }

        mongoTemplate.dropCollection(nombre);

        return ResponseEntity.ok(Map.of(
            "mensaje", "Colección '" + nombre + "' eliminada correctamente"
        ));
    }

    /**
     * POST /api/db/coleccion/{nombre}/borrar-registros
     * Body: {ids: [string]}
     * Elimina registros específicos seleccionados
     */
    @PostMapping("/coleccion/{nombre}/borrar-registros")
    public ResponseEntity<?> borrarRegistros(
            @PathVariable String nombre,
            @RequestBody Map<String, List<String>> body) {

        List<String> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Debe proporcionar una lista de IDs"));
        }

        Set<String> collectionNames = mongoTemplate.getCollectionNames();
        if (!collectionNames.contains(nombre)) {
            return ResponseEntity.notFound().build();
        }

        Query query = new Query(Criteria.where("_id").in(ids));
        long eliminados = mongoTemplate.remove(query, nombre).getDeletedCount();

        return ResponseEntity.ok(Map.of(
            "mensaje", "Registros eliminados de la colección '" + nombre + "'",
            "eliminados", eliminados
        ));
    }

    /**
     * POST /api/db/borrar-logs
     * Body: {fechaLimite: "2024-01-01"}
     * Elimina logs anteriores a la fecha límite
     */
    @PostMapping("/borrar-logs")
    public ResponseEntity<?> borrarLogs(@RequestBody Map<String, String> body) {
        String fechaLimiteStr = body.get("fechaLimite");
        if (fechaLimiteStr == null || fechaLimiteStr.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Debe proporcionar 'fechaLimite' (formato: YYYY-MM-DD)"));
        }

        LocalDate fechaLimite;
        try {
            fechaLimite = LocalDate.parse(fechaLimiteStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Formato de fecha inválido. Use YYYY-MM-DD"));
        }

        // Buscar en la colección "logbook" (bitácora) registros con fecha anterior
        LocalDateTime fechaLimiteDateTime = fechaLimite.atStartOfDay();
        Query query = new Query(Criteria.where("fecha").lt(fechaLimiteDateTime));
        long eliminados = mongoTemplate.remove(query, "logbook").getDeletedCount();

        return ResponseEntity.ok(Map.of(
            "mensaje", "Logs anteriores a " + fechaLimiteStr + " eliminados",
            "eliminados", eliminados
        ));
    }

    /**
     * DELETE /api/db/borrar-inactivos
     * Elimina TODOS los registros inactivos de la BD
     */
    @DeleteMapping("/borrar-inactivos")
    public ResponseEntity<?> borrarInactivos() {
        Set<String> collectionNames = mongoTemplate.getCollectionNames();
        long totalEliminados = 0;

        for (String colName : collectionNames) {
            // Ignorar colecciones del sistema
            if (colName.startsWith("system.")) continue;

            Query query = new Query();
            query.addCriteria(new Criteria().orOperator(
                Criteria.where("visible").is(false),
                Criteria.where("activo").is(false),
                Criteria.where("active").is(false)
            ));

            long eliminados = mongoTemplate.remove(query, colName).getDeletedCount();
            totalEliminados += eliminados;
        }

        return ResponseEntity.ok(Map.of(
            "mensaje", "Registros inactivos eliminados de toda la base de datos",
            "eliminados", totalEliminados
        ));
    }

    // =============================================================
    //           SECCIÓN: COLECCIONES
    // =============================================================

    /**
     * GET /api/db/colecciones
     * Lista todas las colecciones reales de la BD con su conteo de documentos
     */
    @GetMapping("/colecciones")
    public ResponseEntity<?> listarColecciones() {
        Set<String> collectionNames = mongoTemplate.getCollectionNames();
        List<Map<String, Object>> result = collectionNames.stream()
            .filter(name -> !name.startsWith("system."))
            .sorted()
            .map(name -> {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("id", name);
                info.put("registros", mongoTemplate.getCollection(name).countDocuments());
                return info;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // =============================================================
    //           SECCIÓN: BACKUP
    // =============================================================

    /**
     * GET /api/db/backups
     * Lista todos los backups disponibles
     */
    @GetMapping("/backups")
     public ResponseEntity<?> listarBackups() {
        File dir = new File(BACKUP_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Map<String, Object>> backups = Arrays.stream(files)
            .map(f -> {
                String name = f.getName();
                String id = name.replace(".json", "");

                // Parsear tipo y colecciones del nombre del archivo
                String tipo = "desconocido";
                List<String> colecciones = new ArrayList<>();

                if (name.startsWith("backup_completo_")) {
                    tipo = "completo";
                } else if (name.startsWith("backup_parcial_")) {
                    tipo = "parcial";
                    // Extraer colecciones: entre "backup_parcial_" y el timestamp "_YYYYMMDD_HHMMSS.json"
                    // formato: backup_parcial_COL1-COL2_20260225_161700.json
                    String rest = name.replace("backup_parcial_", "").replace(".json", "");
                    // Quitar el timestamp final (ej: _20260225_161700)
                    String colPart = rest.replaceAll("_\\d{8}_\\d{6}$", "");
                    if (!colPart.isBlank()) {
                        colecciones = Arrays.asList(colPart.split("-"));
                    }
                } else if (name.startsWith("backup_")) {
                    tipo = "coleccion";
                    String rest = name.replace("backup_", "").replace(".json", "");
                    String colPart = rest.replaceAll("_\\d{8}_\\d{6}$", "");
                    if (!colPart.isBlank()) colecciones = List.of(colPart);
                }

                long tamanoKb = Math.max(1, f.length() / 1024);

                Map<String, Object> info = new LinkedHashMap<>();
                info.put("id", id);
                info.put("fecha", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new Date(f.lastModified())));
                info.put("descripcion", name);
                info.put("tipo", tipo);
                info.put("colecciones", colecciones);
                info.put("tamanoKb", tamanoKb);
                return info;
            })
            .sorted((a, b) -> b.get("fecha").toString().compareTo(a.get("fecha").toString()))
            .collect(Collectors.toList());

        return ResponseEntity.ok(backups);
    }

    /**
     * POST /api/db/backup-coleccion
     * Body: {coleccion: "usuarios"}
     * Backup de una colección específica
     */
    @PostMapping("/backup-coleccion")
    public ResponseEntity<?> backupColeccion(@RequestBody Map<String, String> body) {
        String coleccion = body.get("coleccion");
        if (coleccion == null || coleccion.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Debe proporcionar el nombre de la 'coleccion'"));
        }

        Set<String> collectionNames = mongoTemplate.getCollectionNames();
        if (!collectionNames.contains(coleccion)) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<Map> docs = mongoTemplate.findAll(Map.class, coleccion);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "backup_" + coleccion + "_" + timestamp + ".json";
            File backupFile = new File(BACKUP_DIR, fileName);

            // Serializar a JSON manualmente
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"coleccion\": \"").append(coleccion).append("\",\n");
            sb.append("  \"fecha\": \"").append(LocalDateTime.now().toString()).append("\",\n");
            sb.append("  \"registros\": ").append(docs.size()).append(",\n");
            sb.append("  \"datos\": ").append(docs.toString()).append("\n");
            sb.append("}");

            try (FileWriter writer = new FileWriter(backupFile)) {
                writer.write(sb.toString());
            }

            return ResponseEntity.ok(Map.of(
                "mensaje", "Backup de colección '" + coleccion + "' creado",
                "archivo", fileName,
                "registros", docs.size()
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al crear backup: " + e.getMessage()));
        }
    }

    /**
     * POST /api/db/backup
     * Body: {colecciones: ["users", "logbook", ...]}
     * Backup de múltiples colecciones en un solo archivo JSON
     */
    @PostMapping("/backup")
    public ResponseEntity<?> backupMultiple(@RequestBody Map<String, List<String>> body) {
        List<String> colecciones = body.get("colecciones");
        if (colecciones == null || colecciones.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Debe proporcionar una lista de 'colecciones'"));
        }

        Set<String> collectionNames = mongoTemplate.getCollectionNames();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        // Incluir nombres de colecciones en el archivo para poder buscarlo
        String colNames = colecciones.stream()
            .filter(collectionNames::contains)
            .collect(Collectors.joining("-"));
        if (colNames.length() > 60) colNames = colNames.substring(0, 60);
        String fileName = "backup_parcial_" + colNames + "_" + timestamp + ".json";
        
        File backupFile = new File(BACKUP_DIR, fileName);

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"tipo\": \"backup_parcial\",\n");
            sb.append("  \"fecha\": \"").append(LocalDateTime.now().toString()).append("\",\n");
            sb.append("  \"colecciones\": {\n");

            List<String> coleccionesBackup = new ArrayList<>();
            boolean first = true;
            for (String colName : colecciones) {
                if (!collectionNames.contains(colName)) continue;
                List<Map> docs = mongoTemplate.findAll(Map.class, colName);
                if (!first) sb.append(",\n");
                sb.append("    \"").append(colName).append("\": ").append(docs.toString());
                coleccionesBackup.add(colName);
                first = false;
            }

            sb.append("\n  }\n}");

            try (FileWriter writer = new FileWriter(backupFile)) {
                writer.write(sb.toString());
            }

            // Limitar backups parciales a 5
            limitarBackups("backup_parcial_", 5);

            return ResponseEntity.ok(Map.of(
                "colecciones", coleccionesBackup,
                "archivo", fileName,
                "mensaje", "Backup de " + coleccionesBackup.size() + " colección(es) creado"
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al crear backup parcial: " + e.getMessage()));
        }
    }

    /**
     * POST /api/db/backup-completo
     * Backup de TODA la base de datos
     */
    @PostMapping("/backup-completo")
    public ResponseEntity<?> backupCompleto() {
        Set<String> collectionNames = mongoTemplate.getCollectionNames();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "backup_completo_" + timestamp + ".json";
        File backupFile = new File(BACKUP_DIR, fileName);

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"tipo\": \"backup_completo\",\n");
            sb.append("  \"fecha\": \"").append(LocalDateTime.now().toString()).append("\",\n");
            sb.append("  \"colecciones\": {\n");

            List<String> coleccionesBackup = new ArrayList<>();
            boolean first = true;
            for (String colName : collectionNames) {
                if (colName.startsWith("system.")) continue;

                List<Map> docs = mongoTemplate.findAll(Map.class, colName);
                if (!first) sb.append(",\n");
                sb.append("    \"").append(colName).append("\": ").append(docs.toString());
                coleccionesBackup.add(colName);
                first = false;
            }

            sb.append("\n  }\n}");

            try (FileWriter writer = new FileWriter(backupFile)) {
                writer.write(sb.toString());
            }

            return ResponseEntity.ok(Map.of(
                "colecciones", coleccionesBackup,
                "archivo", fileName
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al crear backup completo: " + e.getMessage()));
        }
    }

    // =============================================================
    //           SECCIÓN: RESTORE
    // =============================================================

    /**
     * POST /api/db/restore
     * Body: {backupId: string}
     * Restaura la BD a una versión anterior
     */
    @PostMapping("/restore")
    public ResponseEntity<?> restore(@RequestBody Map<String, String> body) {
        String backupId = body.get("backupId");
        if (backupId == null || backupId.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Debe proporcionar 'backupId'"));
        }

        File backupFile = new File(BACKUP_DIR, backupId + ".json");
        if (!backupFile.exists()) {
            return ResponseEntity.notFound().build();
        }
try {
            // Leer el archivo como texto y parsearlo como Document BSON (Extended JSON)
            String content = new String(Files.readAllBytes(backupFile.toPath()));
            Document backupDoc = Document.parse(content);

            // Obtener el mapa de colecciones del backup
            Document coleccionesDoc = (Document) backupDoc.get("colecciones");
            if (coleccionesDoc == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El archivo de backup no contiene colecciones válidas"));
            }

            // Si el frontend envió colecciones específicas, restaurar solo esas; si no, todas
            Object colParam = body.get("colecciones");
            Set<String> colsToRestore;
            if (colParam instanceof List && !((List<?>) colParam).isEmpty()) {
                colsToRestore = new HashSet<>((List<String>) colParam);
            } else {
                colsToRestore = coleccionesDoc.keySet();
            }

            List<String> restauradas = new ArrayList<>();
            List<String> errores = new ArrayList<>();

            for (String colName : colsToRestore) {
                Object colData = coleccionesDoc.get(colName);
                if (colData == null) {
                    errores.add(colName + " (no encontrada en backup)");
                    continue;
                }

                try {
                    List<Document> docs = (List<Document>) colData;

                    // Borrar todos los documentos de la colección (compatible con Atlas)
                    mongoTemplate.getCollection(colName).deleteMany(new Document());

                    if (!docs.isEmpty()) {
                        mongoTemplate.getCollection(colName).insertMany(docs);
                    }
                    restauradas.add(colName + " (" + docs.size() + " docs)");
                } catch (Exception ex) {
                    System.err.println("[RESTORE ERROR] Colección " + colName + ": " + ex.getMessage());
                    ex.printStackTrace();
                    errores.add(colName + ": " + ex.getMessage());
                }
            }

            Map<String, Object> resultado = new LinkedHashMap<>();
            if (errores.isEmpty()) {
                resultado.put("mensaje", "Restore completado correctamente");
            } else if (!restauradas.isEmpty()) {
                resultado.put("mensaje", "Restore completado con errores parciales");
            } else {
                // Todo falló: devolver 500
                resultado.put("mensaje", "Restore fallido");
                resultado.put("errores", errores);
                return ResponseEntity.internalServerError().body(resultado);
            }
            resultado.put("backupId", backupId);
            resultado.put("restauradas", restauradas);
            if (!errores.isEmpty()) {
                resultado.put("errores", errores);
            }
            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            System.err.println("[RESTORE FATAL] " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error al restaurar: " + e.getMessage()));
        }
    }

    /**
     * POST /api/db/borrado-fisico
     * Body: {tipos: [string]} - ej: ["usuarios", "documentos", "logs"]
     * Borrado físico por tipos de colección
     */
    @PostMapping("/borrado-fisico")
    public ResponseEntity<?> borradoFisico(@RequestBody Map<String, List<String>> body) {
        List<String> tipos = body.get("tipos");
        if (tipos == null || tipos.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Debe proporcionar una lista de 'tipos'"));
        }

        // Mapeo de nombres amigables a nombres de colección en MongoDB
        Map<String, String> tipoToColeccion = Map.of(
            "usuarios", "users",
            "documentos", "documents",
            "logs", "logbook",
            "formaciones", "formations",
            "procedimientos", "procedures",
            "proyectos", "projects",
            "planificacion", "planning",
            "infraestructura", "infraestructura",
            "jenkins", "jenkins",
            "multimedia", "media_videos"
        );

        Set<String> collectionNames = mongoTemplate.getCollectionNames();
        List<String> eliminados = new ArrayList<>();
        List<String> noEncontrados = new ArrayList<>();

        for (String tipo : tipos) {
            String colName = tipoToColeccion.getOrDefault(tipo.toLowerCase(), tipo);
            if (collectionNames.contains(colName)) {
                mongoTemplate.dropCollection(colName);
                eliminados.add(colName);
            } else {
                noEncontrados.add(tipo);
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Borrado físico completado");
        response.put("eliminados", eliminados);
        if (!noEncontrados.isEmpty()) {
            response.put("noEncontrados", noEncontrados);
        }

        return ResponseEntity.ok(response);
    }
}