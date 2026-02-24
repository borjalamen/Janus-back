package com.janushub.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "logbook")
public class Bitacora {
    @Id
    private String id;

    private String idProyecto;

    private String titulo; 

    private String contexto;
    private String error;
    private List<Solucion> soluciones; // Array de soluciones (antes era String solucion)
    private String entorno; // 'minsait' | 'preproduccion' | 'produccion'
    private LocalDateTime fecha;
    private List<String> tags;
    private boolean visible = true;
}
