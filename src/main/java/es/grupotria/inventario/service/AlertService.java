package es.grupotria.inventario.service;

import es.grupotria.inventario.dto.IncidenciaRequest;
import es.grupotria.inventario.exception.ApiException;
import es.grupotria.inventario.model.Alerta;
import es.grupotria.inventario.model.Lote;
import es.grupotria.inventario.model.Producto;
import es.grupotria.inventario.repository.AlertaRepository;
import es.grupotria.inventario.repository.ConfiguracionRepository;
import es.grupotria.inventario.repository.LoteRepository;
import es.grupotria.inventario.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AlertService {
    private final AlertaRepository alertas;
    private final ProductoRepository productos;
    private final LoteRepository lotes;
    private final ConfiguracionRepository config;

    public AlertService(AlertaRepository alertas, ProductoRepository productos, LoteRepository lotes,
                        ConfiguracionRepository config) {
        this.alertas = alertas; this.productos = productos; this.lotes = lotes; this.config = config;
    }

    public List<Alerta> list(Boolean resuelta, Boolean leida, String tipo) {
        if (!isModuleEnabled()) return List.of();
        refreshAll();
        return alertas.findAll(resuelta, leida, tipo);
    }

    @Transactional
    public void refreshAll() {
        if (!isModuleEnabled()) return;
        for (Producto p : productos.findAll(null, "ACTIVO", null, null)) refreshStock(p);
        refreshExpiry();
    }

    @Transactional
    public void refreshForProduct(long productId) {
        if (!isModuleEnabled()) return;
        Producto p = productos.findById(productId).orElse(null);
        if (p != null) refreshStock(p);
        refreshExpiry();
    }

    private void refreshStock(Producto p) {
        boolean low = "ACTIVO".equals(p.estado()) && p.stockActual() <= p.stockMinimo();
        if (low) {
            if (alertas.findOpen("STOCK_BAJO", p.id(), null).isEmpty()) {
                String msg = p.stockActual() == 0
                        ? "Sin stock disponible para " + p.nombre() + "."
                        : "Stock bajo de " + p.nombre() + ": " + p.stockActual() + " uds. (mínimo " + p.stockMinimo() + ").";
                alertas.insert("STOCK_BAJO", p.id(), null, msg, p.stockActual() == 0 ? "ALTA" : "MEDIA");
            }
        } else {
            alertas.resolveOpen("STOCK_BAJO", p.id(), null);
        }
    }

    private void refreshExpiry() {
        if (!isModuleEnabled()) return;
        int days = expiryDays();
        LocalDate today = LocalDate.now();
        LocalDate max = today.plusDays(days);
        List<Lote> candidates = lotes.expiringBefore(max.toString());
        // si un lote ya no está en la lista (sin stock o desactivado) su alerta se cierra
        alertas.resolveExpiryExcept(candidates.stream().map(Lote::id).toList());
        for (Lote lote : candidates) {
            LocalDate expiry;
            try {
                if (lote.fechaCaducidad() != null && !lote.fechaCaducidad().isBlank()) {
                    expiry = LocalDate.parse(lote.fechaCaducidad());
                } else if (lote.vidaUtilDias() > 0) {
                    expiry = LocalDate.parse(lote.fechaEntrada()).plusDays(lote.vidaUtilDias());
                } else {
                    continue;
                }
            } catch (Exception ex) { continue; }
            long diff = ChronoUnit.DAYS.between(today, expiry);
            String priority = diff < 0 ? "ALTA" : diff <= 7 ? "ALTA" : "MEDIA";
            String message = diff < 0
                    ? "El lote " + lote.numeroLote() + " de " + lote.productoNombre() + " está caducado."
                    : "El lote " + lote.numeroLote() + " de " + lote.productoNombre() + " caduca en " + diff + " días.";
            if (alertas.findOpen("CADUCIDAD", lote.productoId(), lote.id()).isEmpty()) {
                alertas.insert("CADUCIDAD", lote.productoId(), lote.id(), message, priority);
            }
        }
    }

    @Transactional
    public Alerta createIncident(IncidenciaRequest request) {
        if (!isModuleEnabled()) {
            throw new ApiException(HttpStatus.CONFLICT, "MODULO_DESACTIVADO", "El módulo de alertas está desactivado.");
        }
        Long productId = request.productoId();
        if (productId != null && productos.findById(productId).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PRODUCTO_INVALIDO", "El producto asociado no existe.");
        }
        String priority = request.prioridad() == null || request.prioridad().isBlank() ? "MEDIA" : request.prioridad();
        long id = alertas.insert("INCIDENCIA", productId, null, request.mensaje().trim(), priority);
        return find(id);
    }

    public Alerta markRead(long id, boolean value) {
        Alerta a = find(id);
        alertas.markRead(id, value);
        return find(id);
    }

    public Alerta resolve(long id, boolean value) {
        find(id);
        alertas.resolve(id, value);
        return find(id);
    }

    private Alerta find(long id) {
        return alertas.findAll(null, null, null).stream().filter(a -> a.id() == id).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ALERTA_NO_ENCONTRADA", "La alerta no existe."));
    }

    public int countOpen() { return isModuleEnabled() ? alertas.countOpen() : 0; }

    public List<Alerta> recentOpen(int limit) {
        return isModuleEnabled() ? alertas.recentOpen(limit) : List.of();
    }

    private int expiryDays() {
        return config.findByKey("dias_alerta_caducidad")
                .map(c -> {
                    try { return Math.max(1, Integer.parseInt(c.valor())); } catch (Exception e) { return 30; }
                }).orElse(30);
    }

    private boolean isModuleEnabled() {
        return config.findByKey("modulo_alertas").map(c -> c.activo() && Boolean.parseBoolean(c.valor())).orElse(true);
    }
}
