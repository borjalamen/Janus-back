package com.janushub.controller;

import com.janushub.model.Department;
import com.janushub.repository.DepartmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(
    origins = "http://localhost:4200",
    allowCredentials = "true"
)
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    public DepartmentController(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllDepartments() {
        try {
            List<Department> departments = departmentRepository.findAll();
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error obtenint departments: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDepartmentById(@PathVariable String id) {
        try {
            return departmentRepository.findById(id)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Departament no trobat"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error obtenint department per id: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody Department department) {
        try {
            if (department.getName() == null || department.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El nom del departament és obligatori");
            }

            String cleanName = department.getName().trim();

            if (departmentRepository.findByNameIgnoreCase(cleanName).isPresent()) {
                return ResponseEntity.badRequest().body("Aquest departament ja existeix");
            }

            Department newDepartment = new Department();
            newDepartment.setName(cleanName);

            Department saved = departmentRepository.save(newDepartment);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creant department: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable String id, @RequestBody Department department) {
        try {
            return departmentRepository.findById(id)
                    .<ResponseEntity<?>>map(existing -> {
                        if (department.getName() == null || department.getName().trim().isEmpty()) {
                            return ResponseEntity.badRequest().body("El nom del departament és obligatori");
                        }

                        String cleanName = department.getName().trim();

                        var duplicate = departmentRepository.findByNameIgnoreCase(cleanName);
                        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
                            return ResponseEntity.badRequest().body("Ja existeix un altre departament amb aquest nom");
                        }

                        existing.setName(cleanName);
                        Department updated = departmentRepository.save(existing);
                        return ResponseEntity.ok(updated);
                    })
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Departament no trobat"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error actualitzant department: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable String id) {
        try {
            return departmentRepository.findById(id)
                    .<ResponseEntity<?>>map(department -> {
                        departmentRepository.deleteById(id);
                        return ResponseEntity.ok("Departament eliminat correctament");
                    })
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Departament no trobat"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error eliminant department: " + e.getClass().getName() + " - " + e.getMessage());
        }
    }
}