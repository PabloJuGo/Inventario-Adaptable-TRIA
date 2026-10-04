package es.grupotria.inventario.controller;

import es.grupotria.inventario.model.Producto;
import es.grupotria.inventario.service.ProductoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
public class StockController {
    private final ProductoService products;
    public StockController(ProductoService products) { this.products = products; }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required=false) String q) {
        return products.list(q, "ACTIVO", null, null).stream().map(this::toStock).toList();
    }

    private Map<String, Object> toStock(Producto p) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("productoId", p.id());
        row.put("nombre", p.nombre());
        row.put("sku", p.sku());
        row.put("categoria", p.categoriaNombre());
        row.put("stockActual", p.stockActual());
        row.put("stockMinimo", p.stockMinimo());
        row.put("nivel", p.stockActual() == 0 ? "SIN_STOCK" : p.stockActual() <= p.stockMinimo() ? "BAJO" : "CORRECTO");
        return row;
    }
}
