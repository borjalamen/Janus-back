package com.janushub.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.janushub.model.Procedure;
import com.janushub.repository.ProceduresRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ═══════════════════════════════════════════════════════════════════════════════════
 * CONTROLADOR DE PROCEDIMIENTOS (Procedures API)
 * ═══════════════════════════════════════════════════════════════════════════════════
 * 
 * Este controlador gestiona todas las operaciones CRUD sobre los procedimientos.
 * Implementa el patrón de borrado lógico (Soft Delete) para mantener historial.
 * 
 * BASE URL: http://localhost:8080/api/procedures
 * 
 * ENDPOINTS DISPONIBLES:
 * ----------------------
 * 1. GET    /api/procedures                        - Obtener todos los procedimientos activos
 * 2. GET    /api/procedures/{id}                   - Obtener un procedimiento por ID
 * 3. GET    /api/procedures/search/{titulo}        - Buscar procedimientos por título
 * 4. POST   /api/procedures                        - Crear nuevo procedimiento
 * 5. PUT    /api/procedures/{id}                   - Actualizar procedimiento existente
 * 6. DELETE /api/procedures/{id}                   - Eliminar lógicamente un procedimiento
 * 7. DELETE /api/procedures/delete-all             - Eliminar lógicamente todos los procedimientos
 * 8. DELETE /api/procedures/physical/{id}          - Eliminar físicamente un procedimiento
 * 9. DELETE /api/procedures/physical-delete-all    - Eliminar físicamente todos (PELIGROSO)
 * 
 * MODELO DE DATOS:
 * ----------------
 * {
 *   "id": "string (MongoDB ObjectId)",
 *   "titulo": "string",
 *   "descripcion": "string",
 *   "departamento": "string",
 *   "tags": ["string", "string", ...],
 *   "steps": [
 *     {
 *       "id": "string",
 *       "titulo": "string",
 *       "descripcion": "string",
 *       "responsable": "string",
 *       "metodo": "string",
 *       "orden": number,
 *       "tags": ["string", ...]
 *     }
 *   ],
 *   "isVisible": boolean,
 *   "isDeleted": boolean,
 *   "createdAt": "ISO DateTime",
 *   "updatedAt": "ISO DateTime"
 * }
 * ═══════════════════════════════════════════════════════════════════════════════════
 */
@RestController
@RequestMapping("/api/procedures")
public class ProceduresController {

    @Autowired
    private ProceduresRepository repository; 

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 1: OBTENER TODOS LOS PROCEDIMIENTOS
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: GET
     * URL: http://localhost:8080/api/procedures
     * 
     * Descripción:
     * - Devuelve una lista de todos los procedimientos que NO están borrados.
     * - Solo muestra procedimientos donde isDeleted = false
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: GET
     * URL: {{baseUrl}}/api/procedures
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * [
     *   {
     *     "id": "67890abcdef123456",
     *     "title": "Procedimiento de Onboarding",
     *     "description": "Proceso para nuevos empleados",
     *     "department": "Recursos Humanos",
     *     "steps": ["Paso 1", "Paso 2", "Paso 3"],
     *     "isVisible": true,
     *     "isDeleted": false,
     *     "createdAt": "2025-12-15T10:30:00",
     *     "updatedAt": "2025-12-15T10:30:00"
     *   }
     * ]
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @GetMapping
    public List<Procedure> getAllProcedures() {
        return repository.findByIsDeletedFalse();
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 2: OBTENER UN PROCEDIMIENTO POR ID
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: GET
     * URL: http://localhost:8080/api/procedures/{id}
     * 
     * Descripción:
     * - Devuelve un procedimiento específico por su ID
     * - Solo si existe y NO está borrado (isDeleted = false)
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: GET
     * URL: {{baseUrl}}/api/procedures/67890abcdef123456
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * Parámetros de ruta:
     * - id: El ID de MongoDB del procedimiento (24 caracteres hexadecimales)
     * 
     * RESPUESTA ESPERADA (200 OK):
     * ----------------------------
     * {
     *   "id": "67890abcdef123456",
     *   "title": "Procedimiento de Onboarding",
     *   "description": "Proceso completo para nuevos empleados",
     *   "department": "Recursos Humanos",
     *   "steps": ["Crear cuenta", "Asignar permisos"],
     *   "isVisible": true,
     *   "isDeleted": false,
     *   "createdAt": "2025-12-15T10:30:00",
     *   "updatedAt": "2025-12-15T10:30:00"
     * }
     * 
     * RESPUESTA SI NO EXISTE (404 NOT FOUND): (vacío)
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @GetMapping("/{id}")
    public ResponseEntity<Procedure> getProcedure(@PathVariable String id) {
        Optional<Procedure> proc = repository.findById(id);
        
        // Verificamos que exista Y que NO esté borrado
        if (proc.isPresent() && !proc.get().isDeleted()) {
            return ResponseEntity.ok(proc.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 3: CREAR NUEVO PROCEDIMIENTO
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: POST
     * URL: http://localhost:8080/api/procedures
     * 
     * Descripción:
     * - Crea un nuevo procedimiento en la base de datos
     * - Inicializa automáticamente fechas de creación y actualización
     * - Marca el procedimiento como activo (isDeleted = false)
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: POST
     * URL: {{baseUrl}}/api/procedures
     * Headers: Content-Type: application/json
     * 
     * Body (raw JSON):
     * {
     *   "title": "Procedimiento de Onboarding",
     *   "description": "Procedimiento para dar de alta nuevos empleados",
     *   "department": "Recursos Humanos",
     *   "steps": [
     *     "Crear cuenta de usuario",
     *     "Asignar permisos",
     *     "Configurar correo electrónico"
     *   ],
     *   "isVisible": true
     * }
     * 
     * NOTA: No enviar 'id', 'isDeleted', 'createdAt' ni 'updatedAt'
     * 
     * RESPUESTA ESPERADA (201 CREATED):
     * {
     *   "id": "67890abcdef123456",
     *   "title": "Procedimiento de Onboarding",
     *   "isDeleted": false,
     *   "createdAt": "2025-12-15T15:30:00.123",
     *   "updatedAt": "2025-12-15T15:30:00.123"
     * }
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Procedure createProcedure(@RequestBody Procedure procedure) {
        procedure.setDeleted(false);
        procedure.setCreatedAt(LocalDateTime.now());
        procedure.setUpdatedAt(LocalDateTime.now()); 
        return repository.save(procedure);
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 4: ACTUALIZAR PROCEDIMIENTO EXISTENTE
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: PUT
     * URL: http://localhost:8080/api/procedures/{id}
     * 
     * Descripción:
     * - Actualiza un procedimiento existente por su ID
     * - Actualiza automáticamente el campo 'updatedAt'
     * - Solo actualiza si el procedimiento existe y NO está borrado
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: PUT
     * URL: {{baseUrl}}/api/procedures/67890abcdef123456
     * Headers: Content-Type: application/json
     * 
     * Body (raw JSON):
     * {
     *   "title": "Procedimiento Actualizado",
     *   "description": "Descripción actualizada",
     *   "department": "Recursos Humanos",
     *   "steps": ["Paso 1", "Paso 2"],
     *   "isVisible": true
     * }
     * 
     * RESPUESTA ESPERADA (200 OK):
     * {
     *   "id": "67890abcdef123456",
     *   "updatedAt": "2025-12-15T16:45:00.456"
     * }
     * 
     * RESPUESTA SI NO EXISTE (404 NOT FOUND): (vacío)
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @PutMapping("/{id}")
    public ResponseEntity<Procedure> updateProcedure(@PathVariable String id, @RequestBody Procedure newData) {
        Optional<Procedure> procOpt = repository.findById(id);

        if (procOpt.isPresent() && !procOpt.get().isDeleted()) {
            Procedure current = procOpt.get();
            
            // Actualización de campos
            current.setTitulo(newData.getTitulo());
            current.setDescripcion(newData.getDescripcion());
            current.setDepartamento(newData.getDepartamento());
            current.setTags(newData.getTags());
            current.setSteps(newData.getSteps());
            current.setVisible(newData.isVisible());
            
            // Actualizamos la fecha de modificación
            current.setUpdatedAt(LocalDateTime.now()); 
            
            return ResponseEntity.ok(repository.save(current));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 3: BUSCAR PROCEDIMIENTOS POR TÍTULO
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: GET
     * URL: http://localhost:8080/api/procedures/search/{titulo}
     * 
     * Descripción:
     * - Busca procedimientos activos cuyo título contenga el texto especificado
     * - La búsqueda es case-insensitive
     * - Solo muestra procedimientos con isDeleted=false
     * 
     * RESPUESTA ESPERADA (200 OK): Lista de procedimientos coincidentes
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @GetMapping("/search/{titulo}")
    public List<Procedure> searchByTitulo(@PathVariable String titulo) {
        return repository.findByTituloContainingIgnoreCaseAndIsDeletedFalse(titulo);
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 5: ELIMINAR PROCEDIMIENTO (BORRADO LÓGICO)
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: DELETE
     * URL: http://localhost:8080/api/procedures/{id}
     * 
     * Descripción:
     * - Realiza un BORRADO LÓGICO (Soft Delete)
     * - No elimina el registro físicamente de la base de datos
     * - Marca isDeleted = true e isVisible = false
     * - Permite recuperar el procedimiento en el futuro
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: DELETE
     * URL: {{baseUrl}}/api/procedures/67890abcdef123456
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * RESPUESTA ESPERADA (204 NO CONTENT): (vacío)
     * RESPUESTA SI NO EXISTE (404 NOT FOUND): (vacío)
     * 
     * NOTA: Después de eliminar, el procedimiento ya NO aparecerá en GET ALL
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteProcedure(@PathVariable String id) {
        Optional<Procedure> procOpt = repository.findById(id);

        if (procOpt.isPresent()) {
            Procedure proc = procOpt.get();
            proc.setDeleted(true);
            proc.setVisible(false);
            proc.setUpdatedAt(LocalDateTime.now());
            repository.save(proc);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 6: ELIMINAR TODOS LOS PROCEDIMIENTOS (BORRADO LÓGICO)
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: DELETE
     * URL: http://localhost:8080/api/procedures/delete-all
     * 
     * Descripción:
     * - Realiza un BORRADO LÓGICO de TODOS los procedimientos
     * - Marca isDeleted = true e isVisible = false en todos
     * - Los registros permanecen en la base de datos
     * 
     * ⚠️ ADVERTENCIA: Afecta a todos los procedimientos de la colección
     * 
     * RESPUESTA ESPERADA (200 OK): (vacío)
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @DeleteMapping("/delete-all")
    public ResponseEntity<Void> softDeleteAllProcedures() {
        List<Procedure> allProcedures = repository.findAll();
        LocalDateTime now = LocalDateTime.now();
        
        allProcedures.forEach(proc -> {
            proc.setDeleted(true);
            proc.setVisible(false);
            proc.setUpdatedAt(now);
        });
        
        repository.saveAll(allProcedures);
        return ResponseEntity.ok().build();
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 7: ELIMINAR PROCEDIMIENTO (BORRADO FÍSICO)
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * Método: DELETE
     * URL: http://localhost:8080/api/procedures/physical/{id}
     * 
     * Descripción:
     * - Elimina FÍSICAMENTE un procedimiento de la base de datos
     * - El registro se elimina permanentemente
     * 
     * ⚠️ ADVERTENCIA: Esta operación elimina permanentemente el registro
     * 
     * RESPUESTA ESPERADA (200 OK): (vacío)
     * RESPUESTA SI NO EXISTE (404 NOT FOUND): (vacío)
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @DeleteMapping("/physical/{id}")
    public ResponseEntity<Void> deletePhysicalProcedure(@PathVariable String id) {
        return repository.findById(id)
                .map(procedure -> {
                    repository.delete(procedure);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * ENDPOINT 8: ELIMINAR TODOS LOS PROCEDIMIENTOS (BORRADO FÍSICO)
     * ═══════════════════════════════════════════════════════════════════════════════
     * 
     * ⚠️ ADVERTENCIA: Este endpoint es PELIGROSO
     * ⚠️ Elimina PERMANENTEMENTE todos los procedimientos
     * ⚠️ NO hay forma de recuperar los datos
     * ⚠️ Solo usar en entornos de desarrollo/testing
     * 
     * Método: DELETE
     * URL: http://localhost:8080/api/procedures/physical-delete-all
     * 
     * POSTMAN - Configuración:
     * ------------------------
     * Method: DELETE
     * URL: {{baseUrl}}/api/procedures/physical-delete-all
     * Headers: Content-Type: application/json
     * Body: (ninguno)
     * 
     * RESPUESTA ESPERADA (204 NO CONTENT): (vacío)
     * ═══════════════════════════════════════════════════════════════════════════════
     */
    @DeleteMapping("/physical-delete-all")
    public ResponseEntity<Void> deleteAllPhysical() {
        repository.deleteAll();
        return ResponseEntity.noContent().build();
    }
}