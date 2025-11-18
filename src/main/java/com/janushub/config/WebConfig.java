package com.janushub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        
        // Esta configuración permite al frontend conectarse
        registry.addMapping("/**") // Permite CORS para todas las rutas (/api/auth/**, /api/config/**)
            
            // Aquí pones los orígenes de tu frontend
            // (Ej. 3000 para React, 5173 para Vite, 4200 para Angular)
            .allowedOrigins("http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:3000", "http://localhost:4200") 
            
            // Métodos permitidos
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") 
            
            // Cabeceras permitidas (Content-Type es clave para el login)
            .allowedHeaders("*") 
            
            .allowCredentials(true);
    }
}