package com.academia.model;

import java.time.LocalDateTime;

public class CierreCaja {
    private int id;
    private int usuarioId;
    private String nombreUsuario;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private double montoInicialEfectivo;
    private double totalIngresosEfectivo;
    private double totalEgresosEfectivo;
    private double saldoEsperadoEfectivo;
    private double saldoRealContado;
    private double diferencia;
    private String notas;
    private String estado; // ABIERTA, CERRADA

    public CierreCaja() {
        this.estado = "ABIERTA";
        this.fechaApertura = LocalDateTime.now();
    }

    public void recalcularSaldoEsperado() {
        this.saldoEsperadoEfectivo = montoInicialEfectivo + totalIngresosEfectivo - totalEgresosEfectivo;
        this.diferencia = saldoRealContado - saldoEsperadoEfectivo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public LocalDateTime getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDateTime fechaApertura) { this.fechaApertura = fechaApertura; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }

    public double getMontoInicialEfectivo() { return montoInicialEfectivo; }
    public void setMontoInicialEfectivo(double montoInicialEfectivo) { this.montoInicialEfectivo = montoInicialEfectivo; }

    public double getTotalIngresosEfectivo() { return totalIngresosEfectivo; }
    public void setTotalIngresosEfectivo(double totalIngresosEfectivo) { this.totalIngresosEfectivo = totalIngresosEfectivo; }

    public double getTotalEgresosEfectivo() { return totalEgresosEfectivo; }
    public void setTotalEgresosEfectivo(double totalEgresosEfectivo) { this.totalEgresosEfectivo = totalEgresosEfectivo; }

    public double getSaldoEsperadoEfectivo() { return saldoEsperadoEfectivo; }
    public void setSaldoEsperadoEfectivo(double saldoEsperadoEfectivo) { this.saldoEsperadoEfectivo = saldoEsperadoEfectivo; }

    public double getSaldoRealContado() { return saldoRealContado; }
    public void setSaldoRealContado(double saldoRealContado) { this.saldoRealContado = saldoRealContado; }

    public double getDiferencia() { return diferencia; }
    public void setDiferencia(double diferencia) { this.diferencia = diferencia; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
