package es.grupotria.inventario.repository;

import es.grupotria.inventario.model.DocumentoInventario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DocumentoRepository {
    private final JdbcTemplate jdbc;
    public DocumentoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String BASE = """
            SELECT d.*, p.nombre AS producto_nombre, u.nombre AS usuario_nombre
            FROM documentos_inventario d
            LEFT JOIN productos p ON p.id=d.producto_id
            JOIN usuarios u ON u.id=d.usuario_id
            """;

    private DocumentoInventario map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Long productId = rs.getObject("producto_id") == null ? null : rs.getLong("producto_id");
        Long movementId = rs.getObject("movimiento_id") == null ? null : rs.getLong("movimiento_id");
        return new DocumentoInventario(rs.getLong("id"), rs.getString("nombre"), rs.getString("nombre_archivo"),
                rs.getString("ruta_archivo"), rs.getString("tipo_archivo"), rs.getLong("tamano"),
                rs.getString("fecha_subida"), productId, rs.getString("producto_nombre"), movementId,
                rs.getLong("usuario_id"), rs.getString("usuario_nombre"));
    }

    public List<DocumentoInventario> findAll(Long productoId) {
        if (productoId == null) return jdbc.query(BASE + " ORDER BY datetime(d.fecha_subida) DESC", this::map);
        return jdbc.query(BASE + " WHERE d.producto_id=? ORDER BY datetime(d.fecha_subida) DESC", this::map, productoId);
    }

    public Optional<DocumentoInventario> findById(long id) {
        return jdbc.query(BASE + " WHERE d.id=?", this::map, id).stream().findFirst();
    }

    public long insert(String nombre, String nombreArchivo, String rutaArchivo, String tipoArchivo, long tamano,
                       Long productoId, Long movimientoId, long usuarioId) {
        return JdbcIds.insert(jdbc, """
                INSERT INTO documentos_inventario(nombre,nombre_archivo,ruta_archivo,tipo_archivo,tamano,producto_id,movimiento_id,usuario_id)
                VALUES(?,?,?,?,?,?,?,?)
                """, java.util.Arrays.asList(nombre, nombreArchivo, rutaArchivo, tipoArchivo, tamano,
                productoId, movimientoId, usuarioId));
    }

    public void delete(long id) { jdbc.update("DELETE FROM documentos_inventario WHERE id=?", id); }
}
