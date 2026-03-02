package dto;

import lombok.Data;

/**
 * DTO que recibe los datos del formulario "únete" del frontend.
 */
@Data
public class UneteDTO {
    private String fullName;
    private String email;
    private String role;
    private String projectCode;
    private String projectName;
    private String comments;
}
