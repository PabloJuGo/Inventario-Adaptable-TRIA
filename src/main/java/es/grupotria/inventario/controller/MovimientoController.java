package es.grupotria.inventario.controller;

import es.grupotria.inventario.auth.RoleGuard;
import es.grupotria.inventario.dto.MovimientoRequest;
import es.grupotria.inventario.model.Movimiento;
import es.grupotria.inventario.model.Usuario;
import es.grupotria.inventario.service.MovimientoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoController {
    private final MovimientoService service;
    public MovimientoController(MovimientoService service) { this.service = service; }

    @GetMapping
    public List<Movimiento> list(@RequestParam(required=false) String tipo,
                                 @RequestParam(required=false) Long productoId,
                                 @RequestParam(required=false) String desde,
                                 @RequestParam(required=false) String hasta,
                                 @RequestParam(required=false) Integer limit) {
        return service.list(tipo, productoId, desde, hasta, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Movimiento create(@Valid @RequestBody MovimientoRequest request,
                             @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireWrite(user);
        return service.create(request, user.id());
    }
}
