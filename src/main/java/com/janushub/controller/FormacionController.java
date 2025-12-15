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
 * 1. GET    /api/formacion/all           - Obtener todas las formaciones
 * 2. GET    /api/formacion/{id}          - Obtener una formación por ID
 * 3. GET    /api/formacion/search/{name} - Buscar formaciones por nombre
 * 4. POST   /api/formacion/create        - Crear nueva formación
 * 5. PUT    /api/formacion/update/{id}   - Actualizar formación existente
 * 6. DELETE /api/formacion/delete/{id}   - Eliminar formación
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
     * ENDPOINT 1: OBTENER TODAS LAS FORMACIONES
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: GET
     * URL: http://localhost:8080/api/formacion/all
     * 
     * Descripción:
     * - Devuelve una lista de todas las formaciones en la base de datos
     * - No aplica filtros, devuelve todas las formaciones
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
     *     "location": "Online"
     *   },
     *   ...
     * ]
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @GetMapping("/all")
    public List<Formacion> getAllFormations() {
        return repository.findAll();
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
        return repository.findById(id)
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
    return repository.findById(id)
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
     * ENDPOINT 5: ELIMINAR FORMACIÓN
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: DELETE
     * URL: http://localhost:8080/api/formacion/delete/{id}
     * 
     * Descripción:
     * - Elimina FÍSICAMENTE una formación de la base de datos
     * - No hay borrado lógico, el registro se elimina permanentemente
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
     * ⚠️ ADVERTENCIA: Esta operación elimina permanentemente el registro
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteFormation(@PathVariable String id) {
        return repository.findById(id)
                .map(formation -> {
                    repository.delete(formation);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 6: BUSCAR FORMACIONES POR NOMBRE
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: GET
     * URL: http://localhost:8080/api/formacion/search/{name}
     * 
     * Descripción:
     * - Busca formaciones cuyo nombre contenga el texto especificado
     * - La búsqueda es case-insensitive (no distingue mayúsculas/minúsculas)
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
     *     "location": "Online"
     *   },
     *   {
     *     "id": "67890abcdef789012",
     *     "name": "Spring Cloud Microservices",
     *     "link": "https://udemy.com/spring-cloud",
     *     "description": "Microservicios con Spring Cloud",
     *     "tags": ["Java", "Spring", "Microservicios"],
     *     "location": "Online"
     *   }
     * ]
     * 
     * RESPUESTA SI NO HAY COINCIDENCIAS: []
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @GetMapping("/search/{name}")
    public List<Formacion> searchByName(@PathVariable String name) {
        return repository.findByNameContainingIgnoreCase(name);
    }
}