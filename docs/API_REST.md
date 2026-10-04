# API REST

Base URL local: `http://localhost:8080/api`

Todas las rutas salvo `/api/auth/login` y `/api/health` requieren la cabecera:

```text
X-Auth-Token: <token>
```

## Autenticación

| Método | Ruta | Uso |
|---|---|---|
| POST | `/auth/login` | Login |
| POST | `/auth/logout` | Cierre de sesión |
| GET | `/auth/me` | Usuario autenticado |

## Dashboard

| Método | Ruta |
|---|---|
| GET | `/dashboard` |

## Productos y stock

| Método | Ruta | Uso |
|---|---|---|
| GET | `/productos` | Listado y filtros |
| GET | `/productos/{id}` | Detalle |
| POST | `/productos` | Alta |
| PUT | `/productos/{id}` | Modificación |
| PATCH | `/productos/{id}/estado` | Activar/desactivar |
| GET | `/stock` | Estado de existencias |

Filtros de productos: `q`, `estado`, `categoriaId`, `proveedorId`.

## Movimientos

| Método | Ruta |
|---|---|
| GET | `/movimientos` |
| POST | `/movimientos` |

Filtros: `tipo`, `productoId`, `desde`, `hasta`, `limit`.

Ejemplo:

```json
{
  "tipo": "SALIDA",
  "productoId": 2,
  "loteId": null,
  "cantidad": 3,
  "motivo": "Venta mostrador",
  "referencia": "V-1042"
}
```

## Lotes

| Método | Ruta |
|---|---|
| GET | `/lotes` |
| POST | `/lotes` |

## Alertas

| Método | Ruta |
|---|---|
| GET | `/alertas` |
| POST | `/alertas` | Registrar incidencia manual |
| PATCH | `/alertas/{id}/leida` |
| PATCH | `/alertas/{id}/resuelta` |

## Documentos

| Método | Ruta |
|---|---|
| GET | `/documentos` |
| POST | `/documentos` | multipart/form-data |
| GET | `/documentos/{id}/download` |
| DELETE | `/documentos/{id}` |

## Usuarios (ADMIN)

| Método | Ruta |
|---|---|
| GET | `/usuarios` |
| POST | `/usuarios` |
| PUT | `/usuarios/{id}` |

## Configuración (ADMIN salvo lectura pública autenticada)

| Método | Ruta |
|---|---|
| GET | `/configuracion/publica` |
| GET | `/configuracion` |
| PUT | `/configuracion/{clave}` |

## Catálogos

| Método | Ruta |
|---|---|
| GET | `/catalogos/categorias` |
| GET | `/catalogos/proveedores` |
| POST | `/catalogos/categorias` (ADMIN) |
| POST | `/catalogos/proveedores` (ADMIN) |

## Códigos HTTP

- `200` consulta o actualización correcta.
- `201` recurso creado.
- `204` operación correcta sin cuerpo.
- `400` datos inválidos.
- `401` sesión no válida.
- `403` permiso insuficiente.
- `404` recurso inexistente.
- `409` conflicto de negocio o integridad.
- `413` archivo demasiado grande.
- `500` error interno.
