-- =============================================================================
-- ESQUEMA DE BASE DE DATOS: SISTEMA DE ADMINISTRACIÓN PARA ACADEMIA
-- Motor: SQLite (compatible con Java 21 / sqlite-jdbc)
-- =============================================================================

PRAGMA foreign_keys = ON;

-- 1. Tabla de Usuarios y Roles (Seguridad)
CREATE TABLE IF NOT EXISTS usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    nombre_completo TEXT NOT NULL,
    rol TEXT NOT NULL CHECK(rol IN ('ADMIN', 'SECRETARIA', 'PROFESOR')),
    activo INTEGER DEFAULT 1,
    creado_en DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. Profesores
CREATE TABLE IF NOT EXISTS profesores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre_completo TEXT NOT NULL,
    cedula TEXT,
    telefono TEXT,
    email TEXT,
    especialidad TEXT,
    pago_por_hora REAL DEFAULT 0.0,
    sueldo_fijo REAL DEFAULT 0.0,
    activo INTEGER DEFAULT 1,
    creado_en DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 3. Grupos, Horarios y Cupos
CREATE TABLE IF NOT EXISTS grupos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT UNIQUE NOT NULL,
    nombre TEXT NOT NULL,
    dias TEXT NOT NULL,             -- Ej: 'Lunes y Miércoles'
    horario TEXT NOT NULL,          -- Ej: '15:00 - 17:00'
    cupo_maximo INTEGER NOT NULL DEFAULT 20,
    costo_mensualidad REAL NOT NULL DEFAULT 0.0,
    costo_inscripcion REAL NOT NULL DEFAULT 0.0,
    profesor_id INTEGER,
    activo INTEGER DEFAULT 1,
    creado_en DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (profesor_id) REFERENCES profesores(id) ON DELETE SET NULL
);

-- 4. Participantes (Estudiantes)
CREATE TABLE IF NOT EXISTS participantes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    matricula TEXT UNIQUE NOT NULL,
    nombres TEXT NOT NULL,
    apellidos TEXT NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    foto_ruta TEXT,
    contacto_emergencia_nombre TEXT,
    contacto_emergencia_telefono TEXT,
    alergias_condiciones TEXT,
    estado TEXT NOT NULL DEFAULT 'ACTIVO' CHECK(estado IN ('ACTIVO', 'PAUSA', 'INACTIVO')),
    motivo_baja TEXT,
    fecha_baja DATE,
    grupo_id INTEGER,
    creado_en DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (grupo_id) REFERENCES grupos(id) ON DELETE SET NULL
);

-- 5. Tutores (Padre, Madre o Tutor Legal)
CREATE TABLE IF NOT EXISTS tutores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    participante_id INTEGER NOT NULL,
    nombre_completo TEXT NOT NULL,
    cedula TEXT NOT NULL,
    telefono_whatsapp TEXT NOT NULL,
    email TEXT,
    direccion TEXT,
    parentesco TEXT NOT NULL,      -- 'PADRE', 'MADRE', 'TUTOR_LEGAL', 'OTRO'
    es_principal INTEGER DEFAULT 1,
    FOREIGN KEY (participante_id) REFERENCES participantes(id) ON DELETE CASCADE
);

-- 6. Documentos Adjuntos de Participantes
CREATE TABLE IF NOT EXISTS documentos_participante (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    participante_id INTEGER NOT NULL,
    tipo_documento TEXT NOT NULL,   -- 'ACTA_NACIMIENTO', 'CEDULA_TUTOR', 'CERTIFICADO_MEDICO', 'OTRO'
    nombre_archivo TEXT NOT NULL,
    ruta_archivo TEXT NOT NULL,
    subido_en DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (participante_id) REFERENCES participantes(id) ON DELETE CASCADE
);

-- 7. Contratos Digitales con Firma
CREATE TABLE IF NOT EXISTS contratos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    participante_id INTEGER NOT NULL,
    version_terminos TEXT NOT NULL,
    fecha_firma DATETIME NOT NULL,
    firma_digital_ruta TEXT NOT NULL,
    pdf_contrato_ruta TEXT,
    firmado_por_nombre TEXT NOT NULL,
    firmado_por_cedula TEXT NOT NULL,
    FOREIGN KEY (participante_id) REFERENCES participantes(id) ON DELETE CASCADE
);

-- 8. Catálogo de Conceptos de Cobro (Inscripción, Mensualidad, Uniformes, Eventos, etc.)
CREATE TABLE IF NOT EXISTS conceptos_cobro (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT UNIQUE NOT NULL,
    nombre TEXT NOT NULL,
    monto_sugerido REAL DEFAULT 0.0,
    activo INTEGER DEFAULT 1
);

-- 9. Mensualidades y Cobros Programados
CREATE TABLE IF NOT EXISTS mensualidades (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    participante_id INTEGER NOT NULL,
    periodo TEXT NOT NULL,              -- Formato: 'YYYY-MM'
    fecha_emision DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    monto_base REAL NOT NULL,
    descuento REAL DEFAULT 0.0,
    recargo_mora REAL DEFAULT 0.0,
    monto_total REAL NOT NULL,
    saldo_pendiente REAL NOT NULL,
    estado_pago TEXT NOT NULL DEFAULT 'VENCE_PRONTO' CHECK(estado_pago IN ('PAGADO', 'VENCE_PRONTO', 'VENCIDO')),
    creado_en DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(participante_id, periodo),
    FOREIGN KEY (participante_id) REFERENCES participantes(id) ON DELETE CASCADE
);

-- 10. Pagos y Abonos Parciales (Consecutivos y Anulación)
CREATE TABLE IF NOT EXISTS pagos_abonos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero_recibo TEXT UNIQUE NOT NULL, -- Ej: 'REC-2026-000001'
    participante_id INTEGER NOT NULL,
    mensualidad_id INTEGER,             -- Puede ser nulo si es pago de otro concepto (ej. uniforme)
    concepto_id INTEGER,
    concepto_detalle TEXT NOT NULL,
    monto_abonado REAL NOT NULL,
    metodo_pago TEXT NOT NULL CHECK(metodo_pago IN ('EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'OTRO')),
    referencia_pago TEXT,               -- Nro de transferencia / tarjeta
    fecha_pago DATETIME DEFAULT CURRENT_TIMESTAMP,
    usuario_id INTEGER NOT NULL,
    estado TEXT NOT NULL DEFAULT 'ACTIVO' CHECK(estado IN ('ACTIVO', 'ANULADO')),
    motivo_anulacion TEXT,
    fecha_anulacion DATETIME,
    usuario_anulacion_id INTEGER,
    FOREIGN KEY (participante_id) REFERENCES participantes(id),
    FOREIGN KEY (mensualidad_id) REFERENCES mensualidades(id),
    FOREIGN KEY (concepto_id) REFERENCES conceptos_cobro(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    FOREIGN KEY (usuario_anulacion_id) REFERENCES usuarios(id)
);

-- 11. Categorías de Gastos
CREATE TABLE IF NOT EXISTS categorias_gasto (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT UNIQUE NOT NULL,
    descripcion TEXT
);

-- 12. Gastos y Compras
CREATE TABLE IF NOT EXISTS gastos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    categoria_id INTEGER NOT NULL,
    concepto TEXT NOT NULL,
    proveedor TEXT,
    monto REAL NOT NULL,
    metodo_pago TEXT NOT NULL CHECK(metodo_pago IN ('EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'OTRO')),
    fecha_gasto DATE NOT NULL,
    comprobante_ruta TEXT,
    es_recurrente INTEGER DEFAULT 0,
    profesor_id INTEGER,                -- Si es nómina a profesor
    usuario_id INTEGER NOT NULL,
    creado_en DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (categoria_id) REFERENCES categorias_gasto(id),
    FOREIGN KEY (profesor_id) REFERENCES profesores(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- 13. Cierre de Caja y Arqueo Diario
CREATE TABLE IF NOT EXISTS cajas_arqueo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER NOT NULL,
    fecha_apertura DATETIME NOT NULL,
    fecha_cierre DATETIME,
    monto_inicial_efectivo REAL NOT NULL DEFAULT 0.0,
    total_ingresos_efectivo REAL DEFAULT 0.0,
    total_egresos_efectivo REAL DEFAULT 0.0,
    saldo_esperado_efectivo REAL DEFAULT 0.0,
    saldo_real_contado REAL,
    diferencia REAL,
    notas TEXT,
    estado TEXT NOT NULL DEFAULT 'ABIERTA' CHECK(estado IN ('ABIERTA', 'CERRADA')),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- 14. Registro de Asistencias
CREATE TABLE IF NOT EXISTS asistencias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    participante_id INTEGER NOT NULL,
    grupo_id INTEGER NOT NULL,
    fecha DATE NOT NULL,
    estado TEXT NOT NULL CHECK(estado IN ('PRESENTE', 'AUSENTE', 'JUSTIFICADA', 'TARDE')),
    observaciones TEXT,
    registrado_por_usuario_id INTEGER,
    creado_en DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(participante_id, grupo_id, fecha),
    FOREIGN KEY (participante_id) REFERENCES participantes(id) ON DELETE CASCADE,
    FOREIGN KEY (grupo_id) REFERENCES grupos(id) ON DELETE CASCADE,
    FOREIGN KEY (registrado_por_usuario_id) REFERENCES usuarios(id)
);

-- 15. Registro de Auditoría (Trazabilidad y Seguridad)
CREATE TABLE IF NOT EXISTS auditoria_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id INTEGER,
    usuario_nombre TEXT NOT NULL,
    accion TEXT NOT NULL,               -- 'LOGIN', 'CREAR_ALUMNO', 'REGISTRAR_PAGO', 'ANULAR_PAGO', 'REGISTRAR_GASTO', etc.
    tabla_afectada TEXT,
    detalle TEXT NOT NULL,
    fecha_hora DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- DATOS INICIALES / SEMILLAS POR DEFECTO
-- =============================================================================

-- Usuario Administrador inicial (clave por defecto: admin123, hash jBCrypt)
INSERT OR IGNORE INTO usuarios (id, username, password_hash, nombre_completo, rol, activo)
VALUES (1, 'admin', '$2a$10$wNcrYJz5kPq/WfQ6eA4pIukm8j2Fh4hUoQz40w7C4N9RjTq6cQeKi', 'Administrador Principal', 'ADMIN', 1);

-- Usuario Secretaria por defecto (clave: secretaria123)
INSERT OR IGNORE INTO usuarios (id, username, password_hash, nombre_completo, rol, activo)
VALUES (2, 'secretaria', '$2a$10$7v.9T3r/YjD8zI4kM7P7r.Z6v1Y2q8kM8L0y4a1b2c3d4e5f6g7h8', 'Secretaría y Recepción', 'SECRETARIA', 1);

-- Categorías de gastos predeterminadas
INSERT OR IGNORE INTO categorias_gasto (id, nombre, descripcion) VALUES
(1, 'Alquiler del Local', 'Pago de renta mensual del establecimiento'),
(2, 'Servicios Básicos (Luz/Agua/Gas)', 'Facturas de electricidad, acueducto y servicios'),
(3, 'Internet y Telefonía', 'Conectividad y comunicaciones del centro'),
(4, 'Nómina Profesores', 'Honorarios y salarios de docentes'),
(5, 'Nómina Administrativa', 'Sueldos de secretaria, recepción y limpieza'),
(6, 'Materiales y Didácticos', 'Instrumentos, libros, papelería y consumibles'),
(7, 'Mantenimiento y Reparaciones', 'Reparación de equipos e infraestructura'),
(8, 'Otros Gastos', 'Gastos menores e imprevistos');

-- Conceptos de Cobro iniciales
INSERT OR IGNORE INTO conceptos_cobro (id, codigo, nombre, monto_sugerido) VALUES
(1, 'INSCRIP', 'Inscripción / Matrícula Anual', 1500.0),
(2, 'MENSUAL', 'Mensualidad Regular', 2500.0),
(3, 'UNIFORME', 'Uniforme Oficial / Remera', 800.0),
(4, 'MATERIAL', 'Pack de Materiales y Guías', 500.0),
(5, 'RECITAL', 'Participación en Recital / Evento', 1000.0);
