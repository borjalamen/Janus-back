package com.janushub.controller;

import com.janushub.model.Formacion;
import com.janushub.repository.FormacionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════════
 * CONTROLADOR DE FORMACIÓN (Formation/Training API)
 * ═══════════════════════════════════════════════════════════════════════════════════
 * 
 * Este controlador gestiona todas las operaciones CRUD sobre formaciones/cursos.
 * Permite crear, leer, actualizar, eliminar y buscar formaciones.
 * 
 * BASE URL: http://localhost:8080/api/formacion
 * 
 * ENDPOINTS DISPONIBLES:
 * ----------------------
 * 1. GET    /api/formacion/all                  - Obtener todas las formaciones activas
 * 2. GET    /api/formacion/{id}                 - Obtener una formación por ID
 * 3. GET    /api/formacion/search/{name}        - Buscar formaciones activas por nombre
 * 4. POST   /api/formacion/create               - Crear nueva formación
 * 5. PUT    /api/formacion/update/{id}          - Actualizar formación existente
 * 6. DELETE /api/formacion/delete/{id}          - Eliminar formación (borrado lógico)
 * 7. DELETE /api/formacion/delete/physical/{id} - Eliminar formación (borrado físico)
 * 
 * MODELO DE DATOS:
 * ----------------
 * {
 *   "id": "string (MongoDB ObjectId)",
 *   "name": "string",
 *   "link": "string (URL)",
 *   "description": "string",
 *   "tags": ["string", "string", ...],
 *   "location": "string"
 * }
 * ═══════════════════════════════════════════════════════════════════════════════════
 */
@RestController
@RequestMapping("/api/formacion")
public class FormacionController {

    private final FormacionRepository repository;

    public FormacionController(FormacionRepository repository) {
        this.repository = repository;
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 1: OBTENER TODAS LAS FORMACIONES ACTIVAS
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: GET
     * URL: http://localhost:8080/api/formacion/all
     * 
     * Descripción:
     * - Devuelve una lista de todas las formaciones ACTIVAS en la base de datos
     * - Solo muestra formaciones con active=true (no eliminadas lógicamente)
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: GET
     * URL: {{baseUrl}}/api/formacion/all
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * [
     *   {
     *     "id": "67890abcdef123456",
     *     "name": "Curso de Spring Boot",
     *     "link": "https://udemy.com/spring-boot",
     *     "description": "Curso completo de Spring Boot",
     *     "tags": ["Java", "Spring", "Backend"],
     *     "location": "Online",
     *     "active": true
     *   },
     *   ...
     * ]
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @GetMapping("/all")
   public List<Formacion> getAllFormations() {
    return repository.findByDeletedFalseAndVisibleTrue();
}
    
    /**
     * ENDPOINT DEBUG: Ver todas las formaciones sin filtro (incluye inactivas)
     */
    @GetMapping("/all-debug")
    public List<Formacion> getAllFormationsDebug() {
        return repository.findAll();
    }
    
    /**
     * ENDPOINT DEBUG: Arreglar todos los registros para que deleted=false
     */
    @GetMapping("/fix-all")
    public String fixAllFormations() {
        List<Formacion> all = repository.findAll();
        for (Formacion f : all) {
            if (f.getDeleted() == null || f.getDeleted()) {
                f.setDeleted(false);
            }
            if (f.getVisible() == null) {
                f.setVisible(true);
            }
        }
        repository.saveAll(all);
        return "Fixed " + all.size() + " formations";
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 2: OBTENER UNA FORMACIÓN POR ID
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: GET
     * URL: http://localhost:8080/api/formacion/{id}
     * 
     * Descripción:
     * - Devuelve una formación específica por su ID
     * - Retorna 404 si no existe
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: GET
     * URL: {{baseUrl}}/api/formacion/67890abcdef123456
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * Parámetros de ruta:
     * - id: El ID de MongoDB de la formación (24 caracteres hexadecimales)
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * {
     *   "id": "67890abcdef123456",
     *   "name": "Curso de Spring Boot",
     *   "link": "https://udemy.com/spring-boot",
     *   "description": "Curso completo de Spring Boot",
     *   "tags": ["Java", "Spring", "Backend"],
     *   "location": "Online"
     * }
     * 
     * RESPUESTA SI NO EXISTE (404 NOT FOUND): (vacío)
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @GetMapping("/{id}")
    public ResponseEntity<Formacion> getFormationById(@PathVariable String id) {
        return repository.findByIdAndDeletedFalse(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 3: CREAR NUEVA FORMACIÓN
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: POST
     * URL: http://localhost:8080/api/formacion/create
     * 
     * Descripción:
     * - Crea una nueva formación en la base de datos
     * - MongoDB genera automáticamente el ID
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: POST
     * URL: {{baseUrl}}/api/formacion/create
     * Headers: Content-Type: application/json
     * 
     * Body (raw JSON):
     * {
     *   "name": "Curso de Spring Boot Avanzado",
     *   "link": "https://udemy.com/spring-boot-advanced",
     *   "description": "Curso avanzado de Spring Boot con microservicios",
     *   "tags": ["Java", "Spring", "Microservicios", "Backend"],
     *   "location": "Online"
     * }
     * 
     * NOTA: No enviar el campo 'id', se genera automáticamente
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * {
     *   "id": "67890abcdef123456",
     *   "name": "Curso de Spring Boot Avanzado",
     *   "link": "https://udemy.com/spring-boot-advanced",
     *   "description": "Curso avanzado de Spring Boot con microservicios",
     *   "tags": ["Java", "Spring", "Microservicios", "Backend"],
     *   "location": "Online"
     * }
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @PostMapping("/create")
    public Formacion createFormation(@RequestBody Formacion formation) {

          String prefix = "Formacion-";

    Formacion last = repository.findTopByIdStartingWithOrderByIdDesc(prefix);

    int nextNumber = 1;

    if (last != null) {
        String lastId = last.getId().replace(prefix, "");
        nextNumber = Integer.parseInt(lastId) + 1;
    }
    String newId = prefix + String.format("%03d", nextNumber);

   

    formation.setId(newId);
        formation.setDeleted(false);
        formation.setVisible(true);
        formation.setDeletedAt(null);
        return repository.save(formation);
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 4: ACTUALIZAR FORMACIÓN EXISTENTE
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: PUT
     * URL: http://localhost:8080/api/formacion/update/{id}
     * 
     * Descripción:
     * - Actualiza una formación existente por su ID
     * - Retorna 404 si no existe
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: PUT
     * URL: {{baseUrl}}/api/formacion/update/67890abcdef123456
     * Headers: Content-Type: application/json
     * 
     * Parámetros de ruta:
     * - id: El ID de MongoDB de la formación a actualizar
     * 
     * Body (raw JSON):
     * {
     *   "name": "Curso de Spring Boot Actualizado",
     *   "link": "https://udemy.com/spring-boot-updated",
     *   "description": "Descripción actualizada del curso",
     *   "tags": ["Java", "Spring", "Backend", "REST API"],
     *   "location": "Online"
     * }
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * {
     *   "id": "67890abcdef123456",
     *   "name": "Curso de Spring Boot Actualizado",
     *   "link": "https://udemy.com/spring-boot-updated",
     *   "description": "Descripción actualizada del curso",
     *   "tags": ["Java", "Spring", "Backend", "REST API"],
     *   "location": "Online"
     * }
     * 
     * RESPUESTA SI NO EXISTE (404 NOT FOUND): (vacío)
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<Formacion> updateFormation(@PathVariable String id, @RequestBody Formacion formationDetails) {
    return repository.findByIdAndDeletedFalse(id)
            .map(formation -> {
                
                formation.setName(formationDetails.getName());
                formation.setLink(formationDetails.getLink());
                formation.setDescription(formationDetails.getDescription());
                formation.setTags(formationDetails.getTags());
                formation.setLocation(formationDetails.getLocation());

                Formacion updatedFormation = repository.save(formation);
                return ResponseEntity.ok(updatedFormation);
            })
            .orElse(ResponseEntity.notFound().build());
}

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 5A: ELIMINAR FORMACIÓN (BORRADO LÓGICO)
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: DELETE
     * URL: http://localhost:8080/api/formacion/delete/{id}
     * 
     * Descripción:
     * - Elimina LÓGICAMENTE una formación (marca active=false)
     * - El registro permanece en la base de datos pero no se mostrará en consultas
     * - Retorna 404 si no existe
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: DELETE
     * URL: {{baseUrl}}/api/formacion/delete/67890abcdef123456
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * Parámetros de ruta:
     * - id: El ID de MongoDB de la formación a eliminar
     * 
     * RESPUESTA ESPERADA (200 OK): (vacío)
     * RESPUESTA SI NO EXISTE (404 NOT FOUND): (vacío)
     * 
     * ℹ️ NOTA: Esta operación NO elimina permanentemente el registro
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteFormation(@PathVariable String id) {
        return repository.findByIdAndDeletedFalse(id)
                .map(formation -> {
                    formation.setDeleted(true); // Borrado lógico
                    formation.setVisible(false);
                    formation.setDeletedAt(java.time.LocalDateTime.now());
                    repository.save(formation);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 5B: ELIMINAR FORMACIÓN (BORRADO FÍSICO)
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: DELETE
     * URL: http://localhost:8080/api/formacion/delete/physical/{id}
     * 
     * Descripción:
     * - Elimina FÍSICAMENTE una formación de la base de datos
     * - El registro se elimina permanentemente
     * - Retorna 404 si no existe
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: DELETE
     * URL: {{baseUrl}}/api/formacion/delete/physical/67890abcdef123456
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * Parámetros de ruta:
     * - id: El ID de MongoDB de la formación a eliminar
     * 
     * RESPUESTA ESPERADA (200 OK): (vacío)
     * RESPUESTA SI NO EXISTE (404 NOT FOUND): (vacío)
     * 
     * ⚠️ ADVERTENCIA: Esta operación elimina permanentemente el registro
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @DeleteMapping("/delete/physical/{id}")
    public ResponseEntity<?> deleteFormationPhysical(@PathVariable String id) {
        return repository.findByIdAndDeletedFalse(id)
                .map(formation -> {
                    repository.delete(formation);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 6: BUSCAR FORMACIONES ACTIVAS POR NOMBRE
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: GET
     * URL: http://localhost:8080/api/formacion/search/{name}
     * 
     * Descripción:
     * - Busca formaciones ACTIVAS cuyo nombre contenga el texto especificado
     * - La búsqueda es case-insensitive (no distingue mayúsculas/minúsculas)
     * - Solo muestra formaciones con active=true (no eliminadas lógicamente)
     * - Retorna lista vacía si no encuentra coincidencias
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: GET
     * URL: {{baseUrl}}/api/formacion/search/spring
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * Parámetros de ruta:
     * - name: Texto a buscar en el nombre de las formaciones
     *         Ejemplo: "spring" encontrará "Spring Boot", "SPRING MVC", etc.
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * [
     *   {
     *     "id": "67890abcdef123456",
     *     "name": "Curso de Spring Boot",
     *     "link": "https://udemy.com/spring-boot",
     *     "description": "Curso completo de Spring Boot",
     *     "tags": ["Java", "Spring", "Backend"],
     *     "location": "Online",
     *     "active": true
     *   },
     *   {
     *     "id": "67890abcdef789012",
     *     "name": "Spring Cloud Microservices",
     *     "link": "https://udemy.com/spring-cloud",
     *     "description": "Microservicios con Spring Cloud",
     *     "tags": ["Java", "Spring", "Microservicios"],
     *     "location": "Online",
     *     "active": true
     *   }
     * ]
     * 
     * RESPUESTA SI NO HAY COINCIDENCIAS: []
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @GetMapping("/search/{name}")
    public List<Formacion> searchByName(@PathVariable String name) {
        return repository.findByNameContainingIgnoreCaseAndDeletedFalse(name);
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 7A: ELIMINAR TODAS LAS FORMACIONES (BORRADO LÓGICO)
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: DELETE
     * URL: http://localhost:8080/api/formacion/delete-all
     * 
     * Descripción:
     * - Elimina LÓGICAMENTE todas las formaciones (marca deleted=true)
     * - Los registros permanecen en la base de datos
     * 
     * ⚠️ ADVERTENCIA: Marca todas las formaciones como eliminadas
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @DeleteMapping("/delete-all")
    public ResponseEntity<String> deleteAllFormationsLogical() {
        List<Formacion> all = repository.findAll();
        for (Formacion f : all) {
            f.setDeleted(true);
            f.setVisible(false);
            f.setDeletedAt(java.time.LocalDateTime.now());
        }
        repository.saveAll(all);
        return ResponseEntity.ok("Deleted (logical) " + all.size() + " formations");
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 7B: ELIMINAR TODAS LAS FORMACIONES (BORRADO FÍSICO)
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: DELETE
     * URL: http://localhost:8080/api/formacion/delete-all/physical
     * 
     * Descripción:
     * - Elimina FÍSICAMENTE todas las formaciones de la base de datos
     * - Los registros se eliminan permanentemente
     * 
     * ⚠️ ADVERTENCIA: Esta operación elimina permanentemente todos los registros
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @DeleteMapping("/delete-all/physical")
    public ResponseEntity<String> deleteAllFormationsPhysical() {
        long count = repository.count();
        repository.deleteAll();
        return ResponseEntity.ok("Deleted (physical) " + count + " formations");
    }
}