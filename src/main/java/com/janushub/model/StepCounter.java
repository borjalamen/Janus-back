package com.janushub.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Document(collection = "counters")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class StepCounter {

     @Id
    private String id;  // ex: "stepId"

    private long seq;


    
}
