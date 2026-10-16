package com.academia.service;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Servicio encargado de calcular las fechas de vencimiento de las cuotas
 * según las reglas de negocio de la academia:
 * - Inscritos del 1 al 8: pagan el 30 del mismo mes.
 * - Inscritos del 8 al 15: pagan el 15 del mes siguiente.
 * - Inscritos del 15 en adelante: pagan el 30 del mes siguiente.
 */
public class BillingCycleService {

    public static class VencimientoInfo {
        private final LocalDate fechaVencimiento;
        private final String periodo; // YYYY-MM
        private final int diaCorteHabitual; // 15 o 30

        public VencimientoInfo(LocalDate fechaVencimiento, String periodo, int diaCorteHabitual) {
            this.fechaVencimiento = fechaVencimiento;
            this.periodo = periodo;
            this.diaCorteHabitual = diaCorteHabitual;
        }

        public LocalDate getFechaVencimiento() { return fechaVencimiento; }
        public String getPeriodo() { return periodo; }
        public int getDiaCorteHabitual() { return diaCorteHabitual; }
    }

    /**
     * Aplica la fórmula exacta según el día del mes de la inscripción:
     */
    public static VencimientoInfo calcularPrimerVencimiento(LocalDate fechaInscripcion) {
        if (fechaInscripcion == null) fechaInscripcion = LocalDate.now();

        int dia = fechaInscripcion.getDayOfMonth();
        LocalDate vencimiento;
        int diaCorteHabitual;

        if (dia >= 1 && dia <= 8) {
            // Regla 1: Del 1 al 8 -> se les cobra el 30 de ese mismo mes
            int maxDia = Math.min(30, fechaInscripcion.lengthOfMonth());
            vencimiento = fechaInscripcion.withDayOfMonth(maxDia);
            diaCorteHabitual = 30;
        } else if (dia > 8 && dia <= 15) {
            // Regla 2: Del 8 al 15 -> se les cobra el 15 del mes siguiente
            LocalDate mesSiguiente = fechaInscripcion.plusMonths(1);
            vencimiento = mesSiguiente.withDayOfMonth(15);
            diaCorteHabitual = 15;
        } else {
            // Regla 3: Del 15 en adelante -> se les cobra el 30 del mes siguiente
            LocalDate mesSiguiente = fechaInscripcion.plusMonths(1);
            int maxDia = Math.min(30, mesSiguiente.lengthOfMonth());
            vencimiento = mesSiguiente.withDayOfMonth(maxDia);
            diaCorteHabitual = 30;
        }

        String periodo = YearMonth.from(vencimiento).toString();
        return new VencimientoInfo(vencimiento, periodo, diaCorteHabitual);
    }

    /**
     * Calcula el vencimiento para un mes específico según el día habitual asignado (15 o 30)
     */
    public static LocalDate calcularVencimientoParaMes(YearMonth periodo, int diaHabitual) {
        if (diaHabitual == 15) {
            return periodo.atDay(15);
        } else {
            int maxDia = Math.min(30, periodo.lengthOfMonth());
            return periodo.atDay(maxDia);
        }
    }
}
