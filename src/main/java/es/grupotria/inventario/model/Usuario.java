package es.grupotria.inventario.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Usuario(
        Long id,
        String nombre,
        String email,
        @JsonIgnore String passwordHash,
        String rol,
        boolean activo,
        String createdAt
) {}
