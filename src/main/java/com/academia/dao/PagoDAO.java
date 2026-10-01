package com.academia.dao;

import com.academia.config.AppSession;
import com.academia.config.DatabaseManager;
import com.academia.model.PagoAbono;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PagoDAO {

    public synchronized String generarSiguienteNumeroRecibo() {
        int anio = LocalDate.now().getYear();
        String prefijo = "REC-" + anio + "-";
        String sql = "SELECT COUNT(*) FROM pagos_abonos WHERE numero_recibo LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefijo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int total = rs.next() ? rs.getInt(1) : 0;
                return String.format("%s%06d", prefijo, total + 1);
            }
        } catch (SQLException e) {
            return prefijo + System.currentTimeMillis() % 1000000;
        }
    }

    public boolean registrarAbonoPago(PagoAbono pago) {
        String sqlPago = """
            INSERT INTO pagos_abonos (
                numero_recibo, participante_id, mensualidad_id, concepto_id,
                concepto_detalle, monto_abonado, metodo_pago, referencia_pago,
                usuario_id, estado
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVO')
        """;

        String sqlUpdateMensualidad = """
            UPDATE mensualidades
            SET saldo_pendiente = MAX(0.0, saldo_pendiente - ?),
                estado_pago = CASE WHEN (saldo_pendiente - ?) <= 0.001 THEN 'PAGADO' ELSE estado_pago END
            WHERE id = ?
        """;

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            if (pago.getNumeroRecibo() == null || pago.getNumeroRecibo().isBlank()) {
                pago.setNumeroRecibo(generarSiguienteNumeroRecibo());
            }

            // 1. Guardar pago
            try (PreparedStatement ps = conn.prepareStatement(sqlPago, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, pago.getNumeroRecibo());
                ps.setInt(2, pago.getParticipanteId());
                ps.setObject(3, pago.getMensualidadId());
                ps.setObject(4, pago.getConceptoId());
                ps.setString(5, pago.getConceptoDetalle());
                ps.setDouble(6, pago.getMontoAbonado());
                ps.setString(7, pago.getMetodoPago());
                ps.setString(8, pago.getReferenciaPago());
                ps.setInt(9, pago.getUsuarioId());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        pago.setId(rs.getInt(1));
                    }
                }
            }

            // 2. Si abona a una mensualidad, descontar del saldo pendiente
            if (pago.getMensualidadId() != null) {
                try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdateMensualidad)) {
                    psUpdate.setDouble(1, pago.getMontoAbonado());
                    psUpdate.setDouble(2, pago.getMontoAbonado());
                    psUpdate.setInt(3, pago.getMensualidadId());
                    psUpdate.executeUpdate();
                }
            }

            conn.commit();
            AppSession.registrarAuditoria("REGISTRAR_PAGO", "pagos_abonos",
                    "Recibo emitido: " + pago.getNumeroRecibo() + " por monto $" + pago.getMontoAbonado() +
                    " (" + pago.getConceptoDetalle() + ") a estudiante ID " + pago.getParticipanteId());
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    public boolean anularPago(int pagoId, String motivo, int usuarioAnulacionId) {
        String sqlBuscar = "SELECT id, numero_recibo, mensualidad_id, monto_abonado, estado FROM pagos_abonos WHERE id = ?";
        String sqlAnular = """
            UPDATE pagos_abonos
            SET estado = 'ANULADO', motivo_anulacion = ?, fecha_anulacion = CURRENT_TIMESTAMP, usuario_anulacion_id = ?
            WHERE id = ? AND estado = 'ACTIVO'
        """;
        String sqlRestaurarMensualidad = """
            UPDATE mensualidades
            SET saldo_pendiente = saldo_pendiente + ?,
                estado_pago = CASE WHEN fecha_vencimiento < CURRENT_DATE THEN 'VENCIDO' ELSE 'VENCE_PRONTO' END
            WHERE id = ?
        """;

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            int mensualidadId = 0;
            double monto = 0;
            String numeroRecibo = "";

            try (PreparedStatement psBusqueda = conn.prepareStatement(sqlBuscar)) {
                psBusqueda.setInt(1, pagoId);
                try (ResultSet rs = psBusqueda.executeQuery()) {
                    if (rs.next()) {
                        if ("ANULADO".equalsIgnoreCase(rs.getString("estado"))) {
                            return false; // Ya está anulado
                        }
                        mensualidadId = rs.getInt("mensualidad_id");
                        monto = rs.getDouble("monto_abonado");
                        numeroRecibo = rs.getString("numero_recibo");
                    } else {
                        return false;
                    }
                }
            }

            // 1. Marcar el recibo como anulado
            try (PreparedStatement psAnular = conn.prepareStatement(sqlAnular)) {
                psAnular.setString(1, motivo);
                psAnular.setInt(2, usuarioAnulacionId);
                psAnular.setInt(3, pagoId);
                int filas = psAnular.executeUpdate();
                if (filas == 0) return false;
            }

            // 2. Restaurar saldo en mensualidad
            if (mensualidadId > 0) {
                try (PreparedStatement psRestaurar = conn.prepareStatement(sqlRestaurarMensualidad)) {
                    psRestaurar.setDouble(1, monto);
                    psRestaurar.setInt(2, mensualidadId);
                    psRestaurar.executeUpdate();
                }
            }

            conn.commit();
            AppSession.registrarAuditoria("ANULAR_PAGO", "pagos_abonos",
                    "Recibo anulado: " + numeroRecibo + " por motivo: " + motivo);
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    public List<PagoAbono> listarHistorial(int limite) {
        List<PagoAbono> lista = new ArrayList<>();
        String sql = """
            SELECT p.id, p.numero_recibo, p.participante_id, p.mensualidad_id, p.concepto_id,
                   p.concepto_detalle, p.monto_abonado, p.metodo_pago, p.referencia_pago,
                   p.fecha_pago, p.usuario_id, p.estado, p.motivo_anulacion, p.fecha_anulacion,
                   (part.nombres || ' ' || part.apellidos) AS nombre_estudiante,
                   u.nombre_completo AS nombre_cajero
            FROM pagos_abonos p
            JOIN participantes part ON p.participante_id = part.id
            JOIN usuarios u ON p.usuario_id = u.id
            ORDER BY p.id DESC
            LIMIT ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PagoAbono p = new PagoAbono();
                    p.setId(rs.getInt("id"));
                    p.setNumeroRecibo(rs.getString("numero_recibo"));
                    p.setParticipanteId(rs.getInt("participante_id"));
                    p.setMensualidadId((Integer) rs.getObject("mensualidad_id"));
                    p.setConceptoId((Integer) rs.getObject("concepto_id"));
                    p.setConceptoDetalle(rs.getString("concepto_detalle"));
                    p.setMontoAbonado(rs.getDouble("monto_abonado"));
                    p.setMetodoPago(rs.getString("metodo_pago"));
                    p.setReferenciaPago(rs.getString("referencia_pago"));
                    p.setFechaPago(LocalDateTime.parse(rs.getString("fecha_pago").replace(" ", "T")));
                    p.setUsuarioId(rs.getInt("usuario_id"));
                    p.setEstado(rs.getString("estado"));
                    p.setMotivoAnulacion(rs.getString("motivo_anulacion"));
                    p.setNombreEstudiante(rs.getString("nombre_estudiante"));
                    p.setNombreCajero(rs.getString("nombre_cajero"));
                    lista.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}
