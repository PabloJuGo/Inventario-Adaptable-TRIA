PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_hash TEXT NOT NULL,
    rol TEXT NOT NULL DEFAULT 'OPERARIO' CHECK (rol IN ('ADMIN','OPERARIO','CONSULTA')),
    activo INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1)),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categorias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE COLLATE NOCASE,
    descripcion TEXT NOT NULL DEFAULT '',
    activo INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1))
);

CREATE TABLE IF NOT EXISTS proveedores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE COLLATE NOCASE,
    contacto TEXT NOT NULL DEFAULT '',
    email TEXT NOT NULL DEFAULT '',
    telefono TEXT NOT NULL DEFAULT '',
    descripcion TEXT NOT NULL DEFAULT '',
    activo INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1))
);

CREATE TABLE IF NOT EXISTS productos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    sku TEXT NOT NULL UNIQUE COLLATE NOCASE,
    codigo_barras TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL DEFAULT '',
    stock_actual INTEGER NOT NULL DEFAULT 0 CHECK (stock_actual >= 0),
    stock_minimo INTEGER NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    estado TEXT NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','NO_ACTIVO')),
    categoria_id INTEGER NOT NULL,
    proveedor_id INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);

CREATE TABLE IF NOT EXISTS lotes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    producto_id INTEGER NOT NULL,
    numero_lote TEXT NOT NULL,
    fecha_entrada TEXT NOT NULL,
    fecha_caducidad TEXT,
    vida_util_dias INTEGER NOT NULL DEFAULT 0 CHECK (vida_util_dias >= 0),
    cantidad INTEGER NOT NULL DEFAULT 0 CHECK (cantidad >= 0),
    estado TEXT NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO','NO_ACTIVO')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (producto_id, numero_lote),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE TABLE IF NOT EXISTS movimientos_inventario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo TEXT NOT NULL CHECK (tipo IN ('ENTRADA','SALIDA','AJUSTE')),
    producto_id INTEGER NOT NULL,
    lote_id INTEGER,
    usuario_id INTEGER NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad <> 0),
    fecha TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo TEXT NOT NULL,
    referencia TEXT NOT NULL DEFAULT '',
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (lote_id) REFERENCES lotes(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS alertas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo TEXT NOT NULL CHECK (tipo IN ('STOCK_BAJO','CADUCIDAD','INCIDENCIA')),
    producto_id INTEGER,
    lote_id INTEGER,
    mensaje TEXT NOT NULL,
    fecha TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leida INTEGER NOT NULL DEFAULT 0 CHECK (leida IN (0,1)),
    resuelta INTEGER NOT NULL DEFAULT 0 CHECK (resuelta IN (0,1)),
    prioridad TEXT NOT NULL DEFAULT 'MEDIA' CHECK (prioridad IN ('ALTA','MEDIA','BAJA')),
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (lote_id) REFERENCES lotes(id)
);

CREATE TABLE IF NOT EXISTS documentos_inventario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    nombre_archivo TEXT NOT NULL,
    ruta_archivo TEXT NOT NULL,
    tipo_archivo TEXT NOT NULL DEFAULT 'application/octet-stream',
    tamano INTEGER NOT NULL DEFAULT 0,
    fecha_subida TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    producto_id INTEGER,
    movimiento_id INTEGER,
    usuario_id INTEGER NOT NULL,
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (movimiento_id) REFERENCES movimientos_inventario(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS configuracion_sistema (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    clave TEXT NOT NULL UNIQUE,
    valor TEXT NOT NULL,
    descripcion TEXT NOT NULL DEFAULT '',
    activo INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1))
);

CREATE INDEX IF NOT EXISTS idx_productos_nombre ON productos(nombre);
CREATE INDEX IF NOT EXISTS idx_productos_estado ON productos(estado);
CREATE INDEX IF NOT EXISTS idx_lotes_producto ON lotes(producto_id);
CREATE INDEX IF NOT EXISTS idx_lotes_caducidad ON lotes(fecha_caducidad);
CREATE INDEX IF NOT EXISTS idx_movimientos_producto ON movimientos_inventario(producto_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_fecha ON movimientos_inventario(fecha);
CREATE INDEX IF NOT EXISTS idx_alertas_resuelta ON alertas(resuelta, leida);
CREATE INDEX IF NOT EXISTS idx_documentos_producto ON documentos_inventario(producto_id);

INSERT OR IGNORE INTO configuracion_sistema(clave, valor, descripcion, activo) VALUES
('modulo_lotes', 'true', 'Activa la gestión por lotes y fechas de caducidad', 1),
('modulo_alertas', 'true', 'Activa el módulo de alertas preventivas', 1),
('modulo_documentos', 'true', 'Activa la gestión documental asociada al inventario', 1),
('dias_alerta_caducidad', '30', 'Días de antelación para avisos de caducidad', 1),
('nombre_empresa', 'Empresa demo TRIA', 'Nombre mostrado en la cabecera de la aplicación', 1);
