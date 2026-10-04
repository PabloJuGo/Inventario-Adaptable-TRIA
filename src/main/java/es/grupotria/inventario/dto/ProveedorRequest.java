package es.grupotria.inventario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank @Size(max=100) String nombre,
        @Size(max=100) String contacto,
        @Email @Size(max=150) String email,
        @Size(max=30) String telefono,
        @Size(max=300) String descripcion
) {}
