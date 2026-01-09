package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "planificacions")
public class Planificacion {

    @Id
    private String id;

    private String titol;
    private String descripcio;

    private LocalDate dataInici;
    private LocalDate dataFi;

    private String usuariId;
    private String estat;
}
