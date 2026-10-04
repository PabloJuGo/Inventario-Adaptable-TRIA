# Inventario Adaptable TRIA

![Java 17](https://img.shields.io/badge/Java-17-orange)
![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F)
![SQLite](https://img.shields.io/badge/SQLite-embebida-003B57)
![Frontend](https://img.shields.io/badge/Frontend-HTML%20%7C%20CSS%20%7C%20JS%20Vanilla-yellow)

Aplicación web de **gestión de inventario adaptable** para Grupo TRIA. Es la migración completa del prototipo Java Swing original a una arquitectura web: **Spring Boot + API REST + SQLite + HTML/CSS/JavaScript Vanilla**.

> Versión 2.0 · Sin servidor de base de datos ni dependencias de frontend: se descarga, se ejecuta y funciona.

## Tabla de contenidos

- [Características](#características)
- [Inicio rápido](#inicio-rápido)
- [Cuentas de demostración](#cuentas-de-demostración)
- [Compilar el JAR](#compilar-el-jar)
- [Configuración](#base-de-datos-y-documentos)
- [Estructura](#estructura)
- [Arquitectura](#arquitectura)
- [Documentación](#documentación)

## Características

- Login real con contraseñas almacenadas mediante **PBKDF2-HMAC-SHA256** y sesiones por token.
- Roles `ADMIN`, `OPERARIO` y `CONSULTA`.
- Gestión de productos: alta, modificación, consulta y activación/desactivación.
- Control de stock y actualización automática al registrar movimientos.
- Entradas, salidas y ajustes con trazabilidad de usuario, fecha, motivo y referencia.
- Gestión por lotes, cantidades y fechas de caducidad.
- Alertas automáticas de stock bajo y caducidad/vida útil, más registro manual de incidencias.
- Búsquedas y filtros.
- Gestión documental real: subida, asociación a producto o movimiento, descarga y borrado.
- Gestión de usuarios.
- Panel de configuración para activar/desactivar módulos.
- Categorías y proveedores.
- Dashboard con indicadores, movimientos y alertas.
- API REST JSON.
- SQLite persistente en `data/inventario_adaptable.db`.
- Frontend responsive sin frameworks ni dependencias externas.
- Pruebas de humo con Spring Boot + MockMvc.

## Inicio rápido

### Requisitos

- Java JDK 17 o superior.
- Apache Maven 3.9 o superior.
- Navegador moderno (Chrome, Edge, Firefox o Safari recientes).

No es necesario instalar SQLite ni un servidor de base de datos. El driver JDBC crea y utiliza el archivo local automáticamente.

### Abrir en Visual Studio Code

Abre directamente la carpeta `InventarioAdaptableTRIA_Web`. El proyecto incluye recomendaciones de extensiones Java/Spring/Maven y tareas de VS Code en `.vscode/`.

### Ejecutar en desarrollo

**Windows**

```bat
scripts\run.bat
```

**Terminal**

```bash
mvn spring-boot:run
```

Después abre:

```text
http://localhost:8080
```

El frontend se sirve desde `frontend/` y consume la API en `/api`.

## Cuentas de demostración

| Rol | Email | Contraseña |
|---|---|---|
| Administrador | `admin@tria.local` | `Admin123!` |
| Operario | `operario@tria.local` | `Operario123!` |
| Consulta | `consulta@tria.local` | `Consulta123!` |

Estas cuentas se crean **solo si la base de datos no contiene ningún usuario**. Para una instalación real deben cambiarse las contraseñas.

## Compilar el JAR

```bash
mvn clean package
```

El resultado será:

```text
target/inventario-adaptable-tria-2.0.0.jar
```

Para ejecutarlo desde la raíz del proyecto:

```bash
java -jar target/inventario-adaptable-tria-2.0.0.jar
```

El proceso de build copia el frontend dentro del JAR. La aplicación también conserva `frontend/` como código fuente desacoplado y editable.

## Base de datos y documentos

Por defecto:

```text
data/inventario_adaptable.db
data/documentos/
```

Se pueden cambiar con variables de entorno:

```text
APP_DB_PATH
APP_DOCUMENTS_PATH
APP_TOKEN_TTL_HOURS
PORT
```

## Estructura

```text
InventarioAdaptableTRIA_Web/
├── frontend/                  # HTML/CSS/JS Vanilla
│   ├── index.html
│   ├── css/
│   └── js/
│       └── modules/
├── src/main/java/            # Backend Spring Boot
│   └── es/grupotria/inventario/
│       ├── auth/
│       ├── config/
│       ├── controller/
│       ├── dto/
│       ├── exception/
│       ├── model/
│       ├── repository/
│       └── service/
├── src/main/resources/
│   ├── application.properties
│   └── schema.sql
├── src/test/java/            # Pruebas
├── data/                     # SQLite y documentos
├── docs/                     # Documentación técnica
├── scripts/                  # Ejecución y build
└── pom.xml
```

## Arquitectura

```text
Navegador
   │
   │ HTML / CSS / JavaScript
   │ fetch + JSON
   ▼
REST Controllers
   ▼
Services (reglas de negocio)
   ▼
Repositories (JdbcTemplate)
   ▼
SQLite
```

La interfaz no contiene SQL ni reglas de negocio. Los controladores no realizan persistencia directa. Las reglas de stock, lotes, alertas y permisos se resuelven en servicios del backend.

## Documentación

Consulta:

- `docs/REQUISITOS_Y_TRAZABILIDAD.md`
- `docs/ARQUITECTURA.md`
- `docs/API_REST.md`
- `docs/PRUEBAS.md`
- `docs/MIGRACION_DESDE_SWING.md`

## Reiniciar los datos de demostración

Detén la aplicación y elimina:

```text
data/inventario_adaptable.db
```

Al volver a iniciar se creará una base nueva, el esquema se inicializará y se cargarán los datos de demostración.
