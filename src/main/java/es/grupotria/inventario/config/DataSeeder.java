package es.grupotria.inventario.config;

import es.grupotria.inventario.auth.PasswordHasher;
import es.grupotria.inventario.dto.LoteRequest;
import es.grupotria.inventario.dto.MovimientoRequest;
import es.grupotria.inventario.dto.ProductoRequest;
import es.grupotria.inventario.model.Categoria;
import es.grupotria.inventario.model.Producto;
import es.grupotria.inventario.model.Proveedor;
import es.grupotria.inventario.model.Usuario;
import es.grupotria.inventario.repository.CategoriaRepository;
import es.grupotria.inventario.repository.ProductoRepository;
import es.grupotria.inventario.repository.ProveedorRepository;
import es.grupotria.inventario.repository.UsuarioRepository;
import es.grupotria.inventario.service.LoteService;
import es.grupotria.inventario.service.MovimientoService;
import es.grupotria.inventario.service.ProductoService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataSeeder implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final ProveedorRepository proveedores;
    private final ProductoRepository productos;
    private final PasswordHasher hasher;
    private final ProductoService productoService;
    private final MovimientoService movimientoService;
    private final LoteService loteService;

    public DataSeeder(UsuarioRepository usuarios, CategoriaRepository categorias, ProveedorRepository proveedores,
                      ProductoRepository productos, PasswordHasher hasher, ProductoService productoService,
                      MovimientoService movimientoService, LoteService loteService) {
        this.usuarios = usuarios; this.categorias = categorias; this.proveedores = proveedores;
        this.productos = productos; this.hasher = hasher; this.productoService = productoService;
        this.movimientoService = movimientoService; this.loteService = loteService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.count() == 0) {
            usuarios.insert("Administrador TRIA", "admin@tria.local", hasher.hash("Admin123!"), "ADMIN", true);
            usuarios.insert("Operario Demo", "operario@tria.local", hasher.hash("Operario123!"), "OPERARIO", true);
            usuarios.insert("Consulta Demo", "consulta@tria.local", hasher.hash("Consulta123!"), "CONSULTA", true);
        }
        if (categorias.count() == 0) {
            categorias.insert("General", "Productos de inventario general");
            categorias.insert("Electrónica", "Equipos y accesorios electrónicos");
            categorias.insert("Consumibles", "Material fungible y consumibles");
        }
        if (proveedores.count() == 0) {
            proveedores.insert("Proveedor Demo", "Departamento comercial", "ventas@proveedor.local", "910000001", "Proveedor inicial para pruebas");
            proveedores.insert("Suministros Centro", "Laura Martín", "pedidos@suministros.local", "910000002", "Proveedor de material general");
        }
        if (productos.count() == 0) seedDemo();
    }

    private void seedDemo() {
        Usuario admin = usuarios.findByEmail("admin@tria.local").orElseThrow();
        Categoria general = categorias.findAll(true).stream().filter(c -> c.nombre().equals("General")).findFirst().orElseThrow();
        Categoria electronics = categorias.findAll(true).stream().filter(c -> c.nombre().equals("Electrónica")).findFirst().orElse(general);
        Categoria consumables = categorias.findAll(true).stream().filter(c -> c.nombre().equals("Consumibles")).findFirst().orElse(general);
        Proveedor p1 = proveedores.findAll(true).get(0);
        Proveedor p2 = proveedores.findAll(true).size() > 1 ? proveedores.findAll(true).get(1) : p1;

        Producto cable = productoService.create(new ProductoRequest("Cable HDMI 2 m", "HDMI-2M", "8430000000012",
                "Cable HDMI de alta velocidad", 10, "ACTIVO", electronics.id(), p1.id()));
        movimientoService.create(new MovimientoRequest("ENTRADA", cable.id(), null, 28, "Stock inicial de demostración", "INI-001"), admin.id());

        Producto tornillo = productoService.create(new ProductoRequest("Tornillo M8", "TOR-M8-001", "8430000000029",
                "Tornillo métrico M8", 20, "ACTIVO", general.id(), p2.id()));
        movimientoService.create(new MovimientoRequest("ENTRADA", tornillo.id(), null, 85, "Stock inicial de demostración", "INI-002"), admin.id());

        Producto guantes = productoService.create(new ProductoRequest("Guantes de protección", "EPI-GUA-01", "8430000000036",
                "Caja de guantes de protección", 12, "ACTIVO", consumables.id(), p2.id()));
        loteService.create(new LoteRequest(guantes.id(), "GUA-2026-09", LocalDate.now().minusDays(20).toString(),
                LocalDate.now().plusDays(18).toString(), 0, 9, "ACTIVO"), admin.id());
    }
}
