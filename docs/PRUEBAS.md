# Plan de pruebas

## Automatizadas

Ejecutar:

```bash
mvn test
```

`ApiSmokeTest` comprueba como mínimo:

1. El endpoint de salud es público.
2. El dashboard rechaza peticiones sin autenticación.
3. El login con la cuenta inicial funciona.
4. El token obtenido permite consultar el dashboard.

## Pruebas funcionales manuales recomendadas

### PF-01 Login
- Entrar con `admin@tria.local`.
- Resultado esperado: dashboard visible.
- Probar contraseña incorrecta.
- Resultado esperado: mensaje claro y acceso denegado.

### PF-02 Producto
- Crear producto con SKU y código de barras nuevos.
- Editarlo.
- Desactivarlo y volver a activarlo.
- Resultado: los cambios persisten tras reiniciar la aplicación.

### PF-03 Entrada de stock
- Registrar una entrada de 10 unidades.
- Resultado: el stock aumenta en 10 y aparece un movimiento ENTRADA.

### PF-04 Salida
- Registrar una salida inferior al stock.
- Resultado: disminuye el stock.
- Intentar salida superior al disponible.
- Resultado: HTTP 409 y mensaje de stock insuficiente; el stock no cambia.

### PF-05 Ajuste
- Registrar ajuste positivo y negativo.
- Resultado: stock actualizado sin permitir valor final negativo.

### PF-06 Lotes
- Crear lote con cantidad inicial y caducidad.
- Resultado: el lote aparece y se registra entrada automática.
- Realizar salida sobre un producto con lotes sin elegir lote.
- Resultado: el sistema exige seleccionar el lote.

### PF-07 Alertas
- Dejar un producto en stock igual/inferior a su mínimo.
- Resultado: alerta de stock bajo.
- Crear lote con caducidad dentro de 30 días.
- Resultado: alerta de caducidad.

### PF-08 Documentos
- Subir un PDF o imagen menor de 10 MB asociado a producto.
- Descargarlo.
- Eliminarlo.
- Resultado: metadatos y archivo físico se gestionan correctamente.

### PF-09 Roles
- ADMIN: acceso completo.
- OPERARIO: puede operar inventario, pero no administrar usuarios/configuración.
- CONSULTA: visualiza datos, pero el backend rechaza escrituras.

### PF-10 Persistencia
- Realizar cambios y detener Spring Boot.
- Iniciar de nuevo.
- Resultado: datos conservados en `data/inventario_adaptable.db`.
