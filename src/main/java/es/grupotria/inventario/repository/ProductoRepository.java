package es.grupotria.inventario.repository;

import es.grupotria.inventario.model.Producto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductoRepository {
    private final JdbcTemplate jdbc;
    public ProductoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String BASE_SELECT = """
            SELECT p.*, c.nombre AS categoria_nombre, pr.nombre AS proveedor_nombre
            FROM productos p
            JOIN categorias c ON c.id=p.categoria_id
            JOIN proveedores pr ON pr.id=p.proveedor_id
            """;

    private Producto map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Producto(
                rs.getLong("id"), rs.getString("nombre"), rs.getString("sku"), rs.getString("codigo_barras"),
                rs.getString("descripcion"), rs.getInt("stock_actual"), rs.getInt("stock_minimo"),
                rs.getString("estado"), rs.getLong("categoria_id"), rs.getString("categoria_nombre"),
                rs.getLong("proveedor_id"), rs.getString("proveedor_nombre"),
                rs.getString("created_at"), rs.getString("updated_at"));
    }

    public List<Producto> findAll(String q, String estado, Long categoriaId, Long proveedorId) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            sql.append(" AND (lower(p.nombre) LIKE ? OR lower(p.sku) LIKE ? OR lower(p.codigo_barras) LIKE ?)");
            String like = "%" + q.toLowerCase() + "%";
            args.add(like); args.add(like); args.add(like);
        }
        if (estado != null && !estado.isBlank()) { sql.append(" AND p.estado=?"); args.add(estado); }
        if (categoriaId != null) { sql.append(" AND p.categoria_id=?"); args.add(categoriaId); }
        if (proveedorId != null) { sql.append(" AND p.proveedor_id=?"); args.add(proveedorId); }
        sql.append(" ORDER BY p.nombre COLLATE NOCASE");
        return jdbc.query(sql.toString(), this::map, args.toArray());
    }

    public Optional<Producto> findById(long id) {
        return jdbc.query(BASE_SELECT + " WHERE p.id=?", this::map, id).stream().findFirst();
    }

    public long insert(String nombre, String sku, String codigoBarras, String descripcion, int stockMinimo,
                       String estado, long categoriaId, long proveedorId) {
        return JdbcIds.insert(jdbc, """
                INSERT INTO productos(nombre,sku,codigo_barras,descripcion,stock_actual,stock_minimo,estado,categoria_id,proveedor_id)
                VALUES(?,?,?,?,0,?,?,?,?)
                """, List.of(nombre, sku, codigoBarras, descripcion == null ? "" : descripcion,
                stockMinimo, estado, categoriaId, proveedorId));
    }

    public void update(long id, String nombre, String sku, String codigoBarras, String descripcion, int stockMinimo,
                       String estado, long categoriaId, long proveedorId) {
        jdbc.update("""
                UPDATE productos SET nombre=?,sku=?,codigo_barras=?,descripcion=?,stock_minimo=?,estado=?,categoria_id=?,proveedor_id=?,updated_at=CURRENT_TIMESTAMP
                WHERE id=?
                """, nombre, sku, codigoBarras, descripcion == null ? "" : descripcion, stockMinimo,
                estado, categoriaId, proveedorId, id);
    }

    public void setEstado(long id, String estado) {
        jdbc.update("UPDATE productos SET estado=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", estado, id);
    }

    public void setStock(long id, int stock) {
        jdbc.update("UPDATE productos SET stock_actual=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", stock, id);
    }

    public long count() {
        Long v = jdbc.queryForObject("SELECT COUNT(*) FROM productos", Long.class);
        return v == null ? 0 : v;
    }

    public int totalStock() {
        Integer v = jdbc.queryForObject("SELECT COALESCE(SUM(stock_actual),0) FROM productos WHERE estado='ACTIVO'", Integer.class);
        return v == null ? 0 : v;
    }

    public int countLowStock() {
        Integer v = jdbc.queryForObject("SELECT COUNT(*) FROM productos WHERE estado='ACTIVO' AND stock_actual<=stock_minimo", Integer.class);
        return v == null ? 0 : v;
    }
}
