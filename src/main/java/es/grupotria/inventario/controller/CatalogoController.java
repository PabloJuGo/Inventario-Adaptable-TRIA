package es.grupotria.inventario.controller;

import es.grupotria.inventario.auth.RoleGuard;
import es.grupotria.inventario.dto.CategoriaRequest;
import es.grupotria.inventario.dto.ProveedorRequest;
import es.grupotria.inventario.model.Categoria;
import es.grupotria.inventario.model.Proveedor;
import es.grupotria.inventario.model.Usuario;
import es.grupotria.inventario.service.CatalogoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {
    private final CatalogoService service;
    public CatalogoController(CatalogoService service) { this.service = service; }

    @GetMapping("/categorias")
    public List<Categoria> categorias() { return service.categorias(); }

    @GetMapping("/proveedores")
    public List<Proveedor> proveedores() { return service.proveedores(); }

    @PostMapping("/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    public Categoria createCategoria(@Valid @RequestBody CategoriaRequest request,
                                     @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireAdmin(user);
        return service.createCategoria(request);
    }

    @PostMapping("/proveedores")
    @ResponseStatus(HttpStatus.CREATED)
    public Proveedor createProveedor(@Valid @RequestBody ProveedorRequest request,
                                     @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireAdmin(user);
        return service.createProveedor(request);
    }
}
