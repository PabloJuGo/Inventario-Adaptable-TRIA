package es.grupotria.inventario.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 200) String sku,
        @NotBlank @Pattern(regexp = "[0-9A-Za-z-]{6,32}") String codigoBarras,
        @Size(max = 500) String descripcion,
        @Min(0) Integer stockMinimo,
        @Pattern(regexp = "ACTIVO|NO_ACTIVO") String estado,
        @NotNull Long categoriaId,
        @NotNull Long proveedorId
) {}
