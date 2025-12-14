package com.janushub.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "infraestructura")
public class Infraestructura {
@Id
    private String id;

    private boolean deleted;
    private List<String> codProyecto;
    private String ip;
    private String estado;
    private String so;
    private String firewall;
    private String cpu;
    private String ram;
    private String capacidad;
    private List<String> tags;
    private String cpd;
    private String host;

    // Arrays de Objetos Anidados
    private List<AuthItem> auth;
    private List<JdkItem> jdk;
    private List<ServiceItem> services;

    // Objeto CRC (Complejo)
    private CrcData crc;

    // --- CLASES INTERNAS PARA LOS OBJETOS ANIDADOS ---

    @Data
    @NoArgsConstructor
    public static class AuthItem {
        private String user;
        private String passw;
    }

    @Data
    @NoArgsConstructor
    public static class JdkItem {
        private String nombre;
        private String ruta;
    }

    @Data
    @NoArgsConstructor
    public static class ServiceItem {
        private String nombre;
        private String ruta;
    }

    @Data
    @NoArgsConstructor
    public static class CrcData {
        private String cpu;
        private String ram;
        private String capacidad;
        private List<AuthItem> auth; // Reutilizamos AuthItem
        private List<OtherItem> other;
    }

    @Data
    @NoArgsConstructor
    public static class OtherItem {
        private String key;
        private String value;
        private String version;
    }
}
