package com.janushub.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Solucion {
  private int numero;
    private String descripcion;
    private String entorno; // 'minsait' | 'preproduccion' | 'produccion'
    private String archivoNombre;
    private String archivoBase64;
    private String archivoTipo;
}
