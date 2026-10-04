package es.grupotria.inventario.controller;

import es.grupotria.inventario.auth.RoleGuard;
import es.grupotria.inventario.dto.LoteRequest;
import es.grupotria.inventario.model.Lote;
import es.grupotria.inventario.model.Usuario;
import es.grupotria.inventario.service.LoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lotes")
public class LoteController {
    private final LoteService service;
    public LoteController(LoteService service) { this.service = service; }

    @GetMapping
    public List<Lote> list(@RequestParam(required=false) Long productoId,
                           @RequestParam(required=false) String estado,
                           @RequestParam(defaultValue="false") boolean conStock) {
        return service.list(productoId, estado, conStock);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Lote create(@Valid @RequestBody LoteRequest request,
                       @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireWrite(user);
        return service.create(request, user.id());
    }
}
