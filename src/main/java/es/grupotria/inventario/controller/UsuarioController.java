package es.grupotria.inventario.controller;

import es.grupotria.inventario.auth.RoleGuard;
import es.grupotria.inventario.dto.UsuarioRequest;
import es.grupotria.inventario.model.Usuario;
import es.grupotria.inventario.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService service;
    public UsuarioController(UsuarioService service) { this.service = service; }

    @GetMapping
    public List<Usuario> list(@RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireAdmin(user);
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario create(@Valid @RequestBody UsuarioRequest request, @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireAdmin(user);
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Usuario update(@PathVariable long id, @Valid @RequestBody UsuarioRequest request,
                          @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireAdmin(user);
        return service.update(id, request, user.id());
    }
}
