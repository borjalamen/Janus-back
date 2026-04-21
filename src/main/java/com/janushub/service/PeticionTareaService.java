package com.janushub.service;

import com.janushub.model.PeticionTarea;
import com.janushub.repository.PeticionTareaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PeticionTareaService {

    private final PeticionTareaRepository repository;
    private final EmailNotificationService emailNotificationService;

    // Mètode antic (JSON) — el pots deixar sense usar o eliminar si vols
    public PeticionTarea crear(PeticionTarea p) {
        p.setId(null);
        p.setEstado("PENDIENTE");
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        p.setAdminComment(null);
        PeticionTarea saved = repository.save(p);
        emailNotificationService.sendTaskRequestSubmitted(saved, false);
        return saved;
    }

    // Nou mètode per crear peticions amb adjunts (FormData)
    public PeticionTarea crearConAdjuntos(
            String requesterName,
            String requesterEmail,
            String projectName,
            String projectCode,
            String jiraTask,
            String comments,
            String devopsAssignee,
            String deadlineIso,
            String deadlineTime,
            List<MultipartFile> files
            
    ) {
        PeticionTarea p = new PeticionTarea();

        p.setId(null);
        p.setRequesterName(requesterName);
        p.setRequesterEmail(requesterEmail);
        p.setProjectName(projectName);
        p.setProjectCode(projectCode);
        p.setJiraTask(jiraTask);
        p.setDevopsAssignee(devopsAssignee);
        p.setComments(comments);

        if (deadlineIso != null && !deadlineIso.isBlank()) {
            p.setDeadline(
                    LocalDateTime.ofInstant(
                            java.time.Instant.parse(deadlineIso),
                            ZoneId.systemDefault()
                    )
            );
        } else {
            p.setDeadline(null);
        }
        p.setDeadlineTime(deadlineTime);

        if (files != null && !files.isEmpty()) {
    try {
        Path uploadDir = Paths.get("C:/Users/USUARIO/Documents/GitHub/janus-back/uploads/peticiones-tareas");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        List<String> nombres = files.stream().map(file -> {
            String cleanName = file.getOriginalFilename();
            String storedName = System.currentTimeMillis() + "_" + cleanName;

            Path target = uploadDir.resolve(storedName);
            try {
                file.transferTo(target.toFile());
                return storedName;
            } catch (IOException e) {
                throw new RuntimeException("Error guardando fichero: " + cleanName, e);
            }
        }).collect(Collectors.toList());

        p.setAttachments(nombres);
    } catch (IOException e) {
        throw new RuntimeException("Error creando directorio de subida", e);
    }
} else {
    p.setAttachments(List.of());
}

        p.setEstado("PENDIENTE");
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        p.setAdminComment(null);

        PeticionTarea saved = repository.save(p);
        emailNotificationService.sendTaskRequestSubmitted(saved, false);
        return saved;
    }

    public List<PeticionTarea> getAll() {
        return repository.findAll();
    }

    public PeticionTarea approve(String id, String adminComment) {
        PeticionTarea p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PeticionTarea no encontrada: " + id));
        p.setEstado("APROBADA");
        p.setAdminComment(adminComment);
        p.setUpdatedAt(LocalDateTime.now());
        PeticionTarea updated = repository.save(p);
        emailNotificationService.sendTaskRequestStatusUpdate(updated);
        return updated;
    }

    public PeticionTarea reject(String id, String adminComment) {
        PeticionTarea p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PeticionTarea no encontrada: " + id));
        p.setEstado("RECHAZADA");
        p.setAdminComment(adminComment);
        p.setUpdatedAt(LocalDateTime.now());
        PeticionTarea updated = repository.save(p);
        emailNotificationService.sendTaskRequestStatusUpdate(updated);
        return updated;
    }

    public PeticionTarea start(String id, String adminComment) {
        PeticionTarea p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PeticionTarea no encontrada: " + id));

        String estadoActual = String.valueOf(p.getEstado()).toUpperCase().trim();
        if (!"APROBADA".equals(estadoActual)) {
            throw new IllegalStateException("Solo se puede iniciar una petición en estado APROBADA.");
        }

        p.setEstado("INICIADA");
        if (adminComment != null && !adminComment.isBlank()) {
            p.setAdminComment(adminComment);
        }
        p.setUpdatedAt(LocalDateTime.now());

        PeticionTarea updated = repository.save(p);
        emailNotificationService.sendTaskRequestStatusUpdate(updated);
        return updated;
    }

    public PeticionTarea finish(String id, String adminComment) {
        PeticionTarea p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PeticionTarea no encontrada: " + id));

        String estadoActual = String.valueOf(p.getEstado()).toUpperCase().trim();
        if (!"INICIADA".equals(estadoActual)) {
            throw new IllegalStateException("Solo se puede finalizar una petición en estado INICIADA.");
        }

        p.setEstado("FINALIZADA");
        if (adminComment != null && !adminComment.isBlank()) {
            p.setAdminComment(adminComment);
        }
        p.setUpdatedAt(LocalDateTime.now());

        PeticionTarea updated = repository.save(p);
        emailNotificationService.sendTaskRequestStatusUpdate(updated);
        return updated;
    }

    public void resendConfirmation(String id) {
        PeticionTarea p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PeticionTarea no encontrada: " + id));
        emailNotificationService.sendTaskRequestSubmitted(p, true);
    }

    // ================== NUEVO MÉTODO PARA PDF ==================
    public byte[] generarPDF(String id) {
        PeticionTarea p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PeticionTarea no encontrada: " + id));

        // TODO: aquí va la lógica real de generación del PDF a partir de 'p'
        // Ejemplo: incluir también p.getAttachments() en el contenido del PDF

        throw new UnsupportedOperationException("generarPDF(String id) no implementado todavía");
    }
    public PeticionTarea getById(String id) {
    return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("PeticionTarea no encontrada: " + id));
}

}
