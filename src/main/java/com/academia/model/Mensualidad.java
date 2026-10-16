package com.academia.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Mensualidad {
    private int id;
    private int participanteId;
    private String matriculaEstudiante;
    private String nombreEstudiante;
    private String telefonoTutor;
    private String periodo; // YYYY-MM
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private double montoBase;
    private double descuento;
    private double recargoMora;
    private double montoTotal;
    private double saldoPendiente;
    private String estadoPago; // PAGADO, VENCE_PRONTO, VENCIDO
    private String estadoEstudiante; // ACTIVO, PAUSA, INACTIVO

    public Mensualidad() {}

    /**
     * Determina o actualiza el semáforo de 3 colores:
     * - PAGADO (Verde): saldo pendiente es 0 o menor.
     * - VENCE_PRONTO (Amarillo): antes del día 30 (aún no llega la fecha límite).
     * - VENCIDO (Rojo): llegó el día 30 o posterior y hay saldo pendiente (pago exigible).
     */
    public String calcularEstadoSemaforo() {
        if (saldoPendiente <= 0.001) {
            return "PAGADO";
        }
        LocalDate hoy = LocalDate.now();
        if (fechaVencimiento != null) {
            // Llegó el día 30 o lo superó: el pago ya es obligatorio/vencido
            if (!hoy.isBefore(fechaVencimiento)) {
                return "VENCIDO";
            }
        }
        return "VENCE_PRONTO";
    }

    public long getDiasParaVencer() {
        if (fechaVencimiento == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getParticipanteId() { return participanteId; }
    public void setParticipanteId(int participanteId) { this.participanteId = participanteId; }

    public String getMatriculaEstudiante() { return matriculaEstudiante; }
    public void setMatriculaEstudiante(String matriculaEstudiante) { this.matriculaEstudiante = matriculaEstudiante; }

    public String getNombreEstudiante() { return nombreEstudiante; }
    public void setNombreEstudiante(String nombreEstudiante) { this.nombreEstudiante = nombreEstudiante; }

    public String getTelefonoTutor() { return telefonoTutor; }
    public void setTelefonoTutor(String telefonoTutor) { this.telefonoTutor = telefonoTutor; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public double getMontoBase() { return montoBase; }
    public void setMontoBase(double montoBase) { this.montoBase = montoBase; }

    public double getDescuento() { return descuento; }
    public void setDescuento(double descuento) { this.descuento = descuento; }

    public double getRecargoMora() { return recargoMora; }
    public void setRecargoMora(double recargoMora) { this.recargoMora = recargoMora; }

    public double getMontoTotal() { return montoTotal; }
    public void setMontoTotal(double montoTotal) { this.montoTotal = montoTotal; }

    public double getSaldoPendiente() { return saldoPendiente; }
    public void setSaldoPendiente(double saldoPendiente) { this.saldoPendiente = saldoPendiente; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public String getEstadoEstudiante() { return estadoEstudiante; }
    public void setEstadoEstudiante(String estadoEstudiante) { this.estadoEstudiante = estadoEstudiante; }
}
