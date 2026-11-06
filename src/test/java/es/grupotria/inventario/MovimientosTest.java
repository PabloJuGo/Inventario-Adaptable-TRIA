package es.grupotria.inventario;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Pruebas de integracion: se llama a la API igual que lo hace el frontend
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/test-inventario.db",
        "app.documents-path=./target/test-documentos"
})
@AutoConfigureMockMvc
class MovimientosTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    // hace login y devuelve el token
    String login(String email, String password) throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(respuesta).get("token").asText();
    }

    // crea un producto nuevo (sin stock) y devuelve su id
    long crearProducto(String token) throws Exception {
        String cats = mvc.perform(get("/api/catalogos/categorias").header("X-Auth-Token", token))
                .andReturn().getResponse().getContentAsString();
        String provs = mvc.perform(get("/api/catalogos/proveedores").header("X-Auth-Token", token))
                .andReturn().getResponse().getContentAsString();
        long cat = mapper.readTree(cats).get(0).get("id").asLong();
        long prov = mapper.readTree(provs).get(0).get("id").asLong();

        String codigo = String.valueOf(System.nanoTime());
        String json = "{\"nombre\":\"Producto test\",\"sku\":\"SKU-" + codigo + "\",\"codigoBarras\":\"" + codigo
                + "\",\"stockMinimo\":2,\"categoriaId\":" + cat + ",\"proveedorId\":" + prov + "}";
        String respuesta = mvc.perform(post("/api/productos").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(respuesta).get("id").asLong();
    }

    String movimiento(String tipo, long producto, int cantidad) {
        return "{\"tipo\":\"" + tipo + "\",\"productoId\":" + producto + ",\"cantidad\":" + cantidad
                + ",\"motivo\":\"Prueba\"}";
    }

    int stockDe(String token, long producto) throws Exception {
        String respuesta = mvc.perform(get("/api/productos/" + producto).header("X-Auth-Token", token))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(respuesta).get("stockActual").asInt();
    }

    @Test
    void loginConContrasenaIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@tria.local\",\"password\":\"mal\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void entradaYSalidaActualizanElStock() throws Exception {
        String token = login("operario@tria.local", "Operario123!");
        long producto = crearProducto(token);

        mvc.perform(post("/api/movimientos").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(movimiento("ENTRADA", producto, 10)))
                .andExpect(status().isCreated());
        assertEquals(10, stockDe(token, producto));

        mvc.perform(post("/api/movimientos").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(movimiento("SALIDA", producto, 4)))
                .andExpect(status().isCreated());
        assertEquals(6, stockDe(token, producto));
    }

    @Test
    void salidaMayorQueElStockSeRechaza() throws Exception {
        String token = login("operario@tria.local", "Operario123!");
        long producto = crearProducto(token);
        mvc.perform(post("/api/movimientos").header("X-Auth-Token", token)
                .contentType(MediaType.APPLICATION_JSON).content(movimiento("ENTRADA", producto, 5)));

        mvc.perform(post("/api/movimientos").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(movimiento("SALIDA", producto, 6)))
                .andExpect(status().isConflict());
        // el stock no tiene que cambiar
        assertEquals(5, stockDe(token, producto));
    }

    @Test
    void salidaDeTodoElStockSePermite() throws Exception {
        // valor limite: sacar justo lo que hay
        String token = login("operario@tria.local", "Operario123!");
        long producto = crearProducto(token);
        mvc.perform(post("/api/movimientos").header("X-Auth-Token", token)
                .contentType(MediaType.APPLICATION_JSON).content(movimiento("ENTRADA", producto, 5)));

        mvc.perform(post("/api/movimientos").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(movimiento("SALIDA", producto, 5)))
                .andExpect(status().isCreated());
        assertEquals(0, stockDe(token, producto));
    }

    @Test
    void cantidadCeroONegativaSeRechaza() throws Exception {
        String token = login("operario@tria.local", "Operario123!");
        long producto = crearProducto(token);

        mvc.perform(post("/api/movimientos").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(movimiento("ENTRADA", producto, 0)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/movimientos").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(movimiento("ENTRADA", producto, -3)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noSePuedenHacerMovimientosEnUnProductoInactivo() throws Exception {
        String token = login("admin@tria.local", "Admin123!");
        long producto = crearProducto(token);
        mvc.perform(patch("/api/productos/" + producto + "/estado").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"NO_ACTIVO\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/movimientos").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(movimiento("ENTRADA", producto, 3)))
                .andExpect(status().isConflict());
    }

    @Test
    void movimientoDeUnProductoQueNoExisteDevuelve404() throws Exception {
        String token = login("operario@tria.local", "Operario123!");
        mvc.perform(post("/api/movimientos").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(movimiento("ENTRADA", 999999, 3)))
                .andExpect(status().isNotFound());
    }

    @Test
    void siElProductoTieneLotesHayQueIndicarElLote() throws Exception {
        String token = login("operario@tria.local", "Operario123!");
        long producto = crearProducto(token);
        String hoy = java.time.LocalDate.now().toString();
        mvc.perform(post("/api/lotes").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + producto + ",\"numeroLote\":\"L-" + producto
                                + "\",\"fechaEntrada\":\"" + hoy + "\",\"cantidad\":10}"))
                .andExpect(status().isCreated());

        // salida sin loteId
        mvc.perform(post("/api/movimientos").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON).content(movimiento("SALIDA", producto, 2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elUsuarioConsultaNoPuedeRegistrarMovimientos() throws Exception {
        String admin = login("admin@tria.local", "Admin123!");
        long producto = crearProducto(admin);
        String consulta = login("consulta@tria.local", "Consulta123!");

        mvc.perform(post("/api/movimientos").header("X-Auth-Token", consulta)
                        .contentType(MediaType.APPLICATION_JSON).content(movimiento("ENTRADA", producto, 3)))
                .andExpect(status().isForbidden());
    }
}
