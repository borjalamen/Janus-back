package com.janushub.controller;

import com.janushub.model.Bitacora;
import com.janushub.model.PeticionTarea;
import com.janushub.model.ScrumSprint;
import com.janushub.model.ScrumTask;
import com.janushub.model.Unete;
import com.janushub.repository.BitacoraRepository;
import com.janushub.repository.FormacionRepository;
import com.janushub.repository.HerramientaRepository;
import com.janushub.repository.PeticionTareaRepository;
import com.janushub.repository.ProjectRepository;
import com.janushub.repository.ProceduresRepository;
import com.janushub.repository.ScrumSprintRepository;
import com.janushub.repository.ScrumTaskRepository;
import com.janushub.repository.UneteRepository;
import com.janushub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Endpoint único que agrega todos los KPIs del dashboard home.
 * GET /api/dashboard/kpis
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ScrumSprintRepository sprintRepo;
    private final ScrumTaskRepository taskRepo;
    private final PeticionTareaRepository peticionRepo;
    private final UneteRepository uneteRepo;
    private final ProjectRepository projectRepo;
    private final BitacoraRepository bitacoraRepo;
    private final UserRepository userRepo;
    private final ProceduresRepository proceduresRepo;
    private final HerramientaRepository herramientaRepo;
    private final FormacionRepository formacionRepo;

    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Object>> getKpis() {
        Map<String, Object> kpis = new LinkedHashMap<>();

        // ── 1. SPRINT ACTIVO ──────────────────────────────────────────────────
        Optional<ScrumSprint> activeSprint = sprintRepo.findByActiveTrue();
        if (activeSprint.isPresent()) {
            ScrumSprint sprint = activeSprint.get();
            List<ScrumTask> sprintTasks = taskRepo.findBySprintIdAndVisibleTrue(sprint.getId());

            long todo  = sprintTasks.stream().filter(t -> "todo".equals(t.getStatus())).count();
            long doing = sprintTasks.stream().filter(t -> "doing".equals(t.getStatus())).count();
            long done  = sprintTasks.stream().filter(t -> "done".equals(t.getStatus())).count();
            long total = sprintTasks.size();

            double pct = total > 0 ? Math.round((done * 100.0 / total) * 10) / 10.0 : 0.0;

            Map<String, Object> sprintMap = new LinkedHashMap<>();
            sprintMap.put("id",         sprint.getId());
            sprintMap.put("sprintKey",  sprint.getSprintKey());
            sprintMap.put("startDate",  sprint.getStartDate());
            sprintMap.put("endDate",    sprint.getEndDate());
            sprintMap.put("totalTasks", total);
            sprintMap.put("todo",       todo);
            sprintMap.put("doing",      doing);
            sprintMap.put("done",       done);
            sprintMap.put("pct",        pct);
            sprintMap.put("totalHours", sprint.getTotalHours() != null ? sprint.getTotalHours() : 0);
            sprintMap.put("doneHours",  sprint.getDoneHours()  != null ? sprint.getDoneHours()  : 0);
            kpis.put("sprint", sprintMap);
        } else {
            kpis.put("sprint", null);
        }

        // ── 2. PETICIONES TAREAS ──────────────────────────────────────────────
        List<PeticionTarea> todasPeticiones = peticionRepo.findAll();
        Map<String, Long> peticionByEstado = todasPeticiones.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getEstado() != null ? p.getEstado() : "PENDIENTE",
                        Collectors.counting()
                ));

        // Últimas 5 peticiones (más recientes primero)
        List<Map<String, Object>> recentPeticiones = todasPeticiones.stream()
                .filter(p -> p.getCreatedAt() != null)
                .sorted(Comparator.comparing(PeticionTarea::getCreatedAt).reversed())
                .limit(5)
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",            p.getId());
                    m.put("requesterName", p.getRequesterName());
                    m.put("projectName",   p.getProjectName());
                    m.put("estado",        p.getEstado());
                    m.put("createdAt",     p.getCreatedAt().toString());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> peticionMap = new LinkedHashMap<>();
        peticionMap.put("total",      todasPeticiones.size());
        peticionMap.put("byEstado",   peticionByEstado);
        peticionMap.put("recent",     recentPeticiones);
        kpis.put("peticiones", peticionMap);

        // ── 3. SOLICITUDES ÚNETE ──────────────────────────────────────────────
        long joinPendientes = uneteRepo.findByEstado("PENDIENTE").size();
        long joinTotal      = uneteRepo.count();
        kpis.put("join", Map.of("total", joinTotal, "pendientes", joinPendientes));

        // ── 4. PROYECTOS ──────────────────────────────────────────────────────
        long totalProyectos = projectRepo.findByDeletedFalse().size();
        kpis.put("proyectos", Map.of("total", totalProyectos));

        // ── 5. BITÁCORA RECIENTE ──────────────────────────────────────────────
        List<Bitacora> bitacoras = bitacoraRepo.findAllVisible();
        List<Map<String, Object>> recentBitacora = bitacoras.stream()
                .filter(b -> b.getFecha() != null)
                .sorted(Comparator.comparing(Bitacora::getFecha).reversed())
                .limit(5)
                .map(b -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",     b.getId());
                    m.put("titulo", b.getTitulo());
                    m.put("entorno", b.getEntorno());
                    m.put("fecha",  b.getFecha().toString());
                    m.put("tags",   b.getTags() != null ? b.getTags() : List.of());
                    return m;
                })
                .collect(Collectors.toList());

        kpis.put("bitacora", Map.of("total", bitacoras.size(), "recent", recentBitacora));

        // ── 6. USUARIOS ───────────────────────────────────────────────────────
        long totalUsuarios = userRepo.count();
        kpis.put("usuarios", Map.of("total", totalUsuarios));

        // ── 7. TAREAS SCRUM SIN SPRINT (backlog) ──────────────────────────────
        long backlog = taskRepo.findBySprintIdIsNullAndVisibleTrue().size();
        kpis.put("backlog", Map.of("total", backlog));

        // ── 8. SOLICITUDES ÚNETE RECIENTES ────────────────────────────────────
        List<Unete> todasSolicitudes = uneteRepo.findAll();
        List<Map<String, Object>> recentSolicitudes = todasSolicitudes.stream()
                .filter(u -> u.getCreatedAt() != null)
                .sorted(Comparator.comparing(Unete::getCreatedAt).reversed())
                .limit(5)
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",        u.getId());
                    m.put("fullName",  u.getFullName());
                    m.put("email",     u.getEmail());
                    m.put("estado",    u.getEstado());
                    m.put("createdAt", u.getCreatedAt().toString());
                    return m;
                })
                .collect(Collectors.toList());
        kpis.put("solicitudes", Map.of(
                "total", todasSolicitudes.size(),
                "pendientes", joinPendientes,
                "recent", recentSolicitudes
        ));

        // ── 9. PROCEDIMIENTOS ─────────────────────────────────────────────────
        long totalProcedimientos = proceduresRepo.findByIsDeletedFalse().size();
        kpis.put("procedimientos", Map.of("total", totalProcedimientos));

        // ── 10. HERRAMIENTAS ──────────────────────────────────────────────────
        long totalHerramientas = herramientaRepo.findByVisibleTrue().size();
        kpis.put("herramientas", Map.of("total", totalHerramientas));

        // ── 11. FORMACIONES ───────────────────────────────────────────────────
        long totalFormaciones = formacionRepo.findByDeletedFalseAndVisibleTrue().size();
        kpis.put("formaciones", Map.of("total", totalFormaciones));

        return ResponseEntity.ok(kpis);
    }
}
