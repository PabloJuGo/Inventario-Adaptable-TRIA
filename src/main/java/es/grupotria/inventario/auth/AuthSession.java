package es.grupotria.inventario.auth;

import es.grupotria.inventario.model.Usuario;

import java.time.Instant;

public record AuthSession(String token, Usuario usuario, Instant expiresAt) {
    public boolean expired() { return Instant.now().isAfter(expiresAt); }
}
