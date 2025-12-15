package com.janushub.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OpenAiService {

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public String query(String question) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key not configured (openai.api.key)");
        }

        // Build a simple chat completion request for OpenAI v1/chat/completions
        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", new Object[] { Map.of("role", "user", "content", question) },
                "max_tokens", 800
        );

        String body = mapper.writeValueAsString(payload);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() >= 400) {
            throw new RuntimeException("OpenAI error: " + resp.statusCode() + " - " + resp.body());
        }

        // Parse response to extract the assistant message
        Map<?,?> json = mapper.readValue(resp.body(), Map.class);
        try {
            var choices = (java.util.List<?>) json.get("choices");
            if (choices != null && !choices.isEmpty()) {
                var first = (Map<?,?>) choices.get(0);
                var message = (Map<?,?>) first.get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            // ignore parse errors
        }

        // Fallback: return whole body
        return resp.body();
    }
}
