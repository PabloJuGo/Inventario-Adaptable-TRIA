package es.grupotria.inventario;

import es.grupotria.inventario.auth.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Pruebas unitarias del hash de contraseñas (no necesitan arrancar Spring)
class PasswordHasherTest {

    PasswordHasher hasher = new PasswordHasher();

    @Test
    void laContrasenaCorrectaCoincide() {
        String hash = hasher.hash("Prueba123!");
        assertTrue(hasher.matches("Prueba123!", hash));
    }

    @Test
    void laContrasenaIncorrectaNoCoincide() {
        String hash = hasher.hash("Prueba123!");
        assertFalse(hasher.matches("otraCosa", hash));
    }

    @Test
    void noSeGuardaLaContrasenaEnTextoPlano() {
        String hash = hasher.hash("Prueba123!");
        assertFalse(hash.contains("Prueba123!"));
        assertTrue(hash.startsWith("pbkdf2_sha256$"));
    }

    @Test
    void dosHashesDeLaMismaContrasenaSonDistintos() {
        // cada hash lleva su propia sal aleatoria
        assertNotEquals(hasher.hash("Prueba123!"), hasher.hash("Prueba123!"));
    }

    @Test
    void unHashMalFormadoDevuelveFalse() {
        assertFalse(hasher.matches("Prueba123!", "esto-no-es-un-hash"));
    }
}
