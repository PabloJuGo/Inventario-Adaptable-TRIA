package es.grupotria.inventario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(min = 8, max = 100) String password,
        @NotBlank @Pattern(regexp = "ADMIN|OPERARIO|CONSULTA") String rol,
        Boolean activo
) {}
