package es.grupotria.inventario.repository;

import es.grupotria.inventario.model.Alerta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AlertaRepository {
    private final JdbcTemplate jdbc;
    public AlertaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String BASE = """
            SELECT a.*, p.nombre AS producto_nombre, l.numero_lote AS numero_lote
            FROM alertas a
            LEFT JOIN productos p ON p.id=a.producto_id
            LEFT JOIN lotes l ON l.id=a.lote_id
            """;

    private Alerta map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        Long productoId = rs.getObject("producto_id") == null ? null : rs.getLong("producto_id");
        Long loteId = rs.getObject("lote_id") == null ? null : rs.getLong("lote_id");
        return new Alerta(rs.getLong("id"), rs.getString("tipo"), productoId, rs.getString("producto_nombre"),
                loteId, rs.getString("numero_lote"), rs.getString("mensaje"), rs.getString("fecha"),
                rs.getInt("leida") == 1, rs.getInt("resuelta") == 1, rs.getString("prioridad"));
    }

    public List<Alerta> findAll(Boolean resuelta, Boolean leida, String tipo) {
        StringBuilder sql = new StringBuilder(BASE).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (resuelta != null) { sql.append(" AND a.resuelta=?"); args.add(resuelta ? 1 : 0); }
        if (leida != null) { sql.append(" AND a.leida=?"); args.add(leida ? 1 : 0); }
        if (tipo != null && !tipo.isBlank()) { sql.append(" AND a.tipo=?"); args.add(tipo); }
        sql.append(" ORDER BY a.resuelta, CASE a.prioridad WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 ELSE 3 END, datetime(a.fecha) DESC");
        return jdbc.query(sql.toString(), this::map, args.toArray());
    }

    public Optional<Alerta> findOpen(String tipo, Long productoId, Long loteId) {
        String sql = BASE + " WHERE a.tipo=? AND a.resuelta=0 AND " +
                (productoId == null ? "a.producto_id IS NULL" : "a.producto_id=?") + " AND " +
                (loteId == null ? "a.lote_id IS NULL" : "a.lote_id=?") + " LIMIT 1";
        List<Object> args = new ArrayList<>();
        args.add(tipo);
        if (productoId != null) args.add(productoId);
        if (loteId != null) args.add(loteId);
        return jdbc.query(sql, this::map, args.toArray()).stream().findFirst();
    }

    public long insert(String tipo, Long productoId, Long loteId, String mensaje, String prioridad) {
        return JdbcIds.insert(jdbc,
                "INSERT INTO alertas(tipo,producto_id,lote_id,mensaje,prioridad) VALUES(?,?,?,?,?)",
                java.util.Arrays.asList(tipo, productoId, loteId, mensaje, prioridad));
    }

    public void markRead(long id, boolean value) {
        jdbc.update("UPDATE alertas SET leida=? WHERE id=?", value ? 1 : 0, id);
    }

    public void resolve(long id, boolean value) {
        jdbc.update("UPDATE alertas SET resuelta=?,leida=CASE WHEN ?=1 THEN 1 ELSE leida END WHERE id=?",
                value ? 1 : 0, value ? 1 : 0, id);
    }

    public void resolveOpen(String tipo, Long productoId, Long loteId) {
        StringBuilder sql = new StringBuilder("UPDATE alertas SET resuelta=1,leida=1 WHERE tipo=? AND resuelta=0");
        List<Object> args = new ArrayList<>(); args.add(tipo);
        if (productoId == null) sql.append(" AND producto_id IS NULL"); else { sql.append(" AND producto_id=?"); args.add(productoId); }
        if (loteId == null) sql.append(" AND lote_id IS NULL"); else { sql.append(" AND lote_id=?"); args.add(loteId); }
        jdbc.update(sql.toString(), args.toArray());
    }

    public void resolveExpiryExcept(List<Long> loteIds) {
        String sql = "UPDATE alertas SET resuelta=1,leida=1 WHERE tipo='CADUCIDAD' AND resuelta=0";
        if (!loteIds.isEmpty()) {
            sql += " AND (lote_id IS NULL OR lote_id NOT IN (" + loteIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + "))";
        }
        jdbc.update(sql);
    }

    public int countOpen() {
        Integer v = jdbc.queryForObject("SELECT COUNT(*) FROM alertas WHERE resuelta=0", Integer.class);
        return v == null ? 0 : v;
    }

    public List<Alerta> recentOpen(int limit) {
        return jdbc.query(BASE + " WHERE a.resuelta=0 ORDER BY CASE a.prioridad WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 ELSE 3 END, datetime(a.fecha) DESC LIMIT ?",
                this::map, limit);
    }
}
