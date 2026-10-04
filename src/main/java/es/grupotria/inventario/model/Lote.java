package es.grupotria.inventario.model;

public record Lote(
        Long id,
        Long productoId,
        String productoNombre,
        String numeroLote,
        String fechaEntrada,
        String fechaCaducidad,
        int vidaUtilDias,
        int cantidad,
        String estado,
        String createdAt
) {}
