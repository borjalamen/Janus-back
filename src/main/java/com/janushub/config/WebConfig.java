package com.janushub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Mejor soporte para desarrollo local en varios puertos y opciones de producción
        registry.addMapping("/**")
            // Permite orígenes localhost en cualquier puerto y algunos orígenes comunes
            .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", "http://[::1]:*", "http://*.local", "https://*.gencat.cat")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            .allowedHeaders("*")
            // Expone cabeceras útiles al cliente (p. ej. para descargas o autenticación)
            .exposedHeaders("Authorization", "Content-Disposition")
            .allowCredentials(true)
            .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Permite servir recursos estáticos si construyes el frontend dentro de /static
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}