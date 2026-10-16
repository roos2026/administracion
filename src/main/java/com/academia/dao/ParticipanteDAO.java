package com.academia.dao;

import com.academia.config.AppSession;
import com.academia.config.DatabaseManager;
import com.academia.model.Participante;
import com.academia.model.Tutor;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ParticipanteDAO {

    public synchronized String generarSiguienteMatricula() {
        int anio = LocalDate.now().getYear();
        String prefijo = "ACAD-" + anio + "-";
        String sql = "SELECT COUNT(*) FROM participantes WHERE matricula LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefijo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int total = rs.next() ? rs.getInt(1) : 0;
                return String.format("%s%04d", prefijo, total + 1);
            }
        } catch (SQLException e) {
            return prefijo + System.currentTimeMillis() % 10000;
        }
    }

    public boolean registrarParticipante(Participante p, Tutor t, String rutaFirma, String rutaContratoPdf) {
        String sqlParticipante = """
            INSERT INTO participantes (
                matricula, nombres, apellidos, fecha_nacimiento, foto_ruta,
                contacto_emergencia_nombre, contacto_emergencia_telefono, alergias_condiciones,
                estado, grupo_id, dia_corte_habitual, fecha_inscripcion
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVO', ?, ?, ?)
        """;

        String sqlTutor = """
            INSERT INTO tutores (
                participante_id, nombre_completo, cedula, telefono_whatsapp, email, direccion, parentesco, es_principal
            ) VALUES (?, ?, ?, ?, ?, ?, ?, 1)
        """;

        String sqlContrato = """
            INSERT INTO contratos (
                participante_id, version_terminos, fecha_firma, firma_digital_ruta,
                pdf_contrato_ruta, firmado_por_nombre, firmado_por_cedula
            ) VALUES (?, 'v1.0-2026', CURRENT_TIMESTAMP, ?, ?, ?, ?)
        """;

        // Calcular ciclo de pago según la regla de negocio:
        LocalDate fechaIns = p.getFechaInscripcion() != null ? p.getFechaInscripcion() : LocalDate.now();
        com.academia.service.BillingCycleService.VencimientoInfo infoVenc = com.academia.service.BillingCycleService.calcularPrimerVencimiento(fechaIns);
        p.setDiaCorteHabitual(infoVenc.getDiaCorteHabitual());

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Transacción atómica

            // 1. Insertar participante
            int participanteId;
            try (PreparedStatement ps = conn.prepareStatement(sqlParticipante, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getMatricula());
                ps.setString(2, p.getNombres());
                ps.setString(3, p.getApellidos());
                ps.setString(4, p.getFechaNacimiento().toString());
                ps.setString(5, p.getFotoRuta());
                ps.setString(6, p.getContactoEmergenciaNombre());
                ps.setString(7, p.getContactoEmergenciaTelefono());
                ps.setString(8, p.getAlergiasCondiciones());
                ps.setObject(9, p.getGrupoId());
                ps.setInt(10, p.getDiaCorteHabitual());
                ps.setString(11, fechaIns.toString());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        participanteId = rs.getInt(1);
                        p.setId(participanteId);
                    } else {
                        throw new SQLException("No se pudo obtener el ID del participante generado.");
                    }
                }
            }

            // 2. Insertar Tutor si existe (obligatorio si es menor de edad)
            if (t != null && t.getNombreCompleto() != null && !t.getNombreCompleto().isBlank()) {
                try (PreparedStatement psTutor = conn.prepareStatement(sqlTutor)) {
                    psTutor.setInt(1, participanteId);
                    psTutor.setString(2, t.getNombreCompleto());
                    psTutor.setString(3, t.getCedula());
                    psTutor.setString(4, t.getTelefonoWhatsapp());
                    psTutor.setString(5, t.getEmail());
                    psTutor.setString(6, t.getDireccion());
                    psTutor.setString(7, t.getParentesco() != null ? t.getParentesco() : "TUTOR_LEGAL");
                    psTutor.executeUpdate();
                }
            }

            // 3. Registrar Contrato digital si se firmó
            if (rutaFirma != null && !rutaFirma.isBlank()) {
                try (PreparedStatement psContrato = conn.prepareStatement(sqlContrato)) {
                    psContrato.setInt(1, participanteId);
                    psContrato.setString(2, rutaFirma);
                    psContrato.setString(3, rutaContratoPdf);
                    psContrato.setString(4, t != null ? t.getNombreCompleto() : p.getNombreCompleto());
                    psContrato.setString(5, t != null ? t.getCedula() : "N/A");
                    psContrato.executeUpdate();
                }
            }

            // 4. Generar automáticamente la primera mensualidad con el vencimiento calculado por fórmula
            double costoCuota = 2500.0;
            if (p.getGrupoId() != null) {
                String sqlCostoGrupo = "SELECT costo_mensualidad FROM grupos WHERE id = ?";
                try (PreparedStatement psG = conn.prepareStatement(sqlCostoGrupo)) {
                    psG.setInt(1, p.getGrupoId());
                    try (ResultSet rsG = psG.executeQuery()) {
                        if (rsG.next() && rsG.getDouble(1) > 0) {
                            costoCuota = rsG.getDouble(1);
                        }
                    }
                }
            }

            String sqlMensualidadInicial = """
                INSERT OR IGNORE INTO mensualidades (
                    participante_id, periodo, fecha_emision, fecha_vencimiento,
                    monto_base, descuento, recargo_mora, monto_total, saldo_pendiente, estado_pago
                ) VALUES (?, ?, ?, ?, ?, 0.0, 0.0, ?, ?, 'VENCE_PRONTO')
            """;
            try (PreparedStatement psMen = conn.prepareStatement(sqlMensualidadInicial)) {
                psMen.setInt(1, participanteId);
                psMen.setString(2, infoVenc.getPeriodo());
                psMen.setString(3, fechaIns.toString());
                psMen.setString(4, infoVenc.getFechaVencimiento().toString());
                psMen.setDouble(5, costoCuota);
                psMen.setDouble(6, costoCuota);
                psMen.setDouble(7, costoCuota);
                psMen.executeUpdate();
            }

            conn.commit();
            AppSession.registrarAuditoria("REGISTRO_PARTICIPANTE", "participantes",
                    "Nuevo estudiante inscrito: " + p.getNombreCompleto() + " [" + p.getMatricula() +
                    "] - Cuota inicial vence: " + infoVenc.getFechaVencimiento() + " (Corte día " + p.getDiaCorteHabitual() + ")");
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

    public List<Participante> listar(String filtroEstado, String busqueda) {
        List<Participante> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT p.id, p.matricula, p.nombres, p.apellidos, p.fecha_nacimiento,
                   p.foto_ruta, p.contacto_emergencia_nombre, p.contacto_emergencia_telefono,
                   p.alergias_condiciones, p.estado, p.motivo_baja, p.fecha_baja, p.grupo_id,
                   p.dia_corte_habitual, p.fecha_inscripcion,
                   g.nombre AS nombre_grupo,
                   t.id AS tutor_id, t.nombre_completo AS tutor_nombre, t.cedula AS tutor_cedula,
                   t.telefono_whatsapp AS tutor_whatsapp, t.email AS tutor_email, t.parentesco AS tutor_parentesco
            FROM participantes p
            LEFT JOIN grupos g ON p.grupo_id = g.id
            LEFT JOIN tutores t ON p.id = t.participante_id AND t.es_principal = 1
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        if (filtroEstado != null && !filtroEstado.equalsIgnoreCase("TODOS")) {
            sql.append(" AND p.estado = ?");
            params.add(filtroEstado.toUpperCase());
        }

        if (busqueda != null && !busqueda.isBlank()) {
            sql.append(" AND (p.nombres LIKE ? OR p.apellidos LIKE ? OR p.matricula LIKE ? OR t.nombre_completo LIKE ?)");
            String query = "%" + busqueda.trim() + "%";
            params.add(query);
            params.add(query);
            params.add(query);
            params.add(query);
        }

        sql.append(" ORDER BY p.apellidos, p.nombres");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Participante p = new Participante();
                    p.setId(rs.getInt("id"));
                    p.setMatricula(rs.getString("matricula"));
                    p.setNombres(rs.getString("nombres"));
                    p.setApellidos(rs.getString("apellidos"));
                    String fNac = rs.getString("fecha_nacimiento");
                    if (fNac != null) p.setFechaNacimiento(LocalDate.parse(fNac));
                    p.setFotoRuta(rs.getString("foto_ruta"));
                    p.setContactoEmergenciaNombre(rs.getString("contacto_emergencia_nombre"));
                    p.setContactoEmergenciaTelefono(rs.getString("contacto_emergencia_telefono"));
                    p.setAlergiasCondiciones(rs.getString("alergias_condiciones"));
                    p.setEstado(rs.getString("estado"));
                    p.setMotivoBaja(rs.getString("motivo_baja"));
                    String fBaja = rs.getString("fecha_baja");
                    if (fBaja != null) p.setFechaBaja(LocalDate.parse(fBaja));
                    p.setGrupoId((Integer) rs.getObject("grupo_id"));
                    p.setNombreGrupo(rs.getString("nombre_grupo"));
                    p.setDiaCorteHabitual(rs.getInt("dia_corte_habitual") > 0 ? rs.getInt("dia_corte_habitual") : 30);
                    String fIns = rs.getString("fecha_inscripcion");
                    if (fIns != null) p.setFechaInscripcion(LocalDate.parse(fIns));

                    // Tutor
                    if (rs.getObject("tutor_id") != null) {
                        Tutor t = new Tutor();
                        t.setId(rs.getInt("tutor_id"));
                        t.setParticipanteId(p.getId());
                        t.setNombreCompleto(rs.getString("tutor_nombre"));
                        t.setCedula(rs.getString("tutor_cedula"));
                        t.setTelefonoWhatsapp(rs.getString("tutor_whatsapp"));
                        t.setEmail(rs.getString("tutor_email"));
                        t.setParentesco(rs.getString("tutor_parentesco"));
                        p.setTutor(t);
                    }

                    lista.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean cambiarEstado(int participanteId, String nuevoEstado, String motivo) {
        String sql;
        if ("ACTIVO".equalsIgnoreCase(nuevoEstado)) {
            sql = "UPDATE participantes SET estado = 'ACTIVO', motivo_baja = NULL, fecha_baja = NULL WHERE id = ?";
        } else {
            sql = "UPDATE participantes SET estado = ?, motivo_baja = ?, fecha_baja = CURRENT_DATE WHERE id = ?";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if ("ACTIVO".equalsIgnoreCase(nuevoEstado)) {
                ps.setInt(1, participanteId);
            } else {
                ps.setString(1, nuevoEstado.toUpperCase());
                ps.setString(2, motivo);
                ps.setInt(3, participanteId);
            }
            int res = ps.executeUpdate();
            if (res > 0) {
                AppSession.registrarAuditoria("CAMBIO_ESTADO", "participantes",
                        "Estudiante ID " + participanteId + " pasó a estado " + nuevoEstado + (motivo != null ? " por: " + motivo : ""));

                // GESTIÓN AUTOMÁTICA EN MENSUALIDADES SEGÚN ESTADO:
                MensualidadDAO mensualidadDAO = new MensualidadDAO();
                if ("ACTIVO".equalsIgnoreCase(nuevoEstado)) {
                    // Si se reactiva, se reincorpora inmediatamente a la lista activa de mensualidades
                    mensualidadDAO.reintegrarEnMensualidades(participanteId);
                } else {
                    // Si pasa a PAUSA o INACTIVO, se quita automáticamente de la lista de mensualidades (no va a pagar)
                    mensualidadDAO.removerDeListaMensualidades(participanteId, nuevoEstado);
                }

                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int contarPorEstado(String estado) {
        String sql = "SELECT COUNT(*) FROM participantes WHERE estado = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }
}
