package es.grupotria.inventario.controller;

import es.grupotria.inventario.auth.AuthService;
import es.grupotria.inventario.dto.LoginRequest;
import es.grupotria.inventario.dto.LoginResponse;
import es.grupotria.inventario.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        auth.logout(request.getHeader("X-Auth-Token"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public Usuario me(@RequestAttribute("authUser") Usuario user) { return user; }
}
