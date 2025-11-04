// Archivo: src/main/java/com/janushub/JanusBackendApplication.java
package com.janushub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        // Esto inicializa todo el framework de Spring Boot, incluyendo
        // el servidor web, el logger, y las dependencias (MongoDB, GraphQL).
        SpringApplication.run(App.class, args);
    }

}
