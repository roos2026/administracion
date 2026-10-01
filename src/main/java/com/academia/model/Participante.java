package com.academia.model;

import java.time.LocalDate;
import java.time.Period;

public class Participante {
    private int id;
    private String matricula;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String fotoRuta;
    private String contactoEmergenciaNombre;
    private String contactoEmergenciaTelefono;
    private String alergiasCondiciones;
    private String estado; // ACTIVO, PAUSA, INACTIVO
    private String motivoBaja;
    private LocalDate fechaBaja;
    private Integer grupoId;
    private String nombreGrupo;
    private Tutor tutor;

    public Participante() {
        this.estado = "ACTIVO";
    }

    public int getEdad() {
        if (fechaNacimiento == null) return 0;
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    public boolean isMenorDeEdad() {
        return getEdad() < 18;
    }

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getFotoRuta() { return fotoRuta; }
    public void setFotoRuta(String fotoRuta) { this.fotoRuta = fotoRuta; }

    public String getContactoEmergenciaNombre() { return contactoEmergenciaNombre; }
    public void setContactoEmergenciaNombre(String contactoEmergenciaNombre) { this.contactoEmergenciaNombre = contactoEmergenciaNombre; }

    public String getContactoEmergenciaTelefono() { return contactoEmergenciaTelefono; }
    public void setContactoEmergenciaTelefono(String contactoEmergenciaTelefono) { this.contactoEmergenciaTelefono = contactoEmergenciaTelefono; }

    public String getAlergiasCondiciones() { return alergiasCondiciones; }
    public void setAlergiasCondiciones(String alergiasCondiciones) { this.alergiasCondiciones = alergiasCondiciones; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMotivoBaja() { return motivoBaja; }
    public void setMotivoBaja(String motivoBaja) { this.motivoBaja = motivoBaja; }

    public LocalDate getFechaBaja() { return fechaBaja; }
    public void setFechaBaja(LocalDate fechaBaja) { this.fechaBaja = fechaBaja; }

    public Integer getGrupoId() { return grupoId; }
    public void setGrupoId(Integer grupoId) { this.grupoId = grupoId; }

    public String getNombreGrupo() { return nombreGrupo; }
    public void setNombreGrupo(String nombreGrupo) { this.nombreGrupo = nombreGrupo; }

    public Tutor getTutor() { return tutor; }
    public void setTutor(Tutor tutor) { this.tutor = tutor; }

    @Override
    public String toString() {
        return getNombreCompleto() + " (" + matricula + ")";
    }
}
