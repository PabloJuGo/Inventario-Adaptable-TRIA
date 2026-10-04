package es.grupotria.inventario.auth;

import es.grupotria.inventario.dto.LoginRequest;
import es.grupotria.inventario.dto.LoginResponse;
import es.grupotria.inventario.exception.ApiException;
import es.grupotria.inventario.model.Usuario;
import es.grupotria.inventario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordHasher passwordHasher;
    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();
    private final long ttlHours;

    public AuthService(UsuarioRepository usuarios, PasswordHasher passwordHasher,
                       @Value("${app.auth.token-ttl-hours:12}") long ttlHours) {
        this.usuarios = usuarios;
        this.passwordHasher = passwordHasher;
        this.ttlHours = ttlHours;
    }

    public LoginResponse login(LoginRequest request) {
        cleanup();
        Usuario user = usuarios.findByEmail(request.email().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Email o contraseña incorrectos."));
        if (!user.activo() || !passwordHasher.matches(request.password(), user.passwordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "Email o contraseña incorrectos.");
        }
        String token = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        Instant expiry = Instant.now().plus(Duration.ofHours(ttlHours));
        sessions.put(token, new AuthSession(token, user, expiry));
        return new LoginResponse(token, user, Duration.between(Instant.now(), expiry).toSeconds());
    }

    public Usuario resolve(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "NO_AUTENTICADO", "Debes iniciar sesión.");
        }
        AuthSession session = sessions.get(token);
        if (session == null || session.expired()) {
            if (session != null) sessions.remove(token);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SESION_CADUCADA", "La sesión ha caducado. Inicia sesión de nuevo.");
        }
        Usuario fresh = usuarios.findById(session.usuario().id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USUARIO_NO_DISPONIBLE", "El usuario ya no existe."));
        if (!fresh.activo()) {
            sessions.remove(token);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USUARIO_INACTIVO", "El usuario está desactivado.");
        }
        return fresh;
    }

    public void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    public void invalidateUser(long userId) {
        sessions.entrySet().removeIf(e -> e.getValue().usuario().id().equals(userId));
    }

    private void cleanup() {
        sessions.entrySet().removeIf(e -> e.getValue().expired());
    }
}
