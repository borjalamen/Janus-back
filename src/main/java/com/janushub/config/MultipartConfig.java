package com.janushub.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class MultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // No establecer límites aquí: se controlará por propiedades de Spring
        // Si prefieres un límite, descomenta y ajusta las siguientes líneas:
        // factory.setMaxFileSize(DataSize.ofMegabytes(100));
        // factory.setMaxRequestSize(DataSize.ofMegabytes(100));
        return factory.createMultipartConfig();
    }
}
