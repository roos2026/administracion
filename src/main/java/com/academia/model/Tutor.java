package com.academia.model;

public class Tutor {
    private int id;
    private int participanteId;
    private String nombreCompleto;
    private String cedula;
    private String telefonoWhatsapp;
    private String email;
    private String direccion;
    private String parentesco; // PADRE, MADRE, TUTOR_LEGAL, OTRO
    private boolean esPrincipal;

    public Tutor() {}

    public Tutor(String nombreCompleto, String cedula, String telefonoWhatsapp, String parentesco) {
        this.nombreCompleto = nombreCompleto;
        this.cedula = cedula;
        this.telefonoWhatsapp = telefonoWhatsapp;
        this.parentesco = parentesco;
        this.esPrincipal = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getParticipanteId() { return participanteId; }
    public void setParticipanteId(int participanteId) { this.participanteId = participanteId; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getTelefonoWhatsapp() { return telefonoWhatsapp; }
    public void setTelefonoWhatsapp(String telefonoWhatsapp) { this.telefonoWhatsapp = telefonoWhatsapp; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }

    public boolean isEsPrincipal() { return esPrincipal; }
    public void setEsPrincipal(boolean esPrincipal) { this.esPrincipal = esPrincipal; }
}
