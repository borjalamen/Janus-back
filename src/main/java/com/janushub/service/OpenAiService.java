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
import com.janushub.model.Formacion;
import com.janushub.model.Herramienta;
import com.janushub.model.Infraestructura;
import com.janushub.model.Procedure;
import com.janushub.model.Project;
import com.janushub.repository.FormacionRepository;
import com.janushub.repository.HerramientaRepository;
import com.janushub.repository.InfraestructuraRepository;
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
    @Autowired private ProjectRepository        projectRepository;
    @Autowired private HerramientaRepository     herramientaRepository;
    @Autowired private ProceduresRepository      proceduresRepository;
    @Autowired private InfraestructuraRepository  infraestructuraRepository;
    @Autowired private FormacionRepository        formacionRepository;
    @Autowired private UserRepository            userRepository;

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

        String safeRole = (role == null) ? "" : role;
        boolean isPrivileged = PRIVILEGED_ROLES.contains(safeRole.toLowerCase());
        String personalizedSystem = buildSystemPrompt(username)
            + "\n\n" + FILL_INSTRUCTIONS
            + (isPrivileged ? "\n\n" + AGENT_INSTRUCTIONS : "");
        String relevantContext = findRelevantContext(question, 3);
        String liveData = buildLiveDataContext(question, safeRole);

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
        return executeFillOnlyActions(rawAnswer);
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
        "CAPACIDADES DE AGENTE: Tienes acceso directo a la base de datos de JanusHub y puedes ejecutar acciones reales en: " +
        "Herramientas, Proyectos, Procedimientos e Infraestructuras.\n" +
        "Cuando el usuario te pida CREAR, MODIFICAR o ELIMINAR cualquiera de estos recursos, inclúyelo directamente en un bloque ACTION.\n\n" +

        "=== HERRAMIENTAS ===\n" +
        "CREATE_HERRAMIENTA: {\"action\":\"CREATE_HERRAMIENTA\",\"name\":\"...\",\"description\":\"...\",\"functionality\":\"...\",\"tags\":[\"...\"]}\n" +
        "UPDATE_HERRAMIENTA: {\"action\":\"UPDATE_HERRAMIENTA\",\"id\":\"\",\"name\":\"<nombre exacto>\",\"description\":\"...\",\"functionality\":\"...\",\"tags\":[\"...\"]}\n" +
        "DELETE_HERRAMIENTA: {\"action\":\"DELETE_HERRAMIENTA\",\"id\":\"\",\"name\":\"<nombre exacto>\"}\n\n" +

        "=== PROYECTOS ===\n" +
        "CREATE_PROYECTO: {\"action\":\"CREATE_PROYECTO\",\"codigoProyecto\":\"PRJ-XXX\",\"nombre\":\"...\",\"departamento\":\"...\",\"lote\":\"...\",\"responsableProyecto\":\"...\",\"responsableTecnico\":\"...\"}\n" +
        "UPDATE_PROYECTO: {\"action\":\"UPDATE_PROYECTO\",\"id\":\"\",\"codigoProyecto\":\"<código exacto si no tienes id>\",\"nombre\":\"...\",\"departamento\":\"...\",\"responsableProyecto\":\"...\",\"responsableTecnico\":\"...\"}\n" +
        "DELETE_PROYECTO: {\"action\":\"DELETE_PROYECTO\",\"id\":\"\",\"codigoProyecto\":\"<código exacto si no tienes id>\"}\n\n" +

        "=== PROCEDIMIENTOS ===\n" +
        "CREATE_PROCEDIMIENTO: {\"action\":\"CREATE_PROCEDIMIENTO\",\"titulo\":\"...\",\"descripcion\":\"...\",\"departamento\":\"...\",\"tags\":[\"...\"]}\n" +
        "UPDATE_PROCEDIMIENTO: {\"action\":\"UPDATE_PROCEDIMIENTO\",\"id\":\"\",\"titulo\":\"<título exacto si no tienes id>\",\"descripcion\":\"...\",\"departamento\":\"...\",\"tags\":[\"...\"]}\n" +
        "DELETE_PROCEDIMIENTO: {\"action\":\"DELETE_PROCEDIMIENTO\",\"id\":\"\",\"titulo\":\"<título exacto si no tienes id>\"}\n\n" +

        "=== INFRAESTRUCTURA ===\n" +
        "CREATE_INFRA: {\"action\":\"CREATE_INFRA\",\"ip\":\"...\",\"host\":\"...\",\"so\":\"...\",\"estado\":\"activo\",\"cpu\":\"...\",\"ram\":\"...\",\"capacidad\":\"...\",\"cpd\":\"...\",\"tags\":[\"...\"]}\n" +
        "UPDATE_INFRA: {\"action\":\"UPDATE_INFRA\",\"id\":\"\",\"ip\":\"<IP exacta si no tienes id>\",\"host\":\"...\",\"so\":\"...\",\"estado\":\"...\",\"cpu\":\"...\",\"ram\":\"...\",\"capacidad\":\"...\",\"tags\":[\"...\"]}\n" +
        "DELETE_INFRA: {\"action\":\"DELETE_INFRA\",\"id\":\"\",\"ip\":\"<IP exacta si no tienes id>\"}\n\n" +

        "=== FORMACIÓN (TRAINING) ===\n" +
        "CREATE_FORMACION: {\"action\":\"CREATE_FORMACION\",\"name\":\"...\",\"link\":\"https://...\",\"description\":\"...\",\"tags\":[\"...\"],\"location\":\"Online\"}\n" +
        "UPDATE_FORMACION: {\"action\":\"UPDATE_FORMACION\",\"id\":\"\",\"name\":\"<nombre exacto si no tienes id>\",\"link\":\"...\",\"description\":\"...\",\"tags\":[\"...\"],\"location\":\"...\"}\n" +
        "DELETE_FORMACION: {\"action\":\"DELETE_FORMACION\",\"id\":\"\",\"name\":\"<nombre exacto si no tienes id>\"}\n" +
        "DELETE_ALL_FORMACION: {\"action\":\"DELETE_ALL_FORMACION\"} — BORRA TODAS las formaciones activas de una vez.\n\n" +

        "REGLAS GLOBALES:\n" +
        "1) Incluye el bloque ACTION solo cuando el usuario haya pedido EXPLÍCITAMENTE crear/modificar/eliminar.\n" +
        "2) Para UPDATE/DELETE: usa id si lo conoces; si no, usa el campo identificador (name, codigoProyecto, titulo o ip) con el valor EXACTO.\n" +
        "3) NUNCA escribas literales como '<id>' o '<nombre>' — si no tienes el dato, usa cadena vacía y rellena el campo identificador.\n" +
        "4) Tags siempre en inglés y en minúsculas.\n" +
        "5) Pon el bloque ACTION AL FINAL de tu respuesta, sin explicarlo.\n" +
        "6) Formato del bloque, siempre así:\n" +
        "<<<ACTION>>>\n" +
        "{...json...}\n" +
        "<<<END_ACTION>>>";

    // ── Instrucciones de relleno de estimación (disponibles para todos los roles) ────
    private static final String FILL_INSTRUCTIONS =
        "=== RELLENO DE FORMULARIO DE ESTIMACIÓN ===\n" +
        "Cuando el usuario te pida crear, generar o rellenar una estimación (puede adjuntar un txt, descripción, lista de tareas...),\n" +
        "analiza los datos y genera un bloque ACTION con FILL_ESTIMACION AL FINAL de tu respuesta.\n" +
        "Formato: FILL_ESTIMACION: {\"action\":\"FILL_ESTIMACION\",\"estimationName\":\"...\",\"projectCode\":\"PRJ-XXX o vacío\",\n" +
        "\"projectName\":\"...\",\"requester\":\"...\",\"requesterEmail\":\"...\",\"notes\":\"Resumen ejecutivo del trabajo\",\n" +
        "\"weeks\":[\"Semana 1\",\"Semana 2\"],\"tasks\":[{\"title\":\"Nombre tarea\",\"estimates\":[8,4]}]}\n" +
        "REGLAS:\n" +
        "1) El array 'estimates' de cada tarea debe tener exactamente la misma longitud que 'weeks'.\n" +
        "2) Los valores de 'estimates' son horas de trabajo dedicadas esa semana a esa tarea.\n" +
        "3) Genera tantas tareas y semanas como sean necesarias según el contexto proporcionado.\n" +
        "4) Si el usuario no especifica solicitante o email, déjalos vacíos.\n" +
        "5) El bloque ACTION siempre va AL FINAL, con el formato:\n" +
        "<<<ACTION>>>\n{...json...}\n<<<END_ACTION>>>";

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

            if (action == null) {
                actionResult = "⚠️ Acción no reconocida: el bloque ACTION no contiene campo 'action'.";
                return new AiResult(cleanAnswer, actionResult);
            }

            actionResult = switch (action) {
                // ── Herramientas ──────────────────────────────────────────────
                case "CREATE_HERRAMIENTA"                         -> createHerramientaFromJson(actionMap);
                case "UPDATE_HERRAMIENTA", "MODIFICAR_HERRAMIENTA" -> updateHerramientaFromJson(actionMap);
                case "DELETE_HERRAMIENTA", "ELIMINAR_HERRAMIENTA" -> deleteHerramientaFromJson(actionMap);
                // ── Proyectos ─────────────────────────────────────────────────
                case "CREATE_PROYECTO"                            -> createProyectoFromJson(actionMap);
                case "UPDATE_PROYECTO", "MODIFICAR_PROYECTO"      -> updateProyectoFromJson(actionMap);
                case "DELETE_PROYECTO", "ELIMINAR_PROYECTO"       -> deleteProyectoFromJson(actionMap);
                // ── Procedimientos ────────────────────────────────────────────
                case "CREATE_PROCEDIMIENTO"                       -> createProcedimientoFromJson(actionMap);
                case "UPDATE_PROCEDIMIENTO", "MODIFICAR_PROCEDIMIENTO" -> updateProcedimientoFromJson(actionMap);
                case "DELETE_PROCEDIMIENTO", "ELIMINAR_PROCEDIMIENTO"  -> deleteProcedimientoFromJson(actionMap);
                // ── Infraestructura ───────────────────────────────────────────
                case "CREATE_INFRA"                               -> createInfraFromJson(actionMap);
                case "UPDATE_INFRA", "MODIFICAR_INFRA"            -> updateInfraFromJson(actionMap);
                case "DELETE_INFRA", "ELIMINAR_INFRA"             -> deleteInfraFromJson(actionMap);
                // ── Formación ─────────────────────────────────────────────
                case "CREATE_FORMACION"                           -> createFormacionFromJson(actionMap);
                case "UPDATE_FORMACION", "MODIFICAR_FORMACION"    -> updateFormacionFromJson(actionMap);
                case "DELETE_FORMACION", "ELIMINAR_FORMACION"     -> deleteFormacionFromJson(actionMap);
                case "DELETE_ALL_FORMACION", "ELIMINAR_TODAS_FORMACIONES" -> deleteAllFormacionFromJson(actionMap);
                // ── Estimación ────────────────────────────────────────────
                case "FILL_ESTIMACION"                                    -> fillEstimacionFromJson(actionMap);
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

    /**
     * Busca una herramienta por ID MongoDB; si no lo tiene o no lo encuentra,
     * intenta por nombre exacto (case-insensitive).
     */
    private java.util.Optional<Herramienta> resolveHerramienta(Map<?, ?> data) {
        String id   = (String) data.get("id");
        String name = (String) data.get("name");

        // Intentar por id si parece un ObjectId real (no vacío ni placeholder)
        if (id != null && !id.isBlank() && !id.contains("<")) {
            java.util.Optional<Herramienta> byId = herramientaRepository.findById(id);
            if (byId.isPresent()) return byId;
        }
        // Fallback: buscar por nombre
        if (name != null && !name.isBlank() && !name.contains("<")) {
            return herramientaRepository.findByNameIgnoreCase(name);
        }
        return java.util.Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private String updateHerramientaFromJson(Map<?, ?> data) {
        return resolveHerramienta(data).map(h -> {
            if (data.get("description") != null)   h.setDescription((String) data.get("description"));
            if (data.get("functionality") != null) h.setFunctionality((String) data.get("functionality"));
            // name solo se actualiza si viene un campo "newName" para evitar sobreescribir el nombre usado para buscar
            if (data.get("newName") != null)       h.setName((String) data.get("newName"));
            Object tagsObj = data.get("tags");
            if (tagsObj instanceof List) {
                h.setTags(((List<?>) tagsObj).stream()
                    .map(Object::toString).collect(Collectors.toList()));
            }
            Herramienta saved = herramientaRepository.save(h);
            log.info("IAnusHub modificó herramienta '{}' (id={})", saved.getName(), saved.getId());
            return "✅ Herramienta **" + saved.getName() + "** actualizada correctamente.";
        }).orElse("⚠️ No se encontró ninguna herramienta con ese nombre o ID. Revisa que el nombre sea exacto.");
    }

    private String deleteHerramientaFromJson(Map<?, ?> data) {
        return resolveHerramienta(data).map(h -> {
            herramientaRepository.deleteById(h.getId());
            log.info("IAnusHub eliminó herramienta '{}' (id={})", h.getName(), h.getId());
            return "✅ Herramienta **" + h.getName() + "** eliminada correctamente.";
        }).orElse("⚠️ No se encontró ninguna herramienta con ese nombre o ID. Revisa que el nombre sea exacto.");
    }

    // ── Agente: CRUD Proyectos ────────────────────────────────────────────────

    private java.util.Optional<Project> resolveProyecto(Map<?, ?> data) {
        String id     = (String) data.get("id");
        String codigo = (String) data.get("codigoProyecto");
        if (id != null && !id.isBlank() && !id.contains("<")) {
            var byId = projectRepository.findById(id);
            if (byId.isPresent()) return byId;
        }
        if (codigo != null && !codigo.isBlank() && !codigo.contains("<")) {
            return projectRepository.findByCodigoProyecto(codigo);
        }
        return java.util.Optional.empty();
    }

    private String createProyectoFromJson(Map<?, ?> data) {
        Project p = new Project();
        p.setCodigoProyecto((String) data.get("codigoProyecto"));
        p.setNombre((String) data.get("nombre"));
        p.setDepartamento((String) data.get("departamento"));
        p.setLote((String) data.get("lote"));
        p.setResponsableProyecto((String) data.get("responsableProyecto"));
        p.setResponsableTecnico((String) data.get("responsableTecnico"));
        Project saved = projectRepository.save(p);
        log.info("IAnusHub creó proyecto '{}' (id={})", saved.getNombre(), saved.getId());
        return "✅ Proyecto **" + saved.getNombre() + "** creado correctamente (ID: `" + saved.getId() + "`).";
    }

    private String updateProyectoFromJson(Map<?, ?> data) {
        return resolveProyecto(data).map(p -> {
            if (data.get("nombre") != null)               p.setNombre((String) data.get("nombre"));
            if (data.get("departamento") != null)         p.setDepartamento((String) data.get("departamento"));
            if (data.get("lote") != null)                 p.setLote((String) data.get("lote"));
            if (data.get("responsableProyecto") != null)  p.setResponsableProyecto((String) data.get("responsableProyecto"));
            if (data.get("responsableTecnico") != null)   p.setResponsableTecnico((String) data.get("responsableTecnico"));
            Project saved = projectRepository.save(p);
            log.info("IAnusHub modificó proyecto '{}' (id={})", saved.getNombre(), saved.getId());
            return "✅ Proyecto **" + saved.getNombre() + "** actualizado correctamente.";
        }).orElse("⚠️ No se encontró ningún proyecto con ese código o ID.");
    }

    private String deleteProyectoFromJson(Map<?, ?> data) {
        return resolveProyecto(data).map(p -> {
            p.setDeleted(true);
            projectRepository.save(p);
            log.info("IAnusHub marcó como eliminado el proyecto '{}' (id={})", p.getNombre(), p.getId());
            return "✅ Proyecto **" + p.getNombre() + "** eliminado correctamente.";
        }).orElse("⚠️ No se encontró ningún proyecto con ese código o ID.");
    }

    // ── Agente: CRUD Procedimientos ───────────────────────────────────────────

    private java.util.Optional<Procedure> resolveProcedimiento(Map<?, ?> data) {
        String id     = (String) data.get("id");
        String titulo = (String) data.get("titulo");
        if (id != null && !id.isBlank() && !id.contains("<")) {
            var byId = proceduresRepository.findById(id);
            if (byId.isPresent()) return byId;
        }
        if (titulo != null && !titulo.isBlank() && !titulo.contains("<")) {
            return proceduresRepository.searchByTitulo(titulo).stream().findFirst();
        }
        return java.util.Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private String createProcedimientoFromJson(Map<?, ?> data) {
        Procedure p = new Procedure();
        p.setTitulo((String) data.get("titulo"));
        p.setDescripcion((String) data.get("descripcion"));
        p.setDepartamento((String) data.get("departamento"));
        Object tagsObj = data.get("tags");
        if (tagsObj instanceof List) {
            p.setTags(((List<?>) tagsObj).stream().map(Object::toString).collect(Collectors.toList()));
        }
        Procedure saved = proceduresRepository.save(p);
        log.info("IAnusHub creó procedimiento '{}' (id={})", saved.getTitulo(), saved.getId());
        return "✅ Procedimiento **" + saved.getTitulo() + "** creado correctamente (ID: `" + saved.getId() + "`).";
    }

    @SuppressWarnings("unchecked")
    private String updateProcedimientoFromJson(Map<?, ?> data) {
        return resolveProcedimiento(data).map(p -> {
            if (data.get("titulo") != null)      p.setTitulo((String) data.get("titulo"));
            if (data.get("descripcion") != null) p.setDescripcion((String) data.get("descripcion"));
            if (data.get("departamento") != null) p.setDepartamento((String) data.get("departamento"));
            Object tagsObj = data.get("tags");
            if (tagsObj instanceof List) {
                p.setTags(((List<?>) tagsObj).stream().map(Object::toString).collect(Collectors.toList()));
            }
            Procedure saved = proceduresRepository.save(p);
            log.info("IAnusHub modificó procedimiento '{}' (id={})", saved.getTitulo(), saved.getId());
            return "✅ Procedimiento **" + saved.getTitulo() + "** actualizado correctamente.";
        }).orElse("⚠️ No se encontró ningún procedimiento con ese título o ID.");
    }

    private String deleteProcedimientoFromJson(Map<?, ?> data) {
        return resolveProcedimiento(data).map(p -> {
            p.setDeleted(true);
            proceduresRepository.save(p);
            log.info("IAnusHub marcó como eliminado el procedimiento '{}' (id={})", p.getTitulo(), p.getId());
            return "✅ Procedimiento **" + p.getTitulo() + "** eliminado correctamente.";
        }).orElse("⚠️ No se encontró ningún procedimiento con ese título o ID.");
    }

    // ── Agente: CRUD Infraestructura ──────────────────────────────────────────

    private java.util.Optional<Infraestructura> resolveInfra(Map<?, ?> data) {
        String id = (String) data.get("id");
        String ip = (String) data.get("ip");
        if (id != null && !id.isBlank() && !id.contains("<")) {
            var byId = infraestructuraRepository.findById(id);
            if (byId.isPresent()) return byId;
        }
        if (ip != null && !ip.isBlank() && !ip.contains("<")) {
            return infraestructuraRepository.findByIpAndDeletedFalse(ip);
        }
        return java.util.Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private String createInfraFromJson(Map<?, ?> data) {
        Infraestructura infra = new Infraestructura();
        infra.setIp((String) data.get("ip"));
        infra.setHost((String) data.get("host"));
        infra.setSo((String) data.get("so"));
        infra.setEstado(data.get("estado") != null ? (String) data.get("estado") : "activo");
        infra.setCpu((String) data.get("cpu"));
        infra.setRam((String) data.get("ram"));
        infra.setCapacidad((String) data.get("capacidad"));
        infra.setCpd((String) data.get("cpd"));
        infra.setDeleted(false);
        Object tagsObj = data.get("tags");
        if (tagsObj instanceof List) {
            infra.setTags(((List<?>) tagsObj).stream().map(Object::toString).collect(Collectors.toList()));
        }
        Infraestructura saved = infraestructuraRepository.save(infra);
        log.info("IAnusHub creó infraestructura '{}' (id={})", saved.getIp(), saved.getId());
        return "✅ Infraestructura **" + saved.getHost() + "** (" + saved.getIp() + ") creada correctamente (ID: `" + saved.getId() + "`).";
    }

    @SuppressWarnings("unchecked")
    private String updateInfraFromJson(Map<?, ?> data) {
        return resolveInfra(data).map(infra -> {
            if (data.get("host") != null)     infra.setHost((String) data.get("host"));
            if (data.get("so") != null)       infra.setSo((String) data.get("so"));
            if (data.get("estado") != null)   infra.setEstado((String) data.get("estado"));
            if (data.get("cpu") != null)      infra.setCpu((String) data.get("cpu"));
            if (data.get("ram") != null)      infra.setRam((String) data.get("ram"));
            if (data.get("capacidad") != null) infra.setCapacidad((String) data.get("capacidad"));
            if (data.get("cpd") != null)      infra.setCpd((String) data.get("cpd"));
            Object tagsObj = data.get("tags");
            if (tagsObj instanceof List) {
                infra.setTags(((List<?>) tagsObj).stream().map(Object::toString).collect(Collectors.toList()));
            }
            Infraestructura saved = infraestructuraRepository.save(infra);
            log.info("IAnusHub modificó infraestructura '{}' (id={})", saved.getIp(), saved.getId());
            return "✅ Infraestructura **" + saved.getHost() + "** actualizada correctamente.";
        }).orElse("⚠️ No se encontró ninguna infraestructura con esa IP o ID.");
    }

    private String deleteInfraFromJson(Map<?, ?> data) {
        return resolveInfra(data).map(infra -> {
            infra.setDeleted(true);
            infraestructuraRepository.save(infra);
            log.info("IAnusHub marcó como eliminada la infraestructura '{}' (id={})", infra.getIp(), infra.getId());
            return "✅ Infraestructura **" + infra.getHost() + "** eliminada correctamente.";
        }).orElse("⚠️ No se encontró ninguna infraestructura con esa IP o ID.");
    }

    // ── Agente: CRUD Formación ────────────────────────────────────────────────

    private java.util.Optional<Formacion> resolveFormacion(Map<?, ?> data) {
        String id   = (String) data.get("id");
        String name = (String) data.get("name");
        if (id != null && !id.isBlank() && !id.contains("<")) {
            var byId = formacionRepository.findByIdAndDeletedFalse(id);
            if (byId.isPresent()) return byId;
        }
        if (name != null && !name.isBlank() && !name.contains("<")) {
            return formacionRepository.findByNameIgnoreCaseAndDeletedFalse(name);
        }
        return java.util.Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private String createFormacionFromJson(Map<?, ?> data) {
        Formacion f = new Formacion();
        f.setName((String) data.get("name"));
        f.setLink((String) data.get("link"));
        f.setDescription((String) data.get("description"));
        f.setLocation(data.get("location") != null ? (String) data.get("location") : "Online");
        f.setVisible(true);
        f.setDeleted(false);
        Object tagsObj = data.get("tags");
        if (tagsObj instanceof List) {
            f.setTags(((List<?>) tagsObj).stream().map(Object::toString).collect(Collectors.toList()));
        }
        Formacion saved = formacionRepository.save(f);
        log.info("IAnusHub creó formación '{}' (id={})", saved.getName(), saved.getId());
        return "✅ Formación **" + saved.getName() + "** creada correctamente (ID: `" + saved.getId() + "`). Ya aparece en el listado de Training.";
    }

    @SuppressWarnings("unchecked")
    private String updateFormacionFromJson(Map<?, ?> data) {
        return resolveFormacion(data).map(f -> {
            if (data.get("newName") != null)    f.setName((String) data.get("newName"));
            if (data.get("link") != null)        f.setLink((String) data.get("link"));
            if (data.get("description") != null) f.setDescription((String) data.get("description"));
            if (data.get("location") != null)    f.setLocation((String) data.get("location"));
            Object tagsObj = data.get("tags");
            if (tagsObj instanceof List) {
                f.setTags(((List<?>) tagsObj).stream().map(Object::toString).collect(Collectors.toList()));
            }
            Formacion saved = formacionRepository.save(f);
            log.info("IAnusHub modificó formación '{}' (id={})", saved.getName(), saved.getId());
            return "✅ Formación **" + saved.getName() + "** actualizada correctamente.";
        }).orElse("⚠️ No se encontró ninguna formación con ese nombre o ID. Revisa que el nombre sea exacto.");
    }

    private String deleteFormacionFromJson(Map<?, ?> data) {
        return resolveFormacion(data).map(f -> {
            f.setDeleted(true);
            formacionRepository.save(f);
            log.info("IAnusHub marcó como eliminada la formación '{}' (id={})", f.getName(), f.getId());
            return "✅ Formación **" + f.getName() + "** eliminada correctamente.";
        }).orElse("⚠️ No se encontró ninguna formación con ese nombre o ID. Revisa que el nombre sea exacto.");
    }

    /**
     * Para usuarios sin rol privilegiado: solo ejecuta FILL_ESTIMACION (no escribe en BD).
     */
    private AiResult executeFillOnlyActions(String rawAnswer) {
        Matcher m = ACTION_PATTERN.matcher(rawAnswer);
        if (!m.find()) return new AiResult(rawAnswer, null);

        String jsonBlock = m.group(1).trim();
        String cleanAnswer = rawAnswer.substring(0, m.start()).trim();
        if (cleanAnswer.isBlank()) cleanAnswer = rawAnswer.replaceAll("<<<ACTION>>>.*<<<END_ACTION>>>", "").trim();

        try {
            Map<?, ?> actionMap = mapper.readValue(jsonBlock, Map.class);
            String action = (String) actionMap.get("action");
            if ("FILL_ESTIMACION".equals(action)) {
                return new AiResult(cleanAnswer, fillEstimacionFromJson(actionMap));
            }
        } catch (Exception e) {
            log.warn("Error ejecutando fill action: {}", e.getMessage());
        }
        return new AiResult(rawAnswer, null);
    }

    @SuppressWarnings("unchecked")
    private String fillEstimacionFromJson(Map<?, ?> data) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("estimationName", data.getOrDefault("estimationName", ""));
            result.put("projectCode",    data.getOrDefault("projectCode", ""));
            result.put("projectName",    data.getOrDefault("projectName", ""));
            result.put("requester",      data.getOrDefault("requester", ""));
            result.put("requesterEmail", data.getOrDefault("requesterEmail", ""));
            result.put("notes",          data.getOrDefault("notes", ""));

            // Weeks: puede llegar como lista de strings o como número entero
            Object weeksObj = data.get("weeks");
            List<String> weeks;
            if (weeksObj instanceof List) {
                weeks = ((List<?>) weeksObj).stream().map(Object::toString).collect(Collectors.toList());
            } else if (weeksObj instanceof Number) {
                int n = ((Number) weeksObj).intValue();
                weeks = new ArrayList<>();
                for (int i = 1; i <= n; i++) weeks.add("Semana " + i);
            } else {
                weeks = new ArrayList<>(List.of("Semana 1"));
            }
            result.put("weeks", weeks);

            // Tasks
            Object tasksObj = data.get("tasks");
            List<Map<String, Object>> tasks = new ArrayList<>();
            if (tasksObj instanceof List) {
                for (Object t : (List<?>) tasksObj) {
                    if (!(t instanceof Map)) continue;
                    Map<?, ?> tm = (Map<?, ?>) t;
                    Map<String, Object> task = new LinkedHashMap<>();
                    task.put("title", tm.getOrDefault("title", "Tarea sin nombre"));
                    Object est = tm.get("estimates");
                    List<Integer> estimates;
                    if (est instanceof List) {
                        estimates = ((List<?>) est).stream()
                            .map(e -> e instanceof Number ? ((Number) e).intValue() : 0)
                            .collect(Collectors.toList());
                        // Ajustar longitud si no coincide con weeks
                        while (estimates.size() < weeks.size()) estimates.add(0);
                    } else {
                        estimates = new ArrayList<>();
                        weeks.forEach(w -> estimates.add(0));
                    }
                    task.put("estimates", estimates);
                    tasks.add(task);
                }
            }
            result.put("tasks", tasks);

            String json = mapper.writeValueAsString(result);
            log.info("IAnusHub genera estimación: {} tareas, {} semanas", tasks.size(), weeks.size());
            return "FILL_ESTIMACION_DATA:" + json;
        } catch (Exception e) {
            log.warn("Error serializando FILL_ESTIMACION: {}", e.getMessage());
            return "⚠️ Error generando la estimación: " + e.getMessage();
        }
    }

    private String deleteAllFormacionFromJson(Map<?, ?> data) {
        try {
            List<Formacion> activas = formacionRepository.findByDeletedFalseAndVisibleTrue();
            if (activas.isEmpty()) {
                return "ℹ️ No hay formaciones activas que eliminar.";
            }
            activas.forEach(f -> f.setDeleted(true));
            formacionRepository.saveAll(activas);
            log.info("IAnusHub marcó como eliminadas {} formaciones", activas.size());
            return "✅ Se han eliminado **" + activas.size() + "** formaciones correctamente.";
        } catch (Exception e) {
            return "⚠️ Error al eliminar todas las formaciones: " + e.getMessage();
        }
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

        // ── Formación ───────────────────────────────────────────────────────
        boolean asksFormacion = q.matches(".*\\b(formaci[oó]n|formaciones|curso|cursos|training|course|courses|aprendizaje|capacitaci[oó]n)\\b.*");
        if (asksFormacion) {
            try {
                List<Formacion> cursos = formacionRepository.findByDeletedFalseAndVisibleTrue();
                sb.append("Formaciones/cursos activos (").append(cursos.size()).append(" en total):\n");
                cursos.forEach(f -> {
                    sb.append("- **").append(f.getName()).append("**");
                    if (f.getDescription() != null && !f.getDescription().isBlank())
                        sb.append(": ").append(f.getDescription());
                    if (f.getLink() != null && !f.getLink().isBlank())
                        sb.append(" | Link: ").append(f.getLink());
                    if (f.getTags() != null && !f.getTags().isEmpty())
                        sb.append(" | Tags: ").append(String.join(", ", f.getTags()));
                    sb.append("\n");
                });
                sb.append("\n");
            } catch (Exception e) {
                log.warn("Error consultando formaciones para RAG: {}", e.getMessage());
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
