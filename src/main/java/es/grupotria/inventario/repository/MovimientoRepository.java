package es.grupotria.inventario.repository;

import es.grupotria.inventario.model.Movimiento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class MovimientoRepository {
    private final JdbcTemplate jdbc;
    public MovimientoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String BASE = """
            SELECT m.*, p.nombre AS producto_nombre, p.sku AS producto_sku,
                   l.numero_lote AS numero_lote, u.nombre AS usuario_nombre
            FROM movimientos_inventario m
            JOIN productos p ON p.id=m.producto_id
            LEFT JOIN lotes l ON l.id=m.lote_id
            JOIN usuarios u ON u.id=m.usuario_id
            """;

    private Movimiento map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Long loteId = rs.getObject("lote_id") == null ? null : rs.getLong("lote_id");
        return new Movimiento(rs.getLong("id"), rs.getString("tipo"), rs.getLong("producto_id"),
                rs.getString("producto_nombre"), rs.getString("producto_sku"), loteId, rs.getString("numero_lote"),
                rs.getLong("usuario_id"), rs.getString("usuario_nombre"), rs.getInt("cantidad"),
                rs.getString("fecha"), rs.getString("motivo"), rs.getString("referencia"));
    }

    public List<Movimiento> findAll(String tipo, Long productoId, String desde, String hasta, int limit) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (tipo != null && !tipo.isBlank()) { sql.append(" AND m.tipo=?"); args.add(tipo); }
        if (productoId != null) { sql.append(" AND m.producto_id=?"); args.add(productoId); }
        if (desde != null && !desde.isBlank()) { sql.append(" AND date(m.fecha)>=date(?)"); args.add(desde); }
        if (hasta != null && !hasta.isBlank()) { sql.append(" AND date(m.fecha)<=date(?)"); args.add(hasta); }
        sql.append(" ORDER BY datetime(m.fecha) DESC, m.id DESC LIMIT ?");
        args.add(Math.max(1, Math.min(limit, 1000)));
        return jdbc.query(sql.toString(), this::map, args.toArray());
    }

    public Optional<Movimiento> findById(long id) {
        return jdbc.query(BASE + " WHERE m.id=?", this::map, id).stream().findFirst();
    }

    public long insert(String tipo, long productoId, Long loteId, long usuarioId, int cantidad, String motivo, String referencia) {
        return JdbcIds.insert(jdbc, """
                INSERT INTO movimientos_inventario(tipo,producto_id,lote_id,usuario_id,cantidad,motivo,referencia)
                VALUES(?,?,?,?,?,?,?)
                """, java.util.Arrays.asList(tipo, productoId, loteId, usuarioId, cantidad, motivo,
                referencia == null ? "" : referencia));
    }

    public List<Movimiento> recent(int limit) {
        return jdbc.query(BASE + " ORDER BY datetime(m.fecha) DESC, m.id DESC LIMIT ?", this::map, limit);
    }

    public long count() {
        Long v = jdbc.queryForObject("SELECT COUNT(*) FROM movimientos_inventario", Long.class);
        return v == null ? 0 : v;
    }
}
