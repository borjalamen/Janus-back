package com.janushub.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "parametrization")
public class Parametritzacio {
    @Id
    private String id;
    private int numModificacio;
    private String version;
    private String versioQuestionari;
}
