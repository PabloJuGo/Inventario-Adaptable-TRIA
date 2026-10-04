package es.grupotria.inventario.controller;

import es.grupotria.inventario.auth.RoleGuard;
import es.grupotria.inventario.dto.ConfigUpdateRequest;
import es.grupotria.inventario.model.Configuracion;
import es.grupotria.inventario.model.Usuario;
import es.grupotria.inventario.service.ConfiguracionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configuracion")
public class ConfiguracionController {
    private final ConfiguracionService service;
    public ConfiguracionController(ConfiguracionService service) { this.service = service; }

    @GetMapping("/publica")
    public Map<String, Object> publicConfig() { return service.publicMap(); }

    @GetMapping
    public List<Configuracion> list(@RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireAdmin(user);
        return service.list();
    }

    @PutMapping("/{clave}")
    public Configuracion update(@PathVariable String clave, @Valid @RequestBody ConfigUpdateRequest request,
                                @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireAdmin(user);
        return service.update(clave, request);
    }
}
