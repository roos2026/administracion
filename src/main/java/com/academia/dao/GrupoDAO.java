package com.academia.dao;

import com.academia.config.AppSession;
import com.academia.config.DatabaseManager;
import com.academia.model.Grupo;
import com.academia.model.Participante;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GrupoDAO {

    public synchronized String generarSiguienteCodigo() {
        String sql = "SELECT COUNT(*) FROM grupos";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int total = rs.next() ? rs.getInt(1) : 0;
            return String.format("GRP-%03d", total + 1);
        } catch (SQLException e) {
            return "GRP-" + (System.currentTimeMillis() % 1000);
        }
    }

    public List<Grupo> listarGruposActivos() {
        return listar(true, null);
    }

    public List<Grupo> listarTodos(String busqueda) {
        return listar(false, busqueda);
    }

    private List<Grupo> listar(boolean soloActivos, String busqueda) {
        List<Grupo> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT g.id, g.codigo, g.nombre, g.dias, g.horario, g.cupo_maximo,
                   g.costo_mensualidad, g.costo_inscripcion, g.profesor_id, g.activo,
                   p.nombre_completo AS nombre_profesor,
                   (SELECT COUNT(*) FROM participantes part WHERE part.grupo_id = g.id AND part.estado = 'ACTIVO') AS total_inscritos
            FROM grupos g
            LEFT JOIN profesores p ON g.profesor_id = p.id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (soloActivos) {
            sql.append(" AND g.activo = 1");
        }

        if (busqueda != null && !busqueda.isBlank()) {
            sql.append(" AND (g.nombre LIKE ? OR g.codigo LIKE ? OR g.dias LIKE ? OR p.nombre_completo LIKE ?)");
            String query = "%" + busqueda.trim() + "%";
            params.add(query);
            params.add(query);
            params.add(query);
            params.add(query);
        }

        sql.append(" ORDER BY g.activo DESC, g.nombre ASC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Grupo g = new Grupo();
                    g.setId(rs.getInt("id"));
                    g.setCodigo(rs.getString("codigo"));
                    g.setNombre(rs.getString("nombre"));
                    g.setDias(rs.getString("dias"));
                    g.setHorario(rs.getString("horario"));
                    g.setCupoMaximo(rs.getInt("cupo_maximo"));
                    g.setCostoMensualidad(rs.getDouble("costo_mensualidad"));
                    g.setCostoInscripcion(rs.getDouble("costo_inscripcion"));
                    g.setProfesorId((Integer) rs.getObject("profesor_id"));
                    g.setNombreProfesor(rs.getString("nombre_profesor"));
                    g.setTotalInscritos(rs.getInt("total_inscritos"));
                    g.setActivo(rs.getInt("activo") == 1);
                    lista.add(g);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean crearGrupo(Grupo g) {
        String sql = """
            INSERT INTO grupos (codigo, nombre, dias, horario, cupo_maximo, costo_mensualidad, costo_inscripcion, profesor_id, activo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, g.getCodigo());
            ps.setString(2, g.getNombre());
            ps.setString(3, g.getDias());
            ps.setString(4, g.getHorario());
            ps.setInt(5, g.getCupoMaximo());
            ps.setDouble(6, g.getCostoMensualidad());
            ps.setDouble(7, g.getCostoInscripcion());
            ps.setObject(8, g.getProfesorId());
            int res = ps.executeUpdate();
            if (res > 0) {
                AppSession.registrarAuditoria("CREAR_GRUPO", "grupos",
                        "Grupo creado: " + g.getNombre() + " [" + g.getCodigo() + "]");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean actualizarGrupo(Grupo g) {
        String sql = """
            UPDATE grupos
            SET codigo = ?, nombre = ?, dias = ?, horario = ?, cupo_maximo = ?,
                costo_mensualidad = ?, costo_inscripcion = ?, profesor_id = ?
            WHERE id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, g.getCodigo());
            ps.setString(2, g.getNombre());
            ps.setString(3, g.getDias());
            ps.setString(4, g.getHorario());
            ps.setInt(5, g.getCupoMaximo());
            ps.setDouble(6, g.getCostoMensualidad());
            ps.setDouble(7, g.getCostoInscripcion());
            ps.setObject(8, g.getProfesorId());
            ps.setInt(9, g.getId());
            int res = ps.executeUpdate();
            if (res > 0) {
                AppSession.registrarAuditoria("EDITAR_GRUPO", "grupos",
                        "Grupo actualizado: " + g.getNombre() + " [" + g.getCodigo() + "]");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean cambiarEstadoGrupo(int grupoId, boolean activo) {
        String sql = "UPDATE grupos SET activo = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, activo ? 1 : 0);
            ps.setInt(2, grupoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Participante> obtenerEstudiantesDelGrupo(int grupoId) {
        List<Participante> lista = new ArrayList<>();
        String sql = """
            SELECT id, matricula, nombres, apellidos, estado, fecha_nacimiento
            FROM participantes
            WHERE grupo_id = ? AND estado = 'ACTIVO'
            ORDER BY apellidos, nombres
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, grupoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Participante p = new Participante();
                    p.setId(rs.getInt("id"));
                    p.setMatricula(rs.getString("matricula"));
                    p.setNombres(rs.getString("nombres"));
                    p.setApellidos(rs.getString("apellidos"));
                    p.setEstado(rs.getString("estado"));
                    lista.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}
