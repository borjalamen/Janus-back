package com.janushub.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data; 
import java.util.List;

@Data
@Document(collection = "formation") 
public class Formacion { 

    @Id
    private String id; 
    
    private String name; 
    private String link;
    private String description;
    private List<String> tags; 
    private String location; 
}