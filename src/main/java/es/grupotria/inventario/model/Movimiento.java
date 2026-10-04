package es.grupotria.inventario.model;

public record Movimiento(
        Long id,
        String tipo,
        Long productoId,
        String productoNombre,
        String productoSku,
        Long loteId,
        String numeroLote,
        Long usuarioId,
        String usuarioNombre,
        int cantidad,
        String fecha,
        String motivo,
        String referencia
) {}
