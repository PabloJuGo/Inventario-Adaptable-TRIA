package es.grupotria.inventario.repository;

import es.grupotria.inventario.model.Configuracion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ConfiguracionRepository {
    private final JdbcTemplate jdbc;
    public ConfiguracionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private Configuracion map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Configuracion(rs.getLong("id"), rs.getString("clave"), rs.getString("valor"),
                rs.getString("descripcion"), rs.getInt("activo") == 1);
    }

    public List<Configuracion> findAll() {
        return jdbc.query("SELECT * FROM configuracion_sistema ORDER BY id", this::map);
    }

    public Optional<Configuracion> findByKey(String key) {
        return jdbc.query("SELECT * FROM configuracion_sistema WHERE clave=?", this::map, key).stream().findFirst();
    }

    public void update(String key, String value, boolean active) {
        jdbc.update("UPDATE configuracion_sistema SET valor=?,activo=? WHERE clave=?", value, active ? 1 : 0, key);
    }
}
