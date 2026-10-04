package es.grupotria.inventario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:./target/test-inventario.db",
        "app.documents-path=./target/test-documentos"
})
@AutoConfigureMockMvc
class ApiSmokeTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void healthEsPublico() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void dashboardRequiereAutenticacion() throws Exception {
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginYDashboardFuncionan() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@tria.local\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(body);
        String token = json.get("token").asText();
        mvc.perform(get("/api/dashboard").header("X-Auth-Token", token))
                .andExpect(status().isOk());
    }

    @Test
    void alertaDeCaducidadSeCierraAlVaciarElLote() throws Exception {
        String token = mapper.readTree(mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@tria.local\",\"password\":\"Admin123!\"}"))
                .andReturn().getResponse().getContentAsString()).get("token").asText();
        long cat = mapper.readTree(mvc.perform(get("/api/catalogos/categorias").header("X-Auth-Token", token))
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();
        long prov = mapper.readTree(mvc.perform(get("/api/catalogos/proveedores").header("X-Auth-Token", token))
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();
        String sufijo = String.valueOf(System.nanoTime());
        long producto = mapper.readTree(mvc.perform(post("/api/productos").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Test caducidad\",\"sku\":\"SKU-" + sufijo + "\",\"codigoBarras\":\"" + sufijo
                                + "\",\"stockMinimo\":0,\"categoriaId\":" + cat + ",\"proveedorId\":" + prov + "}"))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        String hoy = java.time.LocalDate.now().toString();
        String lote = mapper.readTree(mvc.perform(post("/api/lotes").header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productoId\":" + producto + ",\"numeroLote\":\"L-" + sufijo + "\",\"fechaEntrada\":\"" + hoy
                                + "\",\"fechaCaducidad\":\"" + java.time.LocalDate.now().plusDays(3) + "\",\"cantidad\":5}"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        String abiertas = "/api/alertas?tipo=CADUCIDAD&resuelta=false";
        String antes = mvc.perform(get(abiertas).header("X-Auth-Token", token)).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(antes.contains("L-" + sufijo));

        mvc.perform(post("/api/movimientos").header("X-Auth-Token", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"SALIDA\",\"productoId\":" + producto + ",\"loteId\":" + lote
                                + ",\"cantidad\":5,\"motivo\":\"Prueba\"}"))
                .andExpect(status().isCreated());
        String despues = mvc.perform(get(abiertas).header("X-Auth-Token", token)).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(despues.contains("L-" + sufijo));
    }
}
