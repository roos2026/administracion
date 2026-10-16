package com.academia.dao;

import com.academia.config.AppSession;
import com.academia.config.DatabaseManager;
import com.academia.model.Mensualidad;
import com.academia.service.BillingCycleService;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class MensualidadDAO {

    /**
     * Genera automáticamente las mensualidades del mes especificado (ej. '2026-10')
     * solo para participantes que se encuentren en estado ACTIVO (los inactivos y en pausa no generan cobros).
     */
    public int generarMensualidadesParaActivos(String periodo, LocalDate fechaVencimiento) {
        String sql = """
            INSERT OR IGNORE INTO mensualidades (
                participante_id, periodo, fecha_emision, fecha_vencimiento,
                monto_base, descuento, recargo_mora, monto_total, saldo_pendiente, estado_pago
            )
            SELECT p.id, ?, CURRENT_DATE,
                   CASE WHEN COALESCE(p.dia_corte_habitual, 30) = 15 THEN (? || '-15') ELSE ? END,
                   COALESCE(g.costo_mensualidad, 2500.0), 0.0, 0.0,
                   COALESCE(g.costo_mensualidad, 2500.0),
                   COALESCE(g.costo_mensualidad, 2500.0), 'VENCE_PRONTO'
            FROM participantes p
            LEFT JOIN grupos g ON p.grupo_id = g.id
            WHERE p.estado = 'ACTIVO'
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, periodo);
            ps.setString(2, periodo);
            ps.setString(3, fechaVencimiento.toString());
            int generadas = ps.executeUpdate();
            if (generadas > 0) {
                AppSession.registrarAuditoria("GENERAR_MENSUALIDADES", "mensualidades",
                        "Generadas " + generadas + " mensualidades para el período " + periodo);
            }
            return generadas;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Quita automáticamente al participante de la lista activa de mensualidades cuando pasa a PAUSA o INACTIVO,
     * eliminando cuotas del período actual o futuras que no hayan recibido ningún abono
     * (ya que no va a pagar por ese período).
     */
    public boolean removerDeListaMensualidades(int participanteId, String nuevoEstado) {
        String sql = """
            DELETE FROM mensualidades 
            WHERE participante_id = ? 
              AND saldo_pendiente = monto_total 
              AND id NOT IN (SELECT mensualidad_id FROM pagos_abonos WHERE mensualidad_id IS NOT NULL)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, participanteId);
            int eliminadas = ps.executeUpdate();
            AppSession.registrarAuditoria("PAUSA_MENSUALIDAD", "mensualidades",
                    "Estudiante ID " + participanteId + " pasó a " + nuevoEstado +
                    ". Cuotas pendientes no cobradas removidas de la lista: " + eliminadas);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reincorpora automáticamente al participante en la lista activa de mensualidades cuando vuelve a estar ACTIVO.
     * Si no tiene cuota generada para el período actual, la crea de inmediato con el cálculo exacto de vencimiento.
     */
    public boolean reintegrarEnMensualidades(int participanteId) {
        YearMonth ymActual = YearMonth.now();
        String periodoActual = ymActual.toString();

        // 1. Verificar si ya existe mensualidad para el período actual
        String sqlCheck = "SELECT COUNT(*) FROM mensualidades WHERE participante_id = ? AND periodo = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            psCheck.setInt(1, participanteId);
            psCheck.setString(2, periodoActual);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    AppSession.registrarAuditoria("REINTEGRO_MENSUALIDAD", "mensualidades",
                            "Estudiante ID " + participanteId + " reactivado en lista de mensualidades para el período " + periodoActual);
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 2. Si no tiene mensualidad activa del mes, buscar su grupo, costo y día de corte para generarla
        String sqlInfo = """
            SELECT p.dia_corte_habitual, COALESCE(g.costo_mensualidad, 2500.0) AS costo
            FROM participantes p
            LEFT JOIN grupos g ON p.grupo_id = g.id
            WHERE p.id = ?
        """;

        int diaCorte = 30;
        double costo = 2500.0;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement psInfo = conn.prepareStatement(sqlInfo)) {
            psInfo.setInt(1, participanteId);
            try (ResultSet rs = psInfo.executeQuery()) {
                if (rs.next()) {
                    diaCorte = rs.getInt("dia_corte_habitual") == 15 ? 15 : 30;
                    costo = rs.getDouble("costo");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        LocalDate fechaVencimiento = BillingCycleService.calcularVencimientoParaMes(ymActual, diaCorte);

        String sqlInsert = """
            INSERT OR IGNORE INTO mensualidades (
                participante_id, periodo, fecha_emision, fecha_vencimiento,
                monto_base, descuento, recargo_mora, monto_total, saldo_pendiente, estado_pago
            ) VALUES (?, ?, CURRENT_DATE, ?, ?, 0.0, 0.0, ?, ?, 'VENCE_PRONTO')
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement psIns = conn.prepareStatement(sqlInsert)) {
            psIns.setInt(1, participanteId);
            psIns.setString(2, periodoActual);
            psIns.setString(3, fechaVencimiento.toString());
            psIns.setDouble(4, costo);
            psIns.setDouble(5, costo);
            psIns.setDouble(6, costo);
            int res = psIns.executeUpdate();
            if (res > 0) {
                AppSession.registrarAuditoria("REINTEGRO_MENSUALIDAD", "mensualidades",
                        "Estudiante ID " + participanteId + " reincorporado y cuota creada para " + periodoActual + " (Vence: " + fechaVencimiento + ")");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Mensualidad> listar(String filtroEstado, String busqueda) {
        return listar(filtroEstado, busqueda, true);
    }

    public List<Mensualidad> listar(String filtroEstado, String busqueda, boolean soloActivos) {
        List<Mensualidad> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT m.id, m.participante_id, m.periodo, m.fecha_emision, m.fecha_vencimiento,
                   m.monto_base, m.descuento, m.recargo_mora, m.monto_total, m.saldo_pendiente, m.estado_pago,
                   p.matricula, (p.nombres || ' ' || p.apellidos) AS nombre_estudiante,
                   p.estado AS estado_estudiante,
                   t.telefono_whatsapp AS telefono_tutor
            FROM mensualidades m
            JOIN participantes p ON m.participante_id = p.id
            LEFT JOIN tutores t ON p.id = t.participante_id AND t.es_principal = 1
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (soloActivos) {
            sql.append(" AND p.estado = 'ACTIVO'");
        }

        if (busqueda != null && !busqueda.isBlank()) {
            sql.append(" AND (p.nombres LIKE ? OR p.apellidos LIKE ? OR p.matricula LIKE ?)");
            String query = "%" + busqueda.trim() + "%";
            params.add(query);
            params.add(query);
            params.add(query);
        }

        sql.append(" ORDER BY m.fecha_vencimiento ASC, p.apellidos ASC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Mensualidad m = new Mensualidad();
                    m.setId(rs.getInt("id"));
                    m.setParticipanteId(rs.getInt("participante_id"));
                    m.setMatriculaEstudiante(rs.getString("matricula"));
                    m.setNombreEstudiante(rs.getString("nombre_estudiante"));
                    m.setEstadoEstudiante(rs.getString("estado_estudiante"));
                    m.setTelefonoTutor(rs.getString("telefono_tutor"));
                    m.setPeriodo(rs.getString("periodo"));
                    m.setFechaEmision(LocalDate.parse(rs.getString("fecha_emision")));
                    m.setFechaVencimiento(LocalDate.parse(rs.getString("fecha_vencimiento")));
                    m.setMontoBase(rs.getDouble("monto_base"));
                    m.setDescuento(rs.getDouble("descuento"));
                    m.setRecargoMora(rs.getDouble("recargo_mora"));
                    m.setMontoTotal(rs.getDouble("monto_total"));
                    m.setSaldoPendiente(rs.getDouble("saldo_pendiente"));

                    // Cálculo dinámico del semáforo de 3 colores
                    String estadoCalculado = m.calcularEstadoSemaforo();
                    m.setEstadoPago(estadoCalculado);

                    // Filtrar en memoria por semáforo si aplica
                    if (filtroEstado == null || filtroEstado.equalsIgnoreCase("TODOS") ||
                        filtroEstado.equalsIgnoreCase(estadoCalculado)) {
                        lista.add(m);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<Mensualidad> obtenerMorosos() {
        return listar("VENCIDO", null, true);
    }
}
