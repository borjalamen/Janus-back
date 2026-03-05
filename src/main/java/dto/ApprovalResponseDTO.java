package dto;
 
import com.janushub.model.Unete;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
/**
 * DTO de respuesta cuando se aprueba una solicitud de "unete".
 * Incluye la petición actualizada y las credenciales del usuario creado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponseDTO {
   
    /**
     * La petición actualizada con estado APROBADA
     */
    private Unete request;
   
    /**
     * Credenciales del usuario creado
     */
    private UserCredentials credentials;
   
    /**
     * Clase interna para las credenciales
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserCredentials {
        private String username;
        private String password;
        private String email;
    }
}
 