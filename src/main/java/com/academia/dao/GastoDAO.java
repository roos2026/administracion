package com.academia.dao;

import com.academia.config.AppSession;
import com.academia.config.DatabaseManager;
import com.academia.model.Gasto;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GastoDAO {

    public List<Map<String, Object>> listarCategorias() {
        List<Map<String, Object>> cats = new ArrayList<>();
        String sql = "SELECT id, nombre, descripcion FROM categorias_gasto ORDER BY nombre";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("nombre", rs.getString("nombre"));
                map.put("descripcion", rs.getString("descripcion"));
                cats.add(map);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cats;
    }

    public boolean registrarGasto(Gasto g) {
        String sql = """
            INSERT INTO gastos (
                categoria_id, concepto, proveedor, monto, metodo_pago,
                fecha_gasto, comprobante_ruta, es_recurrente, profesor_id, usuario_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, g.getCategoriaId());
            ps.setString(2, g.getConcepto());
            ps.setString(3, g.getProveedor());
            ps.setDouble(4, g.getMonto());
            ps.setString(5, g.getMetodoPago());
            ps.setString(6, g.getFechaGasto().toString());
            ps.setString(7, g.getComprobanteRuta());
            ps.setInt(8, g.isEsRecurrente() ? 1 : 0);
            ps.setObject(9, g.getProfesorId());
            ps.setInt(10, g.getUsuarioId());

            int res = ps.executeUpdate();
            if (res > 0) {
                AppSession.registrarAuditoria("REGISTRAR_GASTO", "gastos",
                        "Gasto registrado: $" + g.getMonto() + " en '" + g.getConcepto() +
                        "' (" + (g.getProveedor() != null ? g.getProveedor() : "S/P") + ")");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Gasto> listarGastos(LocalDate desde, LocalDate hasta) {
        List<Gasto> lista = new ArrayList<>();
        String sql = """
            SELECT g.id, g.categoria_id, c.nombre AS nombre_categoria, g.concepto, g.proveedor,
                   g.monto, g.metodo_pago, g.fecha_gasto, g.comprobante_ruta, g.es_recurrente,
                   g.profesor_id, p.nombre_completo AS nombre_profesor, g.usuario_id
            FROM gastos g
            JOIN categorias_gasto c ON g.categoria_id = c.id
            LEFT JOIN profesores p ON g.profesor_id = p.id
            WHERE g.fecha_gasto BETWEEN ? AND ?
            ORDER BY g.fecha_gasto DESC, g.id DESC
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, desde.toString());
            ps.setString(2, hasta.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Gasto g = new Gasto();
                    g.setId(rs.getInt("id"));
                    g.setCategoriaId(rs.getInt("categoria_id"));
                    g.setNombreCategoria(rs.getString("nombre_categoria"));
                    g.setConcepto(rs.getString("concepto"));
                    g.setProveedor(rs.getString("proveedor"));
                    g.setMonto(rs.getDouble("monto"));
                    g.setMetodoPago(rs.getString("metodo_pago"));
                    g.setFechaGasto(LocalDate.parse(rs.getString("fecha_gasto")));
                    g.setComprobanteRuta(rs.getString("comprobante_ruta"));
                    g.setEsRecurrente(rs.getInt("es_recurrente") == 1);
                    g.setProfesorId((Integer) rs.getObject("profesor_id"));
                    g.setNombreProfesor(rs.getString("nombre_profesor"));
                    g.setUsuarioId(rs.getInt("usuario_id"));
                    lista.add(g);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public double totalGastosMes(String anioMes) {
        String sql = "SELECT COALESCE(SUM(monto), 0.0) FROM gastos WHERE strftime('%Y-%m', fecha_gasto) = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, anioMes);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException e) {
            return 0.0;
        }
    }
}
