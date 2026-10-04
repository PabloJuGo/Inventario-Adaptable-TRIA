# Arquitectura técnica

## Decisión de migración

El prototipo original utilizaba Java 17 + Swing y planteaba una arquitectura por capas que todavía no estaba materializada en el código. La versión 2.0 conserva Java y SQLite, pero sustituye la presentación Swing por una interfaz web y formaliza la separación mediante una API REST.

```text
┌──────────────────────────────────────────────┐
│ FRONTEND                                     │
│ HTML5 · CSS3 · JavaScript ES6+               │
│ módulos, fetch(), validación y UI responsive │
└──────────────────────┬───────────────────────┘
                       │ HTTP / JSON
                       ▼
┌──────────────────────────────────────────────┐
│ CONTROLLERS REST                             │
│ rutas, HTTP, validación de entrada, permisos │
└──────────────────────┬───────────────────────┘
                       ▼
┌──────────────────────────────────────────────┐
│ SERVICES                                     │
│ reglas de negocio y transacciones            │
└──────────────────────┬───────────────────────┘
                       ▼
┌──────────────────────────────────────────────┐
│ REPOSITORIES                                 │
│ Spring JdbcTemplate                          │
└──────────────────────┬───────────────────────┘
                       ▼
┌──────────────────────────────────────────────┐
│ SQLite + carpeta de documentos               │
└──────────────────────────────────────────────┘
```

## Backend

- Java 17.
- Spring Boot 3.3.5.
- Spring Web.
- Spring JDBC / `JdbcTemplate`.
- Bean Validation.
- Xerial SQLite JDBC.
- API JSON.

No se utiliza JPA porque el proyecto necesita una capa de persistencia pequeña y transparente y SQLite encaja mejor con SQL explícito para este MVP.

## Frontend

- `index.html` como shell.
- CSS propio responsive.
- JavaScript ES6 por módulos.
- Sin React, Angular, Bootstrap o librerías externas.
- Navegación mediante hash para evitar configuración adicional del servidor.
- Consumo de REST con `fetch()`.

## Seguridad del MVP

- Contraseñas: PBKDF2-HMAC-SHA256, salt aleatoria y 120.000 iteraciones.
- Login: credenciales contra SQLite.
- Sesión: token aleatorio de 12 horas almacenado en memoria en el backend y `sessionStorage` en el navegador.
- Permisos: comprobación de rol también en backend; ocultar un botón en frontend no sustituye la autorización.
- Usuarios `CONSULTA`: solo lectura.
- Documentos: el nombre físico se genera con UUID para evitar colisiones y traversal de ruta.

Para un despliegue comercial distribuido se sustituiría el sistema de sesión por un mecanismo persistente/estándar y se desplegaría siempre bajo HTTPS.

## Regla central de stock

La única vía normal para modificar stock es un movimiento o el alta inicial de un lote:

```text
ENTRADA -> stock + cantidad
SALIDA  -> stock - cantidad (no permite negativo)
AJUSTE  -> stock + delta (admite negativo, nunca deja stock < 0)
```

Si el movimiento apunta a un lote, se actualiza también su cantidad en la misma transacción.

## Alertas

Después de operaciones que afectan al inventario se recalculan alertas:

- `STOCK_BAJO` si el stock actual es menor o igual al mínimo.
- `CADUCIDAD` si un lote está caducado o dentro del umbral configurado.

## Frontend empaquetado

Durante desarrollo, Spring puede servir directamente `frontend/`. En `mvn package`, `maven-resources-plugin` copia el frontend a `target/classes/static`, por lo que el JAR final contiene también la interfaz.
