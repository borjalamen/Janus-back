package com.janushub.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Solucion {
  private int numero;
    private String descripcion;
    private List<String> entorno;
    private String archivoNombre;
    private String archivoBase64;
    private String archivoTipo;
}
