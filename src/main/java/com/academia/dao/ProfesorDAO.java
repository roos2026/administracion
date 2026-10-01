package com.academia.dao;

import com.academia.config.DatabaseManager;
import com.academia.model.Profesor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProfesorDAO {

    public List<Profesor> listarActivos() {
        List<Profesor> lista = new ArrayList<>();
        String sql = "SELECT id, nombre_completo, cedula, telefono, email, especialidad, pago_por_hora, sueldo_fijo, activo FROM profesores WHERE activo = 1 ORDER BY nombre_completo";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Profesor p = new Profesor();
                p.setId(rs.getInt("id"));
                p.setNombreCompleto(rs.getString("nombre_completo"));
                p.setCedula(rs.getString("cedula"));
                p.setTelefono(rs.getString("telefono"));
                p.setEmail(rs.getString("email"));
                p.setEspecialidad(rs.getString("especialidad"));
                p.setPagoPorHora(rs.getDouble("pago_por_hora"));
                p.setSueldoFijo(rs.getDouble("sueldo_fijo"));
                p.setActivo(rs.getInt("activo") == 1);
                lista.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean crearProfesor(Profesor p) {
        String sql = "INSERT INTO profesores (nombre_completo, cedula, telefono, email, especialidad, pago_por_hora, sueldo_fijo, activo) VALUES (?, ?, ?, ?, ?, ?, ?, 1)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombreCompleto());
            ps.setString(2, p.getCedula());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getEmail());
            ps.setString(5, p.getEspecialidad());
            ps.setDouble(6, p.getPagoPorHora());
            ps.setDouble(7, p.getSueldoFijo());
            int res = ps.executeUpdate();
            if (res > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        p.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
