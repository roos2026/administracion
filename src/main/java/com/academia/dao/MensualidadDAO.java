package com.academia.dao;

import com.academia.config.AppSession;
import com.academia.config.DatabaseManager;
import com.academia.model.Mensualidad;

import java.sql.*;
import java.time.LocalDate;
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
            SELECT p.id, ?, CURRENT_DATE, ?,
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
            ps.setString(2, fechaVencimiento.toString());
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

    public List<Mensualidad> listar(String filtroEstado, String busqueda) {
        List<Mensualidad> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT m.id, m.participante_id, m.periodo, m.fecha_emision, m.fecha_vencimiento,
                   m.monto_base, m.descuento, m.recargo_mora, m.monto_total, m.saldo_pendiente, m.estado_pago,
                   p.matricula, (p.nombres || ' ' || p.apellidos) AS nombre_estudiante,
                   t.telefono_whatsapp AS telefono_tutor
            FROM mensualidades m
            JOIN participantes p ON m.participante_id = p.id
            LEFT JOIN tutores t ON p.id = t.participante_id AND t.es_principal = 1
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

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
        return listar("VENCIDO", null);
    }
}
