// Archivo: src/main/java/com/janushub/HelloResolver.java
package com.janushub;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller // Necesario para que Spring lo detecte
public class HelloResolver {

    @QueryMapping // Mapea este método a la query 'hello' en el esquema
    public String hello() {
        return "Hola desde JanusHub GraphQL!";
    }
}
