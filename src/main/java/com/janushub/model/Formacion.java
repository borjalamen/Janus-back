package com.janushub.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "formations") 
public class Formacion { 

    @Id
    private String id; 
    
    @Field("name")
    private String name; 
    
    @Field("link")
    private String link;
    
    @Field("description")
    private String description;
    
    @Field("tags")
    private List<String> tags; 
    
    @Field("location")
    private String location;
    
    @Field("visible")
    private Boolean visible = true;
    
    @Field("deleted")
    private Boolean deleted = false; // Campo para borrado lógico
    
    @Field("deletedAt")
    private LocalDateTime deletedAt;
}