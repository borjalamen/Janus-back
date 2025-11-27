package com.janushub.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "jenkins") // <-- Asegúrate de que coincida con tu colección en Mongo
public class Jenkins {

    @Id
    private String id;
    
    private String idProyecto; // ID del proyecto al que pertenece
    private String nombre;     // Nombre del Job o servidor
    private String url;        // URL de Jenkins
}
