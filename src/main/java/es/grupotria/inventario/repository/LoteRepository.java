package es.grupotria.inventario.repository;

import es.grupotria.inventario.model.Lote;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class LoteRepository {
    private final JdbcTemplate jdbc;
    public LoteRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String BASE = """
            SELECT l.*, p.nombre AS producto_nombre
            FROM lotes l JOIN productos p ON p.id=l.producto_id
            """;

    private Lote map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Lote(rs.getLong("id"), rs.getLong("producto_id"), rs.getString("producto_nombre"),
                rs.getString("numero_lote"), rs.getString("fecha_entrada"), rs.getString("fecha_caducidad"),
                rs.getInt("vida_util_dias"), rs.getInt("cantidad"), rs.getString("estado"), rs.getString("created_at"));
    }

    public List<Lote> findAll(Long productoId, String estado, boolean onlyWithStock) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (productoId != null) { sql.append(" AND l.producto_id=?"); args.add(productoId); }
        if (estado != null && !estado.isBlank()) { sql.append(" AND l.estado=?"); args.add(estado); }
        if (onlyWithStock) sql.append(" AND l.cantidad>0");
        sql.append(" ORDER BY CASE WHEN l.fecha_caducidad IS NULL THEN 1 ELSE 0 END, l.fecha_caducidad, l.fecha_entrada DESC");
        return jdbc.query(sql.toString(), this::map, args.toArray());
    }

    public Optional<Lote> findById(long id) {
        return jdbc.query(BASE + " WHERE l.id=?", this::map, id).stream().findFirst();
    }

    public long insert(long productoId, String numeroLote, String fechaEntrada, String fechaCaducidad,
                       int vidaUtilDias, int cantidad, String estado) {
        return JdbcIds.insert(jdbc, """
                INSERT INTO lotes(producto_id,numero_lote,fecha_entrada,fecha_caducidad,vida_util_dias,cantidad,estado)
                VALUES(?,?,?,?,?,?,?)
                """, java.util.Arrays.asList(productoId, numeroLote, fechaEntrada,
                fechaCaducidad == null || fechaCaducidad.isBlank() ? null : fechaCaducidad,
                vidaUtilDias, cantidad, estado));
    }

    public void setCantidad(long id, int cantidad) {
        jdbc.update("UPDATE lotes SET cantidad=? WHERE id=?", cantidad, id);
    }

    public int totalStockByProduct(long productId) {
        Integer v = jdbc.queryForObject("SELECT COALESCE(SUM(cantidad),0) FROM lotes WHERE producto_id=? AND estado='ACTIVO'", Integer.class, productId);
        return v == null ? 0 : v;
    }

    public boolean hasAnyForProduct(long productId) {
        Integer v = jdbc.queryForObject("SELECT COUNT(*) FROM lotes WHERE producto_id=? AND estado='ACTIVO'", Integer.class, productId);
        return v != null && v > 0;
    }

    private static final String EFFECTIVE_EXPIRY = "CASE WHEN l.fecha_caducidad IS NOT NULL THEN date(l.fecha_caducidad) " +
            "WHEN l.vida_util_dias>0 THEN date(l.fecha_entrada, '+' || l.vida_util_dias || ' days') ELSE NULL END";

    public List<Lote> expiringBefore(String maxDate) {
        return jdbc.query(BASE + " WHERE l.estado='ACTIVO' AND l.cantidad>0 AND (" + EFFECTIVE_EXPIRY + ") IS NOT NULL " +
                        "AND (" + EFFECTIVE_EXPIRY + ")<=date(?) ORDER BY (" + EFFECTIVE_EXPIRY + ")",
                this::map, maxDate);
    }

    public long countExpiringBefore(String maxDate) {
        String effective = "CASE WHEN fecha_caducidad IS NOT NULL THEN date(fecha_caducidad) " +
                "WHEN vida_util_dias>0 THEN date(fecha_entrada, '+' || vida_util_dias || ' days') ELSE NULL END";
        Long v = jdbc.queryForObject("SELECT COUNT(*) FROM lotes WHERE estado='ACTIVO' AND cantidad>0 AND (" + effective + ") IS NOT NULL AND (" + effective + ")<=date(?)", Long.class, maxDate);
        return v == null ? 0 : v;
    }
}
