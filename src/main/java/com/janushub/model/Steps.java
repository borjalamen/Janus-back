package com.janushub.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Document(collection = "steps")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Steps {

    @Id
    private String id;

    private String title;
    private String description;
    private String responsible;
    private String method;
    private Integer order;
    private java.util.List<String> tags;


    public Steps(String title, String description, String responsible, String method, Integer order, java.util.List<String> tags) {
        this.title = title;
        this.description = description;
        this.responsible = responsible;
        this.method = method;
        this.order = order;
        this.tags = tags;
    }
public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}