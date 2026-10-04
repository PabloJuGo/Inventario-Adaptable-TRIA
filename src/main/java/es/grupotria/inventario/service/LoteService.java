package es.grupotria.inventario.service;

import es.grupotria.inventario.dto.LoteRequest;
import es.grupotria.inventario.exception.ApiException;
import es.grupotria.inventario.model.Lote;
import es.grupotria.inventario.model.Producto;
import es.grupotria.inventario.repository.LoteRepository;
import es.grupotria.inventario.repository.MovimientoRepository;
import es.grupotria.inventario.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LoteService {
    private final LoteRepository lotes;
    private final ProductoRepository productos;
    private final MovimientoRepository movimientos;
    private final AlertService alertas;

    public LoteService(LoteRepository lotes, ProductoRepository productos, MovimientoRepository movimientos, AlertService alertas) {
        this.lotes = lotes; this.productos = productos; this.movimientos = movimientos; this.alertas = alertas;
    }

    public List<Lote> list(Long productoId, String estado, boolean onlyWithStock) {
        return lotes.findAll(productoId, estado, onlyWithStock);
    }

    @Transactional
    public Lote create(LoteRequest r, long userId) {
        Producto p = productos.findById(r.productoId()).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "PRODUCTO_NO_ENCONTRADO", "El producto no existe."));
        parseDate(r.fechaEntrada(), "fecha de entrada");
        if (r.fechaCaducidad() != null && !r.fechaCaducidad().isBlank()) {
            LocalDate exp = parseDate(r.fechaCaducidad(), "fecha de caducidad");
            if (exp.isBefore(parseDate(r.fechaEntrada(), "fecha de entrada"))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "FECHAS_INVALIDAS", "La caducidad no puede ser anterior a la entrada.");
            }
        }
        int qty = r.cantidad() == null ? 0 : r.cantidad();
        int life = r.vidaUtilDias() == null ? 0 : r.vidaUtilDias();
        String state = r.estado() == null || r.estado().isBlank() ? "ACTIVO" : r.estado();
        if (!"ACTIVO".equals(state) && !"NO_ACTIVO".equals(state)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ESTADO_INVALIDO", "Estado de lote no válido.");
        }
        long id = lotes.insert(p.id(), r.numeroLote().trim(), r.fechaEntrada(), r.fechaCaducidad(), life, qty, state);
        if (qty > 0) {
            productos.setStock(p.id(), p.stockActual() + qty);
            movimientos.insert("ENTRADA", p.id(), id, userId, qty, "Alta de lote", "LOTE-" + r.numeroLote().trim());
        }
        alertas.refreshForProduct(p.id());
        return lotes.findById(id).orElseThrow();
    }

    private LocalDate parseDate(String value, String label) {
        try { return LocalDate.parse(value); }
        catch (Exception ex) { throw new ApiException(HttpStatus.BAD_REQUEST, "FECHA_INVALIDA", "La " + label + " no es válida."); }
    }
}
