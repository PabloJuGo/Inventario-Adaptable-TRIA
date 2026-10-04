# Validación técnica

Comprobaciones hechas sobre la versión 2.0 antes de entregarla:

1. **Backend**: el proyecto compila con Java 17 y Maven.
2. **SQLite**: `schema.sql` se ejecuta varias veces sobre una base vacía sin dar errores (crea las nueve tablas y la configuración inicial). También se probaron inserciones y consultas de productos, movimientos, lotes y alertas.
3. **JavaScript**: los módulos se revisaron con `node --check` y se comprobó que cada import apunta a un archivo que existe.
4. **Contraseñas**: se probó `PasswordHasher` con una contraseña correcta y con una incorrecta.
5. **Pruebas automáticas**: `ApiSmokeTest` comprueba la salud de la API, el login y el dashboard con MockMvc.

## Cómo repetir las comprobaciones

```bash
mvn clean test
mvn clean package
```

En Windows también se puede usar:

```bat
scripts\build.bat
```

Después se inicia con:

```bash
java -jar target/inventario-adaptable-tria-2.0.0.jar
```

Y se abre `http://localhost:8080`.
