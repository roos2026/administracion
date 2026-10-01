package com.academia.service;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class WhatsAppService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Limpia el número telefónico y agrega el código de país si es necesario.
     */
    public static String normalizarTelefono(String tel) {
        if (tel == null) return "";
        return tel.replaceAll("[^0-9]", "");
    }

    /**
     * Abre WhatsApp Web o WhatsApp Desktop con el mensaje pre-cargado con 1 clic.
     */
    public static boolean abrirChat(String telefono, String mensaje) {
        String num = normalizarTelefono(telefono);
        if (num.isBlank()) return false;

        try {
            String encodedMsg = URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
            String url = "https://wa.me/" + num + "?text=" + encodedMsg;
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return true;
            }
        } catch (Exception e) {
            System.err.println("No se pudo abrir WhatsApp: " + e.getMessage());
        }
        return false;
    }

    public static boolean enviarRecordatorioVencimiento(String telefono, String tutor, String estudiante,
                                                        String periodo, double saldo, LocalDate vencimiento) {
        String mensaje = String.format(
                "Estimado/a %s,\n\nLe recordamos cordialmente de parte de la Academia que la mensualidad del período %s de su representado/a *%s* vence el *%s*.\n\n" +
                "Monto a pagar: *$%.2f*\n\n" +
                "Agradecemos su puntualidad. ¡Que tenga un excelente día!",
                tutor != null ? tutor : "representante",
                periodo, estudiante, vencimiento.format(FORMATTER), saldo
        );
        return abrirChat(telefono, mensaje);
    }

    public static boolean enviarAvisoMora(String telefono, String tutor, String estudiante,
                                          String periodo, double saldo, LocalDate vencimiento) {
        String mensaje = String.format(
                "Estimado/a %s,\n\nNos comunicamos de la Academia para recordarle que la cuota de *%s* correspondiente a *%s* se encuentra vencida desde el *%s*.\n\n" +
                "Saldo pendiente actual: *$%.2f*\n\n" +
                "Por favor comuníquese con secretaría para coordinar su pago o reportar su comprobante. ¡Muchas gracias!",
                tutor != null ? tutor : "representante",
                estudiante, periodo, vencimiento.format(FORMATTER), saldo
        );
        return abrirChat(telefono, mensaje);
    }

    public static boolean enviarConfirmacionPago(String telefono, String estudiante, String numeroRecibo,
                                                 double montoPagado, double saldoRestante) {
        String mensaje = String.format(
                "¡Pago recibido con éxito!\n\n" +
                "Hemos registrado su abono para el estudiante *%s*.\n" +
                "• Recibo N°: *%s*\n" +
                "• Monto recibido: *$%.2f*\n" +
                "• Saldo restante: *$%.2f*\n\n" +
                "¡Gracias por su confianza!",
                estudiante, numeroRecibo, montoPagado, saldoRestante
        );
        return abrirChat(telefono, mensaje);
    }
}
