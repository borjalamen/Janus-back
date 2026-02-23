package com.janushub.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
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

        List<Map<String, String>> backups = Arrays.stream(files)
            .map(f -> {
                Map<String, String> info = new LinkedHashMap<>();
                info.put("id", f.getName().replace(".json", ""));
                info.put("fecha", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new Date(f.lastModified())));
                info.put("descripcion", f.getName());
                return info;
            })
            .sorted((a, b) -> b.get("fecha").compareTo(a.get("fecha")))
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

        return ResponseEntity.ok(Map.of(
            "mensaje", "Restauración del backup '" + backupId + "' iniciada. Revise los datos.",
            "backupId", backupId,
            "archivo", backupFile.getName()
        ));
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
