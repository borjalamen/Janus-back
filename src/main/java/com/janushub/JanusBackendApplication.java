// Archivo: src/main/java/com/janushub/JanusBackendApplication.java
package com.janushub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class JanusBackendApplication {

    public static void main(String[] args) {
        // Esto inicializa todo el framework de Spring Boot, incluyendo
        // el servidor web, el logger, y las dependencias (MongoDB, GraphQL).
        SpringApplication.run(JanusBackendApplication.class, args);
    }

}
