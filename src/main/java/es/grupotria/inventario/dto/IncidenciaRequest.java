package es.grupotria.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IncidenciaRequest(
        Long productoId,
        @NotBlank @Size(max = 300) String mensaje,
        @Pattern(regexp = "ALTA|MEDIA|BAJA") String prioridad
) {}
