package com.academia.dao;

import com.academia.config.AppSession;
import com.academia.config.DatabaseManager;
import com.academia.model.CierreCaja;

import java.sql.*;
import java.time.LocalDateTime;

public class CajaDAO {

    public CierreCaja obtenerCajaAbierta() {
        String sql = """
            SELECT c.id, c.usuario_id, u.nombre_completo AS nombre_usuario,
                   c.fecha_apertura, c.monto_inicial_efectivo, c.estado
            FROM cajas_arqueo c
            JOIN usuarios u ON c.usuario_id = u.id
            WHERE c.estado = 'ABIERTA'
            ORDER BY c.id DESC LIMIT 1
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                CierreCaja c = new CierreCaja();
                c.setId(rs.getInt("id"));
                c.setUsuarioId(rs.getInt("usuario_id"));
                c.setNombreUsuario(rs.getString("nombre_usuario"));
                c.setFechaApertura(LocalDateTime.parse(rs.getString("fecha_apertura").replace(" ", "T")));
                c.setMontoInicialEfectivo(rs.getDouble("monto_inicial_efectivo"));
                c.setEstado(rs.getString("estado"));
                
                // Actualizar totales en vivo de efectivo
                actualizarTotalesEnVivo(conn, c);
                return c;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void actualizarTotalesEnVivo(Connection conn, CierreCaja c) throws SQLException {
        // Ingresos en efectivo desde la fecha de apertura
        String sqlIngresos = """
            SELECT COALESCE(SUM(monto_abonado), 0.0)
            FROM pagos_abonos
            WHERE metodo_pago = 'EFECTIVO' AND estado = 'ACTIVO'
              AND fecha_pago >= ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sqlIngresos)) {
            ps.setString(1, c.getFechaApertura().toString().replace("T", " "));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    c.setTotalIngresosEfectivo(rs.getDouble(1));
                }
            }
        }

        // Salidas en efectivo desde la fecha de apertura
        String sqlEgresos = """
            SELECT COALESCE(SUM(monto), 0.0)
            FROM gastos
            WHERE metodo_pago = 'EFECTIVO'
              AND fecha_gasto >= ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sqlEgresos)) {
            ps.setString(1, c.getFechaApertura().toLocalDate().toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    c.setTotalEgresosEfectivo(rs.getDouble(1));
                }
            }
        }

        c.recalcularSaldoEsperado();
    }

    public boolean abrirCaja(int usuarioId, double montoInicial) {
        if (obtenerCajaAbierta() != null) {
            return false; // Ya hay una caja abierta
        }

        String sql = """
            INSERT INTO cajas_arqueo (usuario_id, fecha_apertura, monto_inicial_efectivo, estado)
            VALUES (?, CURRENT_TIMESTAMP, ?, 'ABIERTA')
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setDouble(2, montoInicial);
            int res = ps.executeUpdate();
            if (res > 0) {
                AppSession.registrarAuditoria("APERTURA_CAJA", "cajas_arqueo",
                        "Apertura de turno de caja con fondo inicial en efectivo: $" + montoInicial);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean cerrarCaja(int cajaId, double saldoRealContado, String notas) {
        CierreCaja caja = obtenerCajaAbierta();
        if (caja == null || caja.getId() != cajaId) return false;

        caja.setSaldoRealContado(saldoRealContado);
        caja.recalcularSaldoEsperado();

        String sql = """
            UPDATE cajas_arqueo
            SET fecha_cierre = CURRENT_TIMESTAMP,
                total_ingresos_efectivo = ?,
                total_egresos_efectivo = ?,
                saldo_esperado_efectivo = ?,
                saldo_real_contado = ?,
                diferencia = ?,
                notas = ?,
                estado = 'CERRADA'
            WHERE id = ? AND estado = 'ABIERTA'
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, caja.getTotalIngresosEfectivo());
            ps.setDouble(2, caja.getTotalEgresosEfectivo());
            ps.setDouble(3, caja.getSaldoEsperadoEfectivo());
            ps.setDouble(4, saldoRealContado);
            ps.setDouble(5, caja.getDiferencia());
            ps.setString(6, notas);
            ps.setInt(7, cajaId);

            int res = ps.executeUpdate();
            if (res > 0) {
                AppSession.registrarAuditoria("CIERRE_CAJA", "cajas_arqueo",
                        "Arqueo de caja finalizado. Esperado: $" + caja.getSaldoEsperadoEfectivo() +
                        ", Real: $" + saldoRealContado + ", Diferencia: $" + caja.getDiferencia());
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
