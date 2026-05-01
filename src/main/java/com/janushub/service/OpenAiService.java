package com.janushub.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /** Resultado devuelto al controller: texto visible + resultado de acción opcional. */
    public record AiResult(String answer, String actionResult) {}

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
        return queryFull(question, username, "").answer();
    }

    public AiResult query(String question, String username, String role) throws Exception {
        return queryFull(question, username, role);
    }

    private AiResult queryFull(String question, String username, String role) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "Groq API key no configurada. Añade 'groq.api.key' en application.properties " +
                "o la variable de entorno GROQ_API_KEY."
            );
        }

        boolean isPrivileged = PRIVILEGED_ROLES.contains(role.toLowerCase());
        String personalizedSystem = buildSystemPrompt(username)
            + (isPrivileged ? "\n\n" + AGENT_INSTRUCTIONS : "");
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
        String rawAnswer = resp.body();
        try {
            var choices = (List<?>) json.get("choices");
            if (choices != null && !choices.isEmpty()) {
                var first   = (Map<?, ?>) choices.get(0);
                var message = (Map<?, ?>) first.get("message");
                if (message != null) {
                    rawAnswer = (String) message.get("content");
                }
            }
        } catch (Exception e) {
            log.warn("Error parseando respuesta de Groq: {}", e.getMessage());
        }

        // ── Parsear y ejecutar acciones si el rol tiene permisos ──
        if (isPrivileged) {
            return executeActions(rawAnswer);
        }
        return new AiResult(rawAnswer, null);
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

    // ── Agente: instrucciones y ejecución de acciones ────────────────────────

    private static final String AGENT_INSTRUCTIONS =
        "CAPACIDADES DE AGENTE: Tienes acceso directo a la base de datos de JanusHub y puedes ejecutar acciones reales.\n" +
        "Cuando el usuario te pida CREAR, MODIFICAR o ELIMINAR algo, hazlo directamente con el bloque ACTION.\n\n" +
        "--- CREAR herramienta ---\n" +
        "<<<ACTION>>>\n" +
        "{\"action\":\"CREATE_HERRAMIENTA\",\"name\":\"...\",\"description\":\"...\",\"functionality\":\"...\",\"tags\":[\"...\"]}\n" +
        "<<<END_ACTION>>>\n\n" +
        "--- MODIFICAR herramienta (usa el id que conoces por contexto o por lo que el usuario menciona) ---\n" +
        "<<<ACTION>>>\n" +
        "{\"action\":\"UPDATE_HERRAMIENTA\",\"id\":\"<id MongoDB>\",\"name\":\"...\",\"description\":\"...\",\"functionality\":\"...\",\"tags\":[\"...\"]}\n" +
        "<<<END_ACTION>>>\n\n" +
        "--- ELIMINAR herramienta ---\n" +
        "<<<ACTION>>>\n" +
        "{\"action\":\"DELETE_HERRAMIENTA\",\"id\":\"<id MongoDB>\"}\n" +
        "<<<END_ACTION>>>\n\n" +
        "Reglas: " +
        "1) Solo incluye el bloque cuando el usuario haya pedido EXPLÍCITAMENTE crear/modificar/eliminar. " +
        "2) Para UPDATE y DELETE DEBES saber el id; si no lo sabes, di al usuario que indique el ID de la herramienta. " +
        "3) Tags deben ser términos técnicos en inglés (ej: [\"build\",\"java\",\"dependencies\",\"devops\"]). " +
        "4) Incluye SIEMPRE el bloque aunque ya hayas explicado los pasos.";

    private static final Pattern ACTION_PATTERN =
        Pattern.compile("<<<ACTION>>>\\s*(\\{.*?\\})\\s*<<<END_ACTION>>>", Pattern.DOTALL);

    /**
     * Extrae bloques ACTION del texto de la IA, los ejecuta y devuelve
     * la respuesta limpia (sin el bloque JSON) + el resultado de la acción.
     */
    private AiResult executeActions(String rawAnswer) {
        Matcher m = ACTION_PATTERN.matcher(rawAnswer);
        if (!m.find()) {
            return new AiResult(rawAnswer, null);
        }

        String jsonBlock = m.group(1).trim();
        // Limpiar el bloque ACTION del texto visible
        String cleanAnswer = rawAnswer.substring(0, m.start()).trim();
        if (cleanAnswer.isBlank()) cleanAnswer = rawAnswer.replaceAll("<<<ACTION>>>.*<<<END_ACTION>>>", "").trim();

        String actionResult = null;
        try {
            Map<?, ?> actionMap = mapper.readValue(jsonBlock, Map.class);
            String action = (String) actionMap.get("action");

            actionResult = switch (action) {
                case "CREATE_HERRAMIENTA" -> createHerramientaFromJson(actionMap);
                case "UPDATE_HERRAMIENTA", "MODIFICAR_HERRAMIENTA" -> updateHerramientaFromJson(actionMap);
                case "DELETE_HERRAMIENTA", "ELIMINAR_HERRAMIENTA" -> deleteHerramientaFromJson(actionMap);
                default -> "⚠️ Acción desconocida: " + action;
            };
        } catch (Exception e) {
            log.warn("Error ejecutando acción de agente: {}", e.getMessage());
            actionResult = "⚠️ Error al ejecutar la acción: " + e.getMessage();
        }

        return new AiResult(cleanAnswer, actionResult);
    }

    @SuppressWarnings("unchecked")
    private String createHerramientaFromJson(Map<?, ?> data) {
        Herramienta h = new Herramienta();
        h.setName((String) data.get("name"));
        h.setDescription((String) data.get("description"));
        h.setFunctionality((String) data.get("functionality"));
        h.setVisible(true);

        Object tagsObj = data.get("tags");
        if (tagsObj instanceof List) {
            h.setTags(((List<?>) tagsObj).stream()
                .map(Object::toString).collect(Collectors.toList()));
        }

        Herramienta saved = herramientaRepository.save(h);
        log.info("IAnusHub creó herramienta '{}' con id {}", saved.getName(), saved.getId());
        return "✅ Herramienta **" + saved.getName() + "** creada correctamente (ID: `" + saved.getId() + "`). Ya aparece en el listado de Herramientas.";
    }

    @SuppressWarnings("unchecked")
    private String updateHerramientaFromJson(Map<?, ?> data) {
        String id = (String) data.get("id");
        if (id == null || id.isBlank()) {
            return "⚠️ No se puede modificar: falta el ID de la herramienta. Indícame el ID y lo hago.";
        }
        return herramientaRepository.findById(id).map(h -> {
            if (data.get("name") != null)          h.setName((String) data.get("name"));
            if (data.get("description") != null)   h.setDescription((String) data.get("description"));
            if (data.get("functionality") != null) h.setFunctionality((String) data.get("functionality"));
            Object tagsObj = data.get("tags");
            if (tagsObj instanceof List) {
                h.setTags(((List<?>) tagsObj).stream()
                    .map(Object::toString).collect(Collectors.toList()));
            }
            Herramienta saved = herramientaRepository.save(h);
            log.info("IAnusHub modificó herramienta '{}' (id={})", saved.getName(), saved.getId());
            return "✅ Herramienta **" + saved.getName() + "** actualizada correctamente.";
        }).orElse("⚠️ No se encontró ninguna herramienta con ID `" + id + "`.");
    }

    private String deleteHerramientaFromJson(Map<?, ?> data) {
        String id = (String) data.get("id");
        if (id == null || id.isBlank()) {
            return "⚠️ No se puede eliminar: falta el ID de la herramienta. Indícame el ID y lo hago.";
        }
        return herramientaRepository.findById(id).map(h -> {
            herramientaRepository.deleteById(id);
            log.info("IAnusHub eliminó herramienta '{}' (id={})", h.getName(), id);
            return "✅ Herramienta **" + h.getName() + "** eliminada correctamente.";
        }).orElse("⚠️ No se encontró ninguna herramienta con ID `" + id + "`.");
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
