package com.janushub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Carpeta del teu PC on es guarden avatars i CVs
    private static final String UPLOAD_ROOT =
            "C:/Users/USUARIO/Documents/GitHub/Janus-back/uploads/";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS per desenvolupament
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://[::1]:*",
                        "http://*.local",
                        "https://*.gencat.cat"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Frontend (si algun cop hi poses el build a /static)
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");

        // Fitxers pujats (avatars, CV, etc.) sota /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + UPLOAD_ROOT);
    }
}
