package es.grupotria.inventario.repository;

import es.grupotria.inventario.model.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioRepository {
    private final JdbcTemplate jdbc;

    public UsuarioRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private Usuario map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Usuario(
                rs.getLong("id"), rs.getString("nombre"), rs.getString("email"),
                rs.getString("password_hash"), rs.getString("rol"), rs.getInt("activo") == 1,
                rs.getString("created_at"));
    }

    public Optional<Usuario> findByEmail(String email) {
        return jdbc.query("SELECT * FROM usuarios WHERE lower(email)=lower(?)", this::map, email)
                .stream().findFirst();
    }

    public Optional<Usuario> findById(long id) {
        return jdbc.query("SELECT * FROM usuarios WHERE id=?", this::map, id).stream().findFirst();
    }

    public List<Usuario> findAll() {
        return jdbc.query("SELECT * FROM usuarios ORDER BY nombre COLLATE NOCASE", this::map);
    }

    public long insert(String nombre, String email, String passwordHash, String rol, boolean activo) {
        return JdbcIds.insert(jdbc,
                "INSERT INTO usuarios(nombre,email,password_hash,rol,activo) VALUES(?,?,?,?,?)",
                List.of(nombre, email, passwordHash, rol, activo ? 1 : 0));
    }

    public void update(long id, String nombre, String email, String rol, boolean activo) {
        jdbc.update("UPDATE usuarios SET nombre=?, email=?, rol=?, activo=? WHERE id=?",
                nombre, email, rol, activo ? 1 : 0, id);
    }

    public void updatePassword(long id, String passwordHash) {
        jdbc.update("UPDATE usuarios SET password_hash=? WHERE id=?", passwordHash, id);
    }

    public long count() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Long.class);
        return count == null ? 0 : count;
    }
}
