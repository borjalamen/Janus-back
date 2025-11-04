// CÓDIGO CORREGIDO (JUnit 5)
package com.janushub;

import static org.junit.jupiter.api.Assertions.assertTrue; // Aserción de JUnit 5
import org.junit.jupiter.api.Test; // Anotación de JUnit 5

/**
 * Unit test for simple App.
 */
public class AppTest {
    
    @Test // Anotación de JUnit 5
    void shouldAnswerWithTrue() { // Los métodos de prueba ya no necesitan ser públicos
        assertTrue(true); 
    }
}