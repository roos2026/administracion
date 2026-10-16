package com.academia.config;

import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;

/**
 * Gestor centralizado de la conexión a la base de datos SQLite.
 * Se encarga de inicializar el archivo de base de datos y correr el schema.sql
 * si es la primera vez que se ejecuta la aplicación, asegurando usuarios base.
 */
public class DatabaseManager {

    private static final String DB_DIR = "datos";
    private static final String DB_FILE = DB_DIR + File.separator + "academia.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    static {
        initDatabase();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static synchronized void initDatabase() {
        try {
            File dir = new File(DB_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File docsDir = new File(DB_DIR + File.separator + "documentos");
            if (!docsDir.exists()) docsDir.mkdirs();

            File firmasDir = new File(DB_DIR + File.separator + "firmas");
            if (!firmasDir.exists()) firmasDir.mkdirs();

            File recibosDir = new File(DB_DIR + File.separator + "recibos");
            if (!recibosDir.exists()) recibosDir.mkdirs();

            // 1. Ejecutar el script schema.sql sentencia a sentencia
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                InputStream is = DatabaseManager.class.getResourceAsStream("/db/schema.sql");
                if (is != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        StringBuilder sql = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String trimmed = line.trim();
                            if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                                continue;
                            }
                            sql.append(line).append("\n");
                            if (trimmed.endsWith(";")) {
                                try {
                                    stmt.execute(sql.toString());
                                } catch (SQLException ex) {
                                    // Ignorar si la tabla o índice ya existía
                                }
                                sql.setLength(0);
                            }
                        }
                    }
                }

                // Migraciones seguras para bases de datos existentes
                try { stmt.execute("ALTER TABLE participantes ADD COLUMN dia_corte_habitual INTEGER DEFAULT 30;"); } catch (SQLException ignored) {}
                try { stmt.execute("ALTER TABLE participantes ADD COLUMN fecha_inscripcion DATE;"); } catch (SQLException ignored) {}

                // 2. Sembrar usuario inicial SOLO si la tabla está completamente vacía (instalación limpia)
                asegurarUsuarioInicialSiTablaVacia(conn);
            }
        } catch (Exception e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void asegurarUsuarioInicialSiTablaVacia(Connection conn) {
        String sqlCheck = "SELECT COUNT(*) FROM usuarios";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sqlCheck)) {
            if (rs.next() && rs.getInt(1) == 0) {
                // Solo si no existe ningún usuario, crear el admin inicial para el primer acceso
                String hash = BCrypt.hashpw("admin123", BCrypt.gensalt(10));
                String sqlInsert = "INSERT INTO usuarios (username, password_hash, nombre_completo, rol, activo) VALUES ('admin', ?, 'Administrador Principal', 'ADMIN', 1)";
                try (PreparedStatement psIns = conn.prepareStatement(sqlInsert)) {
                    psIns.setString(1, hash);
                    psIns.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar usuarios iniciales: " + e.getMessage());
        }
    }

    public static String getDatabasePath() {
        return new File(DB_FILE).getAbsolutePath();
    }
}
