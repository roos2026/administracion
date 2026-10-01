package com.academia.model;

import java.time.LocalDateTime;

public class PagoAbono {
    private int id;
    private String numeroRecibo;
    private int participanteId;
    private String nombreEstudiante;
    private Integer mensualidadId;
    private Integer conceptoId;
    private String conceptoDetalle;
    private double montoAbonado;
    private String metodoPago; // EFECTIVO, TRANSFERENCIA, TARJETA, OTRO
    private String referenciaPago;
    private LocalDateTime fechaPago;
    private int usuarioId;
    private String nombreCajero;
    private String estado; // ACTIVO, ANULADO
    private String motivoAnulacion;
    private LocalDateTime fechaAnulacion;

    public PagoAbono() {
        this.estado = "ACTIVO";
        this.fechaPago = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumeroRecibo() { return numeroRecibo; }
    public void setNumeroRecibo(String numeroRecibo) { this.numeroRecibo = numeroRecibo; }

    public int getParticipanteId() { return participanteId; }
    public void setParticipanteId(int participanteId) { this.participanteId = participanteId; }

    public String getNombreEstudiante() { return nombreEstudiante; }
    public void setNombreEstudiante(String nombreEstudiante) { this.nombreEstudiante = nombreEstudiante; }

    public Integer getMensualidadId() { return mensualidadId; }
    public void setMensualidadId(Integer mensualidadId) { this.mensualidadId = mensualidadId; }

    public Integer getConceptoId() { return conceptoId; }
    public void setConceptoId(Integer conceptoId) { this.conceptoId = conceptoId; }

    public String getConceptoDetalle() { return conceptoDetalle; }
    public void setConceptoDetalle(String conceptoDetalle) { this.conceptoDetalle = conceptoDetalle; }

    public double getMontoAbonado() { return montoAbonado; }
    public void setMontoAbonado(double montoAbonado) { this.montoAbonado = montoAbonado; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getReferenciaPago() { return referenciaPago; }
    public void setReferenciaPago(String referenciaPago) { this.referenciaPago = referenciaPago; }

    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getNombreCajero() { return nombreCajero; }
    public void setNombreCajero(String nombreCajero) { this.nombreCajero = nombreCajero; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }

    public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }

    public boolean isAnulado() {
        return "ANULADO".equalsIgnoreCase(estado);
    }
}
