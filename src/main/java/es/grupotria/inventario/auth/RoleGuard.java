package es.grupotria.inventario.auth;

import es.grupotria.inventario.exception.ApiException;
import es.grupotria.inventario.model.Usuario;
import org.springframework.http.HttpStatus;

public final class RoleGuard {
    private RoleGuard() {}

    public static void requireAdmin(Usuario user) {
        if (user == null || !"ADMIN".equals(user.rol())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SIN_PERMISO", "Esta operación requiere permisos de administrador.");
        }
    }

    public static void requireWrite(Usuario user) {
        if (user == null || "CONSULTA".equals(user.rol())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SOLO_LECTURA", "El usuario de consulta no puede modificar datos.");
        }
    }
}
