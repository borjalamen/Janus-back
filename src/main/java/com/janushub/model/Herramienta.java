package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "herramientas")
public class Herramienta {

    @Id
    private String id;

    private String name;
    private String description;
    private String functionality;

    private List<String> tags = new ArrayList<>();
    private List<InstallStep> installSteps = new ArrayList<>();
    private List<String> projects = new ArrayList<>();

    private String projectsString;
    private Boolean visible = true;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstallStep {
        private String text;
        private List<ToolAttachment> attachments = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolAttachment {
        private String name;
        private String mimeType;
        private String dataUrl;
        private Long size;
    }
}
