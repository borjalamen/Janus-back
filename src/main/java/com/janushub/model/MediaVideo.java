package com.janushub.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Document(collection = "media_videos") // nom de la col·lecció a Mongo
public class MediaVideo {

    @Id
    private String id;          

    private String title;
    private String description;
    private String file;       
    private String thumbnail;   
    private String duration;    

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
