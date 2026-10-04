package es.grupotria.inventario.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfigUpdateRequest(@NotBlank String valor, Boolean activo) {}
