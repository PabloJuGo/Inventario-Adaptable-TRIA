# Migración desde la versión Java Swing

## Punto de partida

El código legado contenía una interfaz Swing avanzada con ventanas de login, dashboard y paneles de productos, stock, movimientos, lotes, alertas, documentos, usuarios y configuración. Sin embargo, gran parte de los datos eran de demostración y los formularios todavía no persistían las operaciones en SQLite.

La documentación previa ya proponía una separación por capas y una base SQLite mediante JDBC. La migración toma esa intención y la materializa.

## Equivalencias

| Swing | Web v2.0 |
|---|---|
| `VentanaLogin` | `auth.js` + `POST /api/auth/login` |
| `VentanaPrincipal` | shell web + sidebar + router hash |
| `PanelInicio` | `dashboard.js` + `/api/dashboard` |
| `PanelProductos` | `productos.js` + `ProductoController` |
| `PanelStock` | `stock.js` + `StockController` |
| `PanelMovimientos` | `movimientos.js` + `MovimientoController` |
| `PanelLotes` | `lotes.js` + `LoteController` |
| `PanelAlertas` | `alertas.js` + `AlertaController` |
| `PanelDocumentos` | `documentos.js` + `DocumentoController` |
| `PanelUsuarios` | `usuarios.js` + `UsuarioController` |
| `PanelConfiguracion` | `configuracion.js` + `ConfiguracionController` |
| `DatabaseManager` | DataSource Spring + JdbcTemplate + repositories |

## Mejora principal

Antes:

```text
Swing -> datos simulados / futura persistencia
```

Ahora:

```text
HTML/CSS/JS -> REST -> Service -> Repository -> SQLite
```

La UI queda desacoplada de la persistencia y las reglas de negocio. Esto permite cambiar o ampliar el frontend sin reescribir la lógica de inventario.
