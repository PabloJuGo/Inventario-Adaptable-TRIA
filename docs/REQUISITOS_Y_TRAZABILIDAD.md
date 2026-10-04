# Requisitos y trazabilidad con los Entregables 1, 2 y 3

Este documento explica cómo la versión web conserva el alcance funcional definido en la documentación previa del Grupo TRIA y cómo se adapta la solución técnica desde Swing a una arquitectura web REST.

## Alcance conservado

Los tres entregables previos definen como núcleo del producto la gestión de productos, stock, movimientos, trazabilidad, lotes/caducidades, alertas, consulta estructurada, documentación y administración. La migración mantiene ese alcance y no añade integraciones ERP, marketplaces, aplicación móvil nativa ni despliegue cloud comercial.

## Matriz de trazabilidad

| Requisito previo | Implementación web v2.0 |
|---|---|
| Alta, modificación, baja y consulta de productos | `ProductoController`, `ProductoService`, módulo `productos.js`. La baja se implementa como desactivación para conservar trazabilidad. |
| Control de stock | `StockController` + actualización transaccional desde `MovimientoService`. |
| Entradas y salidas | `POST /api/movimientos` con tipos `ENTRADA` y `SALIDA`. |
| Ajustes | Tipo `AJUSTE`, ya contemplado en el diseño conceptual del Entregable 3. |
| Trazabilidad | Historial con producto, lote, usuario, fecha, motivo y referencia. |
| Gestión por lotes | CRUD de alta/consulta mediante `LoteService`; cantidades y caducidad. |
| Control de caducidad / vida útil | `AlertService` revisa la fecha de caducidad y, si no existe, calcula el fin de vida útil a partir de la fecha de entrada y los días configurados en el lote. |
| Alertas de stock bajo | Se generan cuando `stock_actual <= stock_minimo`. |
| Incidencias de inventario | El operario/administrador puede registrar incidencias manuales con prioridad y producto relacionado opcional. |
| Búsquedas y filtros | Productos, stock, movimientos, lotes, alertas y documentos. |
| Persistencia | SQLite mediante JDBC/JdbcTemplate. |
| Documentación asociada | Subida real de archivos, asociación a producto o movimiento, metadatos SQLite y almacenamiento en `data/documentos`. |
| Panel principal | Dashboard con indicadores, movimientos recientes y alertas. |
| Usuarios internos | Roles ADMIN, OPERARIO y CONSULTA. |
| Login real | Contraseña hasheada con PBKDF2 y sesión por token. |
| Configuración modular | Activación/desactivación de lotes, alertas y documentos. |
| Interfaz clara para usuarios no técnicos | SPA responsive con navegación lateral, tablas, filtros, modales y mensajes de estado. |
| Modularidad / ampliación futura | Capas Controller → Service → Repository → SQLite; frontend por módulos ES6. |

## Correcciones realizadas al modelo inicial

Durante la migración se han resuelto incoherencias del borrador de base de datos sin alterar el objetivo funcional:

1. `PROVEEDORES_CATEGORIA` se separa en `categorias` y `proveedores`, coherente con las cardinalidades descritas en el propio Entregable 3.
2. `ALERTAS.tipo` pasa a `STOCK_BAJO`, `CADUCIDAD` o `INCIDENCIA`; `ENTRADA/SALIDA` pertenecen a movimientos, no a alertas.
3. Las claves foráneas de alertas apuntan a productos/lotes y no a usuarios.
4. La contraseña deja de ser un `VARCHAR(20)` de texto y se almacena como hash PBKDF2.
5. Se añade `AJUSTE` a los movimientos, porque el diseño conceptual y los requisitos de actualización de stock ya lo contemplaban.
6. Los `ENUM` se representan en SQLite mediante `TEXT` + restricciones `CHECK`.
7. `lote_id` en movimientos puede ser nulo para productos sin seguimiento por lotes; cuando un producto tiene gestión por lotes, cualquier movimiento debe indicar el lote para mantener cuadradas las existencias globales y las existencias por lote.
8. La baja de productos es lógica (`NO_ACTIVO`) para no romper el historial.

## Roles

### ADMIN
- Acceso total.
- Gestión de usuarios y configuración.
- Gestión de catálogos.
- Operaciones de inventario.

### OPERARIO
- Productos, stock, movimientos, lotes, alertas y documentos.
- No puede administrar usuarios ni configuración global.

### CONSULTA
- Acceso de lectura.
- No puede crear, modificar ni eliminar datos.

## Fuera de alcance

Se mantienen fuera del MVP:

- ERP externo real.
- Marketplace o tienda online.
- Multiempresa completo.
- Hardware industrial.
- App móvil nativa.
- Analítica avanzada.
- Infraestructura cloud comercial.
