package com.academia.dao;

import com.academia.config.AppSession;
import com.academia.config.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NominaDAO {

    public synchronized String generarSiguienteNumeroComprobante() {
        int anio = LocalDate.now().getYear();
        String prefijo = "NOM-" + anio + "-";
        String sql = "SELECT COUNT(*) FROM gastos WHERE categoria_id IN (4, 5)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int total = rs.next() ? rs.getInt(1) : 0;
            return String.format("%s%04d", prefijo, total + 1);
        } catch (SQLException e) {
            return prefijo + (System.currentTimeMillis() % 10000);
        }
    }

    public boolean registrarPagoNomina(int categoriaId, Integer profesorId, String proveedorBeneficiario,
                                       String concepto, double monto, String metodoPago, LocalDate fechaPago,
                                       String comprobanteRuta, int usuarioId) {
        String sql = """
            INSERT INTO gastos (
                categoria_id, concepto, proveedor, monto, metodo_pago,
                fecha_gasto, comprobante_ruta, es_recurrente, profesor_id, usuario_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoriaId);
            ps.setString(2, concepto);
            ps.setString(3, proveedorBeneficiario);
            ps.setDouble(4, monto);
            ps.setString(5, metodoPago);
            ps.setString(6, fechaPago.toString());
            ps.setString(7, comprobanteRuta);
            ps.setObject(8, profesorId);
            ps.setInt(9, usuarioId);

            int res = ps.executeUpdate();
            if (res > 0) {
                AppSession.registrarAuditoria("PAGO_NOMINA", "gastos",
                        "Pago de nómina realizado a: " + proveedorBeneficiario + " por monto $" + monto + " (" + concepto + ")");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Map<String, Object>> listarPagosNomina() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = """
            SELECT g.id, g.categoria_id, c.nombre AS categoria_nombre,
                   g.concepto, g.proveedor, g.monto, g.metodo_pago,
                   g.fecha_gasto, g.comprobante_ruta, g.profesor_id,
                   u.nombre_completo AS registrado_por
            FROM gastos g
            JOIN categorias_gasto c ON g.categoria_id = c.id
            JOIN usuarios u ON g.usuario_id = u.id
            WHERE g.categoria_id IN (4, 5) -- Nómina Profesores y Nómina Administrativa
            ORDER BY g.fecha_gasto DESC, g.id DESC
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("id", rs.getInt("id"));
                fila.put("categoria", rs.getString("categoria_nombre"));
                fila.put("concepto", rs.getString("concepto"));
                fila.put("beneficiario", rs.getString("proveedor"));
                fila.put("monto", rs.getDouble("monto"));
                fila.put("metodo", rs.getString("metodo_pago"));
                fila.put("fecha", rs.getString("fecha_gasto"));
                fila.put("comprobante", rs.getString("comprobante_ruta"));
                fila.put("registrado_por", rs.getString("registrado_por"));
                lista.add(fila);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}
