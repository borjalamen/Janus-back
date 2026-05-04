package com.janushub.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.janushub.service.OpenAiService;
import com.janushub.service.OpenAiService.AiResult;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final OpenAiService openAiService;

    public AiController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @PostMapping("/query")
    public ResponseEntity<?> query(@RequestBody Map<String,Object> body){
        String question = (String) body.get("question");
        String username = (String) body.getOrDefault("username", "");
        String role     = (String) body.getOrDefault("role", "");
        if(question == null || question.isBlank()){
            return ResponseEntity.badRequest().body(Map.of("error","question required"));
        }

        try{
            AiResult result = openAiService.query(question, username, role);
            Map<String, Object> resp = new HashMap<>();
            resp.put("answer", result.answer());
            if (result.actionResult() != null) {
                resp.put("actionResult", result.actionResult());
            }
            return ResponseEntity.ok(resp);
        } catch(Exception e){
            String details = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Error en AI query: {}", details, e);
            Map<String, Object> err = new HashMap<>();
            err.put("error", "AI request failed");
            err.put("details", details);
            return ResponseEntity.status(500).body(err);
        }
    }
}
