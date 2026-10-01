package com.academia.model;

public class Profesor {
    private int id;
    private String nombreCompleto;
    private String cedula;
    private String telefono;
    private String email;
    private String especialidad;
    private double pagoPorHora;
    private double sueldoFijo;
    private boolean activo;

    public Profesor() {}

    public Profesor(int id, String nombreCompleto, String especialidad, String telefono) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.especialidad = especialidad;
        this.telefono = telefono;
        this.activo = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }

    public double getPagoPorHora() { return pagoPorHora; }
    public void setPagoPorHora(double pagoPorHora) { this.pagoPorHora = pagoPorHora; }

    public double getSueldoFijo() { return sueldoFijo; }
    public void setSueldoFijo(double sueldoFijo) { this.sueldoFijo = sueldoFijo; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        return nombreCompleto + (especialidad != null && !especialidad.isEmpty() ? " - " + especialidad : "");
    }
}
