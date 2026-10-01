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

                // 2. Garantizar programáticamente que los usuarios admin y secretaria existan
                asegurarUsuarioBase(conn, "admin", "admin123", "Administrador Principal", "ADMIN");
                asegurarUsuarioBase(conn, "secretaria", "secretaria123", "Secretaría y Recepción", "SECRETARIA");
            }
        } catch (Exception e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void asegurarUsuarioBase(Connection conn, String user, String pass, String nombre, String rol) {
        String sqlCheck = "SELECT id FROM usuarios WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
            ps.setString(1, user);
            try (ResultSet rs = ps.executeQuery()) {
                String hash = BCrypt.hashpw(pass, BCrypt.gensalt(10));
                if (!rs.next()) {
                    // No existe, insertar
                    String sqlInsert = "INSERT INTO usuarios (username, password_hash, nombre_completo, rol, activo) VALUES (?, ?, ?, ?, 1)";
                    try (PreparedStatement psIns = conn.prepareStatement(sqlInsert)) {
                        psIns.setString(1, user);
                        psIns.setString(2, hash);
                        psIns.setString(3, nombre);
                        psIns.setString(4, rol);
                        psIns.executeUpdate();
                    }
                } else {
                    // Ya existe, actualizar hash para garantizar que coincida exactamente
                    int id = rs.getInt("id");
                    String sqlUpd = "UPDATE usuarios SET password_hash = ?, activo = 1 WHERE id = ?";
                    try (PreparedStatement psUpd = conn.prepareStatement(sqlUpd)) {
                        psUpd.setString(1, hash);
                        psUpd.setInt(2, id);
                        psUpd.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("No se pudo asegurar el usuario base " + user + ": " + e.getMessage());
        }
    }

    public static String getDatabasePath() {
        return new File(DB_FILE).getAbsolutePath();
    }
}
