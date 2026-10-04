package es.grupotria.inventario.model;

public record DocumentoInventario(
        Long id,
        String nombre,
        String nombreArchivo,
        String rutaArchivo,
        String tipoArchivo,
        long tamano,
        String fechaSubida,
        Long productoId,
        String productoNombre,
        Long movimientoId,
        Long usuarioId,
        String usuarioNombre
) {}
