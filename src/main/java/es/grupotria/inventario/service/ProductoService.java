package es.grupotria.inventario.service;

import es.grupotria.inventario.dto.ProductoRequest;
import es.grupotria.inventario.exception.ApiException;
import es.grupotria.inventario.model.Producto;
import es.grupotria.inventario.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {
    private final ProductoRepository productos;
    private final CatalogoService catalogos;
    private final AlertService alertas;

    public ProductoService(ProductoRepository productos, CatalogoService catalogos, AlertService alertas) {
        this.productos = productos; this.catalogos = catalogos; this.alertas = alertas;
    }

    public List<Producto> list(String q, String estado, Long categoriaId, Long proveedorId) {
        return productos.findAll(q, estado, categoriaId, proveedorId);
    }

    public Producto get(long id) {
        return productos.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PRODUCTO_NO_ENCONTRADO", "El producto no existe."));
    }

    @Transactional
    public Producto create(ProductoRequest r) {
        catalogos.assertCategoria(r.categoriaId());
        catalogos.assertProveedor(r.proveedorId());
        String estado = r.estado() == null ? "ACTIVO" : r.estado();
        int min = r.stockMinimo() == null ? 0 : r.stockMinimo();
        long id = productos.insert(r.nombre().trim(), r.sku().trim(), r.codigoBarras().trim(), r.descripcion(), min,
                estado, r.categoriaId(), r.proveedorId());
        alertas.refreshForProduct(id);
        return get(id);
    }

    @Transactional
    public Producto update(long id, ProductoRequest r) {
        get(id);
        catalogos.assertCategoria(r.categoriaId());
        catalogos.assertProveedor(r.proveedorId());
        String estado = r.estado() == null ? "ACTIVO" : r.estado();
        int min = r.stockMinimo() == null ? 0 : r.stockMinimo();
        productos.update(id, r.nombre().trim(), r.sku().trim(), r.codigoBarras().trim(), r.descripcion(), min,
                estado, r.categoriaId(), r.proveedorId());
        alertas.refreshForProduct(id);
        return get(id);
    }

    @Transactional
    public Producto setEstado(long id, String estado) {
        get(id);
        if (!"ACTIVO".equals(estado) && !"NO_ACTIVO".equals(estado)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ESTADO_INVALIDO", "Estado de producto no válido.");
        }
        productos.setEstado(id, estado);
        alertas.refreshForProduct(id);
        return get(id);
    }
}
