package es.grupotria.inventario.model;

public record Alerta(
        Long id,
        String tipo,
        Long productoId,
        String productoNombre,
        Long loteId,
        String numeroLote,
        String mensaje,
        String fecha,
        boolean leida,
        boolean resuelta,
        String prioridad
) {}
