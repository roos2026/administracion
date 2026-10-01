package com.academia.model;

import java.time.LocalDate;

public class Gasto {
    private int id;
    private int categoriaId;
    private String nombreCategoria;
    private String concepto;
    private String proveedor;
    private double monto;
    private String metodoPago; // EFECTIVO, TRANSFERENCIA, TARJETA, OTRO
    private LocalDate fechaGasto;
    private String comprobanteRuta;
    private boolean esRecurrente;
    private Integer profesorId;
    private String nombreProfesor;
    private int usuarioId;

    public Gasto() {
        this.fechaGasto = LocalDate.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCategoriaId() { return categoriaId; }
    public void setCategoriaId(int categoriaId) { this.categoriaId = categoriaId; }

    public String getNombreCategoria() { return nombreCategoria; }
    public void setNombreCategoria(String nombreCategoria) { this.nombreCategoria = nombreCategoria; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public double getMonto() { return monto; }
    public void setMonto(double monto) { this.monto = monto; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public LocalDate getFechaGasto() { return fechaGasto; }
    public void setFechaGasto(LocalDate fechaGasto) { this.fechaGasto = fechaGasto; }

    public String getComprobanteRuta() { return comprobanteRuta; }
    public void setComprobanteRuta(String comprobanteRuta) { this.comprobanteRuta = comprobanteRuta; }

    public boolean isEsRecurrente() { return esRecurrente; }
    public void setEsRecurrente(boolean esRecurrente) { this.esRecurrente = esRecurrente; }

    public Integer getProfesorId() { return profesorId; }
    public void setProfesorId(Integer profesorId) { this.profesorId = profesorId; }

    public String getNombreProfesor() { return nombreProfesor; }
    public void setNombreProfesor(String nombreProfesor) { this.nombreProfesor = nombreProfesor; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
}
