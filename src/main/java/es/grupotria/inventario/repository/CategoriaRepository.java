package es.grupotria.inventario.repository;

import es.grupotria.inventario.model.Categoria;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoriaRepository {
    private final JdbcTemplate jdbc;
    public CategoriaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private Categoria map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Categoria(rs.getLong("id"), rs.getString("nombre"), rs.getString("descripcion"), rs.getInt("activo") == 1);
    }

    public List<Categoria> findAll(boolean onlyActive) {
        String sql = "SELECT * FROM categorias" + (onlyActive ? " WHERE activo=1" : "") + " ORDER BY nombre COLLATE NOCASE";
        return jdbc.query(sql, this::map);
    }

    public Optional<Categoria> findById(long id) {
        return jdbc.query("SELECT * FROM categorias WHERE id=?", this::map, id).stream().findFirst();
    }

    public long insert(String nombre, String descripcion) {
        return JdbcIds.insert(jdbc, "INSERT INTO categorias(nombre,descripcion,activo) VALUES(?,?,1)",
                List.of(nombre, descripcion == null ? "" : descripcion));
    }

    public long count() {
        Long v = jdbc.queryForObject("SELECT COUNT(*) FROM categorias", Long.class);
        return v == null ? 0 : v;
    }
}
