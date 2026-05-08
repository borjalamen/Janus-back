package com.janushub.config;

import com.janushub.model.Project;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

@Configuration
public class MongoConfig {

    /**
     * Converter que permite leer ResponsableInfo desde MongoDB tanto si el campo
     * está guardado como un String (formato antiguo) como si es un Document/subdocumento
     * (formato nuevo con {nombre, email}).
     */
    @ReadingConverter
    public static class StringToResponsableInfoConverter implements Converter<String, Project.ResponsableInfo> {
        @Override
        public Project.ResponsableInfo convert(String source) {
            // Valor antiguo era un String con el nombre — lo migramos al vuelo
            if (source == null || source.isBlank()) {
                return new Project.ResponsableInfo("", "");
            }
            return new Project.ResponsableInfo(source, "");
        }
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new StringToResponsableInfoConverter()
        ));
    }
}
