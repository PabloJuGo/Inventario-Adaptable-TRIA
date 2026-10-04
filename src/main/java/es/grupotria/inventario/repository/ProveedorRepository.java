package es.grupotria.inventario.repository;

import es.grupotria.inventario.model.Proveedor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProveedorRepository {
    private final JdbcTemplate jdbc;
    public ProveedorRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private Proveedor map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Proveedor(rs.getLong("id"), rs.getString("nombre"), rs.getString("contacto"),
                rs.getString("email"), rs.getString("telefono"), rs.getString("descripcion"), rs.getInt("activo") == 1);
    }

    public List<Proveedor> findAll(boolean onlyActive) {
        String sql = "SELECT * FROM proveedores" + (onlyActive ? " WHERE activo=1" : "") + " ORDER BY nombre COLLATE NOCASE";
        return jdbc.query(sql, this::map);
    }

    public Optional<Proveedor> findById(long id) {
        return jdbc.query("SELECT * FROM proveedores WHERE id=?", this::map, id).stream().findFirst();
    }

    public long insert(String nombre, String contacto, String email, String telefono, String descripcion) {
        return JdbcIds.insert(jdbc,
                "INSERT INTO proveedores(nombre,contacto,email,telefono,descripcion,activo) VALUES(?,?,?,?,?,1)",
                List.of(nombre, n(contacto), n(email), n(telefono), n(descripcion)));
    }

    private String n(String value) { return value == null ? "" : value; }

    public long count() {
        Long v = jdbc.queryForObject("SELECT COUNT(*) FROM proveedores", Long.class);
        return v == null ? 0 : v;
    }
}
