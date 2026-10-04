package es.grupotria.inventario.model;

public record Producto(
        Long id,
        String nombre,
        String sku,
        String codigoBarras,
        String descripcion,
        int stockActual,
        int stockMinimo,
        String estado,
        Long categoriaId,
        String categoriaNombre,
        Long proveedorId,
        String proveedorNombre,
        String createdAt,
        String updatedAt
) {}
