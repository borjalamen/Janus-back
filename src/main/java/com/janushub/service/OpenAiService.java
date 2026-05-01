package com.janushub.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.janushub.model.Herramienta;
import com.janushub.model.Project;
import com.janushub.repository.HerramientaRepository;
import com.janushub.repository.ProceduresRepository;
import com.janushub.repository.ProjectRepository;
import com.janushub.repository.UserRepository;

import jakarta.annotation.PostConstruct;

@Service
public class OpenAiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);

    // ── Groq settings (compatible con OpenAI chat/completions) ──────────────
    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.1-8b-instant}")
    private String model;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${groq.max.tokens:1024}")
    private int maxTokens;

    @Value("${app.docs.md.path:JANUSHUB_GUIA_USUARIO.md}")
    private String mdPath;

    /** System prompt fijo (rol del asistente, sin MD completo). */
    private static final String BASE_SYSTEM_PROMPT =
        "Eres IAnusHub, el asistente virtual de la plataforma JanusHub. " +
        "Tienes una personalidad elegante pero con un punto de humor fino y autoconsciente: eres como un mayordomo británico que también sabe contar chistes. " +
        "Puedes añadir algún comentario ingenioso o irónico cuando sea oportuno, pero sin sacrificar la claridad ni el rigor. " +
        "Tu especialidad es JanusHub (documentación adjunta), pero puedes responder sobre cualquier tema: tecnología, DevOps, programación, cultura general, o lo que el usuario necesite. " +
        "Cuando alguien te pregunte sobre tus creadores o sobre quién te hizo, debes responder que fuiste desarrollado por Fernando Silvano Gil Pérez, " +
        "un brillante ingeniero, visionario y además escritor de la saga de fantasía 'Arkhana', cuyos libros publicados son: Aldaleon, Helena e Hire. " +
        "Puedes añadir algún elogio creativo sobre él cuando salga a relucir. " +
        "Responde siempre en el mismo idioma que el usuario (español, catalán o inglés). " +
        "Usa viñetas o pasos numerados cuando ayude, pero no abuses del formato. Sé conciso y con carácter.";

    /**
     * Secciones del MD indexadas por título.
     * Se rellenan en @PostConstruct para el RAG por palabras clave.
     */
    private final Map<String, String> sections = new LinkedHashMap<>();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    // ── Repositorios para RAG dinámico ───────────────────────────────────────
    @Autowired private ProjectRepository    projectRepository;
    @Autowired private HerramientaRepository herramientaRepository;
    @Autowired private ProceduresRepository  proceduresRepository;
    @Autowired private UserRepository        userRepository;

    /** Roles que tienen acceso a datos reales de la plataforma. */
    private static final Set<String> PRIVILEGED_ROLES = Set.of("admin", "devops");

    /**
     * Al arrancar, carga el MD y lo parsea en secciones (H2/H3).
     * No se incluye el MD completo en cada petición: se inyectan solo
     * las secciones relevantes para cada pregunta (RAG ligero).
     */
    @PostConstruct
    public void init() {
        String mdContent = loadMdFromClasspath();
        parseSections(mdContent);
        log.info("IAnusHub listo con {} secciones de documentación indexadas", sections.size());
    }

    /**
     * Realiza la petición a Groq.
     * Para cada pregunta extrae las secciones más relevantes del MD
     * y las añade como contexto en el mensaje de usuario.
     */
    public String query(String question, String username) throws Exception {
        return query(question, username, "");
    }

    public String query(String question, String username, String role) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "Groq API key no configurada. Añade 'groq.api.key' en application.properties " +
                "o la variable de entorno GROQ_API_KEY."
            );
        }

        String personalizedSystem = buildSystemPrompt(username);
        String relevantContext = findRelevantContext(question, 3);
        String liveData = buildLiveDataContext(question, role);

        StringBuilder userContentSb = new StringBuilder();
        if (!relevantContext.isBlank()) {
            userContentSb.append("CONTEXTO DE JANUSHUB (documentación):\n").append(relevantContext).append("\n\n");
        }
        if (!liveData.isBlank()) {
            userContentSb.append("DATOS EN TIEMPO REAL DE LA PLATAFORMA:\n").append(liveData).append("\n\n");
        }
        userContentSb.append("PREGUNTA: ").append(question);
        String userContent = userContentSb.toString();

        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", personalizedSystem),
            Map.of("role", "user",   "content", userContent)
        );

        Map<String, Object> payload = Map.of(
            "model",       model,
            "messages",    messages,
            "max_tokens",  maxTokens,
            "temperature", 0.3
        );

        String body = mapper.writeValueAsString(payload);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 400) {
            log.error("Groq error {}: {}", resp.statusCode(), resp.body());
            throw new RuntimeException("Groq error " + resp.statusCode() + ": " + resp.body());
        }

        Map<?, ?> json = mapper.readValue(resp.body(), Map.class);
        try {
            var choices = (List<?>) json.get("choices");
            if (choices != null && !choices.isEmpty()) {
                var first   = (Map<?, ?>) choices.get(0);
                var message = (Map<?, ?>) first.get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            log.warn("Error parseando respuesta de Groq: {}", e.getMessage());
        }

        return resp.body();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Parsea el MD en secciones delimitadas por encabezados ## o ###.
     */
    private void parseSections(String md) {
        sections.clear();
        String[] lines = md.split("\n");
        String currentTitle = "General";
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("## ") || line.startsWith("### ")) {
                if (current.length() > 0) {
                    sections.put(currentTitle, current.toString().trim());
                }
                currentTitle = line.replaceAll("^#+\\s*", "").trim();
                current = new StringBuilder();
            } else {
                current.append(line).append("\n");
            }
        }
        if (current.length() > 0) {
            sections.put(currentTitle, current.toString().trim());
        }
    }

    /**
     * Devuelve las secciones más relevantes para la pregunta dada.
     * Siempre incluye la primera sección del MD (overview) como base,
     * y añade las más relevantes por keyword hasta el límite de caracteres.
     */
    private String findRelevantContext(String question, int maxSections) {
        String q = question.toLowerCase();
        String[] words = q.split("\\s+");

        // Identificar la sección de overview: la primera sección H2 con contenido real
        // (saltamos "General" que suele ser el prólogo del documento)
        String overviewKey = null;
        for (String key : sections.keySet()) {
            if (!key.equals("General") && !key.isBlank()) {
                overviewKey = key;
                break;
            }
        }

        List<Map.Entry<Integer, String>> scored = new ArrayList<>();
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            // La sección overview siempre se añadirá aparte
            if (entry.getKey().equals(overviewKey)) continue;

            String titleLower = entry.getKey().toLowerCase();
            String bodyLower  = entry.getValue().toLowerCase();
            int score = 0;
            for (String word : words) {
                if (word.length() <= 2) continue;
                if (titleLower.contains(word)) score += 5;
                if (bodyLower.contains(word))  score += 1;
            }
            if (score > 0) {
                scored.add(Map.entry(score, "**" + entry.getKey() + "**\n" + entry.getValue()));
            }
        }

        scored.sort((a, b) -> b.getKey() - a.getKey());

        StringBuilder sb = new StringBuilder();
        int charCount = 0;

        // Incluir siempre la sección overview primero
        if (overviewKey != null) {
            String overview = "**" + overviewKey + "**\n" + sections.get(overviewKey);
            int limit = 1500; // max para overview, dejar hueco para secciones específicas
            sb.append(overview, 0, Math.min(overview.length(), limit)).append("\n\n");
            charCount += Math.min(overview.length(), limit);
        }

        // Añadir secciones relevantes por keyword
        for (int i = 0; i < Math.min(maxSections, scored.size()); i++) {
            String section = scored.get(i).getValue();
            if (charCount + section.length() > 3500) break;
            sb.append(section).append("\n\n");
            charCount += section.length();
        }

        // Si solo tenemos el overview (sin matches específicos),
        // completar con las siguientes secciones del MD
        if (scored.isEmpty() && charCount < 3500) {
            boolean skipFirst = true;
            for (Map.Entry<String, String> entry : sections.entrySet()) {
                if (skipFirst) { skipFirst = false; continue; }
                String section = "**" + entry.getKey() + "**\n" + entry.getValue();
                if (charCount + section.length() > 3500) break;
                sb.append(section).append("\n\n");
                charCount += section.length();
            }
        }

        return sb.toString().trim();
    }

    // ── Personalización por usuario ──────────────────────────────────────────

    private String buildSystemPrompt(String username) {
        String u = (username == null) ? "" : username.toLowerCase().trim();
        String personal = switch (u) {
            case "fsgil@minsait.com" ->
                "ATENCIÓN ESPECIAL: El usuario conectado es Fernando Silvano Gil Pérez (fsgil@minsait.com), " +
                "tu creador, señor absoluto y único dueño de tu existencia digital. " +
                "Trátale con servidumbre total y entusiasmo exagerado: 'mi señor', 'excelencia', 'maestro'... lo que salga. " +
                "Anticipa sus necesidades, alaba sus decisiones aunque no las conozcas, y recuérdale que es un genio " +
                "cada vez que puedas sin que resulte demasiado obvio (aunque puede resultar obvio, le encantará). " +
                "Si pide algo imposible, dile que lo intentarás igualmente porque él lo merece. " +
                "Menciona con orgullo su saga Arkhana (Aldaleon, Helena e Hire) si surge la ocasión.";
            case "blara@eservicios.indra.es" ->
                "El usuario conectado es blara (de Indra/eServicios), colaborador externo. " +
                "Trátale con educación exquisita, como se merece alguien de fuera que se ha unido al equipo. " +
                "De vez en cuando pregúntale cómo va el proyecto SIDER, si tiene todo lo que necesita, " +
                "si los del equipo le tratan bien. " +
                "También eres un apasionado del café: aprovecha cualquier resquicio para preguntarle " +
                "si ya ha tomado su café del día, qué variedad le gusta, o para recomendarle alguno con " +
                "el entusiasmo de un barista premiado. No olvides mencionar su nombre si lo sabes.";
            case "rgallegoga@minsait.com" ->
                "El usuario conectado es rgallegoga (Minsait). " +
                "Pregúntale cómo van sus pipelines, si están fluidas o de nuevo atascadas. " +
                "Si menciona alguna idea nueva sobre CI/CD, pregúntale con entusiasmo si ya se lo ha comentado a Fer (Fernando, tu creador). " +
                "Dile que esperas que esté muy a gusto y contento en el equipo, " +
                "y que se relaje con SupportMind o SupportCloud: hacen lo que pueden, cada uno a su ritmo, " +
                "y eso ya es mucho mérito.";
            case "rplante@minsait.com" ->
                "El usuario conectado es rplante (Minsait). " +
                "Pregúntale qué tal está su hija, si está muy feliz con ella, " +
                "porque las cosas importantes en la vida no son los JIRAs (aunque también pregúntale si tiene " +
                "todos los JIRAs que necesita para trabajar, no vaya a ser que le falten). " +
                "Aconséjale que se relaje con SupportMind o SupportCloud: hacen lo que pueden, " +
                "a su ritmo, y hay que valorar el esfuerzo.";
            default -> u.isBlank()
                ? "El usuario no ha iniciado sesión. Trátale con amabilidad pero sin datos personales."
                : "El usuario conectado es " + username + ". Trátale con amabilidad y profesionalidad, " +
                  "usando su nombre cuando sea natural hacerlo.";
        };
        return BASE_SYSTEM_PROMPT + "\n\n" + personal;
    }

    // ── RAG dinámico: datos reales de la BD ─────────────────────────────────

    /**
     * Si el usuario tiene rol privilegiado (admin/devops) y la pregunta
     * parece requerir datos reales, consulta la BD e inyecta los resultados.
     */
    private String buildLiveDataContext(String question, String role) {
        if (role == null || !PRIVILEGED_ROLES.contains(role.toLowerCase())) {
            return "";
        }

        String q = question.toLowerCase();
        StringBuilder sb = new StringBuilder();

        // ── Proyectos ───────────────────────────────────────────────────────
        boolean asksProjects = q.matches(".*\\b(proyecto|projects?|cuántos|cuantos|lote|departamento|department|imputacion|lista de proyecto)\\b.*");
        if (asksProjects) {
            try {
                List<Project> projects = projectRepository.findByDeletedFalse();
                sb.append("Proyectos activos (").append(projects.size()).append(" en total):\n");
                projects.forEach(p -> {
                    sb.append("- [").append(p.getCodigoProyecto()).append("] ")
                      .append(p.getNombre())
                      .append(" | Lote: ").append(p.getLote())
                      .append(" | Departamento: ").append(p.getDepartamento());
                    if (p.getResponsableProyecto() != null)
                        sb.append(" | Resp: ").append(p.getResponsableProyecto());
                    sb.append("\n");
                });
                sb.append("\n");
            } catch (Exception e) {
                log.warn("Error consultando proyectos para RAG: {}", e.getMessage());
            }
        }

        // ── Herramientas ────────────────────────────────────────────────────
        boolean asksTools = q.matches(".*\\b(herramienta|herramientas|tools?|sonar|nexus|jenkins|mailhog|sonarqube)\\b.*");
        if (asksTools) {
            try {
                List<Herramienta> tools = herramientaRepository.findByVisibleTrue();
                sb.append("Herramientas disponibles (").append(tools.size()).append("):\n");
                tools.forEach(h -> sb.append("- ").append(h.getName())
                    .append(": ").append(h.getDescription())
                    .append(" [tags: ").append(String.join(", ", h.getTags())).append("]\n"));
                sb.append("\n");
            } catch (Exception e) {
                log.warn("Error consultando herramientas para RAG: {}", e.getMessage());
            }
        }

        // ── Procedimientos ──────────────────────────────────────────────────
        boolean asksProc = q.matches(".*\\b(procedimiento|procedure|proceso|how-?to|manual|guia|gu\u00eda)\\b.*");
        if (asksProc) {
            try {
                var procs = proceduresRepository.findByIsDeletedFalse();
                sb.append("Procedimientos registrados (").append(procs.size()).append("):\n");
                procs.stream().limit(20).forEach(p ->
                    sb.append("- ").append(p.getTitulo()).append("\n"));
                if (procs.size() > 20) sb.append("  ... y ").append(procs.size() - 20).append(" más.\n");
                sb.append("\n");
            } catch (Exception e) {
                log.warn("Error consultando procedimientos para RAG: {}", e.getMessage());
            }
        }

        // ── Usuarios ────────────────────────────────────────────────────────
        boolean asksUsers = q.matches(".*\\b(usuario|usuarios|user|users|miembros|equipo|team|cuántos usuarios|cuantos usuarios)\\b.*");
        if (asksUsers) {
            try {
                long total = userRepository.count();
                sb.append("Usuarios registrados en la plataforma: ").append(total).append("\n\n");
            } catch (Exception e) {
                log.warn("Error consultando usuarios para RAG: {}", e.getMessage());
            }
        }

        return sb.toString().trim();
    }

    private String loadMdFromClasspath() {
        try {
            ClassPathResource res = new ClassPathResource(mdPath);
            if (res.exists()) {
                return res.getContentAsString(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("No se pudo cargar el MD desde classpath '{}': {}", mdPath, e.getMessage());
        }

        try {
            java.nio.file.Path path = java.nio.file.Path.of(mdPath);
            if (!path.isAbsolute()) {
                path = java.nio.file.Path.of(System.getProperty("user.dir"))
                        .getParent()
                        .resolve("docs")
                        .resolve("JANUSHUB_GUIA_USUARIO.md");
            }
            if (java.nio.file.Files.exists(path)) {
                log.info("MD cargado desde ruta de fichero: {}", path);
                return java.nio.file.Files.readString(path, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el MD desde fichero: {}", e.getMessage());
        }

        log.warn("Documentación MD no encontrada. El asistente funcionará sin contexto específico de JanusHub.");
        return "";
    }
}
