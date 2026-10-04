package es.grupotria.inventario.model;

public record Proveedor(
        Long id, String nombre, String contacto, String email, String telefono,
        String descripcion, boolean activo
) {}
