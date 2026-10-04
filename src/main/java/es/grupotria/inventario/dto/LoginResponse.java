package es.grupotria.inventario.dto;

import es.grupotria.inventario.model.Usuario;

public record LoginResponse(String token, Usuario usuario, long expiresInSeconds) {}
