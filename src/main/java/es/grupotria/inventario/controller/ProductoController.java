package es.grupotria.inventario.controller;

import es.grupotria.inventario.auth.RoleGuard;
import es.grupotria.inventario.dto.ProductoRequest;
import es.grupotria.inventario.model.Producto;
import es.grupotria.inventario.model.Usuario;
import es.grupotria.inventario.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {
    private final ProductoService service;
    public ProductoController(ProductoService service) { this.service = service; }

    @GetMapping
    public List<Producto> list(@RequestParam(required=false) String q,
                               @RequestParam(required=false) String estado,
                               @RequestParam(required=false) Long categoriaId,
                               @RequestParam(required=false) Long proveedorId) {
        return service.list(q, estado, categoriaId, proveedorId);
    }

    @GetMapping("/{id}")
    public Producto get(@PathVariable long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Producto create(@Valid @RequestBody ProductoRequest request,
                           @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireWrite(user);
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Producto update(@PathVariable long id, @Valid @RequestBody ProductoRequest request,
                           @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireWrite(user);
        return service.update(id, request);
    }

    @PatchMapping("/{id}/estado")
    public Producto estado(@PathVariable long id, @RequestBody Map<String, String> body,
                           @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireWrite(user);
        return service.setEstado(id, body.get("estado"));
    }
}
