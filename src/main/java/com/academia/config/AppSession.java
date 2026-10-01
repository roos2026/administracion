package com.academia.config;

import com.academia.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Mantiene la sesión del usuario actualmente autenticado en la aplicación
 * y provee el servicio de registro de auditoría.
 */
public class AppSession {

    private static Usuario usuarioActual;

    public static void setUsuarioActual(Usuario usuario) {
        usuarioActual = usuario;
        if (usuario != null) {
            registrarAuditoria("LOGIN", "usuarios", "Inicio de sesión exitoso de " + usuario.getUsername());
        }
    }

    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public static boolean isAutenticado() {
        return usuarioActual != null;
    }

    public static boolean isAdmin() {
        return usuarioActual != null && "ADMIN".equalsIgnoreCase(usuarioActual.getRol());
    }

    public static boolean isSecretaria() {
        return usuarioActual != null && "SECRETARIA".equalsIgnoreCase(usuarioActual.getRol());
    }

    public static boolean isProfesor() {
        return usuarioActual != null && "PROFESOR".equalsIgnoreCase(usuarioActual.getRol());
    }

    public static void registrarAuditoria(String accion, String tabla, String detalle) {
        String query = "INSERT INTO auditoria_logs (usuario_id, usuario_nombre, accion, tabla_afectada, detalle) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setObject(1, usuarioActual != null ? usuarioActual.getId() : null);
            ps.setString(2, usuarioActual != null ? usuarioActual.getNombreCompleto() : "SISTEMA");
            ps.setString(3, accion);
            ps.setString(4, tabla);
            ps.setString(5, detalle);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al registrar auditoría: " + e.getMessage());
        }
    }
}
