package com.academia.model;

public class Grupo {
    private int id;
    private String codigo;
    private String nombre;
    private String dias;
    private String horario;
    private int cupoMaximo;
    private double costoMensualidad;
    private double costoInscripcion;
    private Integer profesorId;
    private String nombreProfesor;
    private int totalInscritos;
    private boolean activo;

    public Grupo() {}

    public Grupo(int id, String codigo, String nombre, String dias, String horario, int cupoMaximo, double costoMensualidad) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.dias = dias;
        this.horario = horario;
        this.cupoMaximo = cupoMaximo;
        this.costoMensualidad = costoMensualidad;
        this.activo = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDias() { return dias; }
    public void setDias(String dias) { this.dias = dias; }

    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }

    public int getCupoMaximo() { return cupoMaximo; }
    public void setCupoMaximo(int cupoMaximo) { this.cupoMaximo = cupoMaximo; }

    public double getCostoMensualidad() { return costoMensualidad; }
    public void setCostoMensualidad(double costoMensualidad) { this.costoMensualidad = costoMensualidad; }

    public double getCostoInscripcion() { return costoInscripcion; }
    public void setCostoInscripcion(double costoInscripcion) { this.costoInscripcion = costoInscripcion; }

    public Integer getProfesorId() { return profesorId; }
    public void setProfesorId(Integer profesorId) { this.profesorId = profesorId; }

    public String getNombreProfesor() { return nombreProfesor; }
    public void setNombreProfesor(String nombreProfesor) { this.nombreProfesor = nombreProfesor; }

    public int getTotalInscritos() { return totalInscritos; }
    public void setTotalInscritos(int totalInscritos) { this.totalInscritos = totalInscritos; }

    public int getCuposDisponibles() {
        return Math.max(0, cupoMaximo - totalInscritos);
    }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        return nombre + " (" + dias + " " + horario + ") [" + totalInscritos + "/" + cupoMaximo + "]";
    }
}
