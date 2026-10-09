package es.grupotria.inventario.service;

import es.grupotria.inventario.dto.ConfigUpdateRequest;
import es.grupotria.inventario.exception.ApiException;
import es.grupotria.inventario.model.Configuracion;
import es.grupotria.inventario.repository.ConfiguracionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConfiguracionService {
    private final ConfiguracionRepository repo;
    public ConfiguracionService(ConfiguracionRepository repo) { this.repo = repo; }

    public List<Configuracion> list() { return repo.findAll(); }

    public Map<String, Object> publicMap() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Configuracion c : repo.findAll()) {
            Object value = c.valor();
            if ("true".equalsIgnoreCase(c.valor()) || "false".equalsIgnoreCase(c.valor())) value = Boolean.parseBoolean(c.valor());
            else {
                try { value = Integer.parseInt(c.valor()); } catch (NumberFormatException ignored) {}
            }
            out.put(c.clave(), value);
        }
        return out;
    }

    public Configuracion update(String key, ConfigUpdateRequest request) {
        Configuracion c = repo.findByKey(key).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "CONFIG_NO_ENCONTRADA", "La opción de configuración no existe."));
        String valor = request.valor().trim();
        if (c.valor().matches("true|false") && !valor.matches("true|false"))
            throw new ApiException(HttpStatus.BAD_REQUEST, "CONFIG_VALOR_INVALIDO", "Esta opción solo admite true o false.");
        if (c.valor().matches("[0-9]+") && !valor.matches("[0-9]{1,6}"))
            throw new ApiException(HttpStatus.BAD_REQUEST, "CONFIG_VALOR_INVALIDO", "Esta opción requiere un número entero positivo.");
        boolean active = request.activo() == null ? c.activo() : request.activo();
        repo.update(key, valor, active);
        return repo.findByKey(key).orElseThrow();
    }
}
