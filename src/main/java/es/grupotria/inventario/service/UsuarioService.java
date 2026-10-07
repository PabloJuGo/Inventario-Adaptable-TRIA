package es.grupotria.inventario.service;

import es.grupotria.inventario.auth.AuthService;
import es.grupotria.inventario.auth.PasswordHasher;
import es.grupotria.inventario.dto.UsuarioRequest;
import es.grupotria.inventario.exception.ApiException;
import es.grupotria.inventario.model.Usuario;
import es.grupotria.inventario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final PasswordHasher hasher;
    private final AuthService auth;

    public UsuarioService(UsuarioRepository usuarios, PasswordHasher hasher, AuthService auth) {
        this.usuarios = usuarios; this.hasher = hasher; this.auth = auth;
    }

    public List<Usuario> list() { return usuarios.findAll(); }

    public Usuario get(long id) {
        return usuarios.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USUARIO_NO_ENCONTRADO", "El usuario no existe."));
    }

    @Transactional
    public Usuario create(UsuarioRequest r) {
        if (r.password() == null || r.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PASSWORD_REQUERIDA", "Indica una contraseña de al menos 8 caracteres.");
        }
        boolean active = r.activo() == null || r.activo();
        long id = usuarios.insert(r.nombre().trim(), r.email().trim().toLowerCase(), hasher.hash(r.password()), r.rol(), active);
        return get(id);
    }

    @Transactional
    public Usuario update(long id, UsuarioRequest r, long currentAdminId) {
        Usuario current = get(id);
        boolean active = r.activo() == null ? current.activo() : r.activo();
        if (id == currentAdminId && !active) {
            throw new ApiException(HttpStatus.CONFLICT, "AUTO_DESACTIVACION", "No puedes desactivar tu propia cuenta mientras la estás utilizando.");
        }
        if (id == currentAdminId && !"ADMIN".equals(r.rol())) {
            throw new ApiException(HttpStatus.CONFLICT, "AUTO_DEGRADACION", "No puedes quitarte a ti mismo el rol de administrador.");
        }
        usuarios.update(id, r.nombre().trim(), r.email().trim().toLowerCase(), r.rol(), active);
        if (r.password() != null && !r.password().isBlank()) usuarios.updatePassword(id, hasher.hash(r.password()));
        if (!active || !current.rol().equals(r.rol())) auth.invalidateUser(id);
        return get(id);
    }
}
