package es.grupotria.inventario.service;

import es.grupotria.inventario.repository.AlertaRepository;
import es.grupotria.inventario.repository.LoteRepository;
import es.grupotria.inventario.repository.MovimientoRepository;
import es.grupotria.inventario.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {
    private final ProductoRepository productos;
    private final MovimientoRepository movimientos;
    private final AlertaRepository alertas;
    private final LoteRepository lotes;
    private final AlertService alertService;

    public DashboardService(ProductoRepository productos, MovimientoRepository movimientos,
                            AlertaRepository alertas, LoteRepository lotes, AlertService alertService) {
        this.productos = productos; this.movimientos = movimientos; this.alertas = alertas; this.lotes = lotes; this.alertService = alertService;
    }

    public Map<String, Object> summary() {
        alertService.refreshAll();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("productos", productos.count());
        out.put("unidadesStock", productos.totalStock());
        out.put("stockBajo", productos.countLowStock());
        out.put("alertasActivas", alertService.countOpen());
        out.put("lotesProximos", lotes.countExpiringBefore(LocalDate.now().plusDays(30).toString()));
        out.put("movimientos", movimientos.count());
        out.put("movimientosRecientes", movimientos.recent(8));
        out.put("alertasRecientes", alertService.recentOpen(6));
        return out;
    }
}
