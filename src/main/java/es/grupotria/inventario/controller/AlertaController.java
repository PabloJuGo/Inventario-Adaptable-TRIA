package es.grupotria.inventario.controller;

import es.grupotria.inventario.auth.RoleGuard;
import es.grupotria.inventario.dto.IncidenciaRequest;
import es.grupotria.inventario.model.Alerta;
import es.grupotria.inventario.model.Usuario;
import es.grupotria.inventario.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {
    private final AlertService service;
    public AlertaController(AlertService service) { this.service = service; }

    @GetMapping
    public List<Alerta> list(@RequestParam(required=false) Boolean resuelta,
                             @RequestParam(required=false) Boolean leida,
                             @RequestParam(required=false) String tipo) {
        return service.list(resuelta, leida, tipo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Alerta createIncident(@Valid @RequestBody IncidenciaRequest request,
                                 @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireWrite(user);
        return service.createIncident(request);
    }

    @PatchMapping("/{id}/leida")
    public Alerta markRead(@PathVariable long id, @RequestBody Map<String, Boolean> body,
                           @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireWrite(user);
        return service.markRead(id, body.getOrDefault("leida", true));
    }

    @PatchMapping("/{id}/resuelta")
    public Alerta resolve(@PathVariable long id, @RequestBody Map<String, Boolean> body,
                          @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireWrite(user);
        return service.resolve(id, body.getOrDefault("resuelta", true));
    }
}
