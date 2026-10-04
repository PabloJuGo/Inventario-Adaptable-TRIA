package es.grupotria.inventario.service;

import es.grupotria.inventario.dto.MovimientoRequest;
import es.grupotria.inventario.exception.ApiException;
import es.grupotria.inventario.model.Lote;
import es.grupotria.inventario.model.Movimiento;
import es.grupotria.inventario.model.Producto;
import es.grupotria.inventario.repository.LoteRepository;
import es.grupotria.inventario.repository.MovimientoRepository;
import es.grupotria.inventario.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MovimientoService {
    private final MovimientoRepository movimientos;
    private final ProductoRepository productos;
    private final LoteRepository lotes;
    private final AlertService alertas;

    public MovimientoService(MovimientoRepository movimientos, ProductoRepository productos, LoteRepository lotes, AlertService alertas) {
        this.movimientos = movimientos; this.productos = productos; this.lotes = lotes; this.alertas = alertas;
    }

    public List<Movimiento> list(String tipo, Long productoId, String desde, String hasta, Integer limit) {
        return movimientos.findAll(tipo, productoId, desde, hasta, limit == null ? 250 : limit);
    }

    @Transactional
    public Movimiento create(MovimientoRequest r, long userId) {
        Producto product = productos.findById(r.productoId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PRODUCTO_NO_ENCONTRADO", "El producto no existe."));
        if (!"ACTIVO".equals(product.estado())) {
            throw new ApiException(HttpStatus.CONFLICT, "PRODUCTO_INACTIVO", "No se pueden registrar movimientos sobre un producto inactivo.");
        }
        int qty = r.cantidad();
        if (("ENTRADA".equals(r.tipo()) || "SALIDA".equals(r.tipo())) && qty <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANTIDAD_INVALIDA", "La cantidad debe ser mayor que cero.");
        }
        if ("AJUSTE".equals(r.tipo()) && qty == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANTIDAD_INVALIDA", "El ajuste no puede ser cero.");
        }

        Lote lot = null;
        if (r.loteId() != null) {
            lot = lotes.findById(r.loteId()).orElseThrow(() ->
                    new ApiException(HttpStatus.NOT_FOUND, "LOTE_NO_ENCONTRADO", "El lote seleccionado no existe."));
            if (!lot.productoId().equals(product.id())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "LOTE_INCOMPATIBLE", "El lote no pertenece al producto seleccionado.");
            }
        }

        if (r.loteId() == null && lotes.hasAnyForProduct(product.id())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LOTE_REQUERIDO",
                    "Este producto tiene gestión por lotes. Selecciona un lote o crea uno nuevo para registrar la entrada.");
        }

        int delta = switch (r.tipo()) {
            case "ENTRADA" -> qty;
            case "SALIDA" -> -qty;
            case "AJUSTE" -> qty;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "TIPO_INVALIDO", "Tipo de movimiento no válido.");
        };
        int newStock = product.stockActual() + delta;
        if (newStock < 0) {
            throw new ApiException(HttpStatus.CONFLICT, "STOCK_INSUFICIENTE",
                    "No hay stock suficiente. Disponible: " + product.stockActual() + " unidades.");
        }

        if (lot != null) {
            int lotNew = lot.cantidad() + delta;
            if (lotNew < 0) {
                throw new ApiException(HttpStatus.CONFLICT, "STOCK_LOTE_INSUFICIENTE",
                        "El lote solo dispone de " + lot.cantidad() + " unidades.");
            }
            lotes.setCantidad(lot.id(), lotNew);
        }

        productos.setStock(product.id(), newStock);
        long id = movimientos.insert(r.tipo(), product.id(), r.loteId(), userId, qty,
                r.motivo().trim(), r.referencia() == null ? "" : r.referencia().trim());
        alertas.refreshForProduct(product.id());
        return movimientos.findAll(null, null, null, null, 1000).stream().filter(m -> m.id() == id).findFirst()
                .orElseThrow();
    }
}
