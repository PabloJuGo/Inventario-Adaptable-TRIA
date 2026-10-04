package es.grupotria.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MovimientoRequest(
        @NotBlank @Pattern(regexp = "ENTRADA|SALIDA|AJUSTE") String tipo,
        @NotNull Long productoId,
        Long loteId,
        @NotNull Integer cantidad,
        @NotBlank @Size(max = 150) String motivo,
        @Size(max = 150) String referencia
) {}
