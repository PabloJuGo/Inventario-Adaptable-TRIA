package es.grupotria.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoteRequest(
        @NotNull Long productoId,
        @NotBlank @Size(max = 50) String numeroLote,
        @NotBlank String fechaEntrada,
        String fechaCaducidad,
        @Min(0) Integer vidaUtilDias,
        @Min(0) Integer cantidad,
        String estado
) {}
