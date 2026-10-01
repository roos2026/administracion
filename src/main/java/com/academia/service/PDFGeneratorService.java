package com.academia.service;

import com.academia.model.CierreCaja;
import com.academia.model.PagoAbono;
import com.academia.model.Participante;
import com.academia.model.Tutor;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PDFGeneratorService {

    private static final Font FONT_TITULO = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 41, 59));
    private static final Font FONT_SUBTITULO = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(71, 85, 105));
    private static final Font FONT_CUERPO = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(51, 65, 85));
    private static final Font FONT_CUERPO_BOLD = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(15, 23, 42));
    private static final Font FONT_MINI = new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(100, 116, 139));

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Genera el Contrato de Inscripción con los términos aceptados y la firma digital incrustada.
     */
    public static boolean generarContratoPdf(Participante p, Tutor t, String rutaFirmaPng, String rutaDestinoPdf) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(rutaDestinoPdf));
            document.open();

            // Encabezado
            Paragraph header = new Paragraph("ACADEMIA EDUCATIVA INTEGRAL", FONT_TITULO);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph sub = new Paragraph("CONTRATO DE PRESTACIÓN DE SERVICIOS EDUCATIVOS - TÉRMINOS Y CONDICIONES\n\n", FONT_SUBTITULO);
            sub.setAlignment(Element.ALIGN_CENTER);
            document.add(sub);
 
            // Datos del Estudiante y Tutor
            PdfPTable tablaDatos = new PdfPTable(2);
            tablaDatos.setWidthPercentage(100);
            tablaDatos.setSpacingBefore(10f);
            tablaDatos.setSpacingAfter(15f);

            agregarFila(tablaDatos, "Matrícula Única:", p.getMatricula());
            agregarFila(tablaDatos, "Estudiante:", p.getNombreCompleto());
            agregarFila(tablaDatos, "Fecha de Nacimiento:", p.getFechaNacimiento().toString() + " (" + p.getEdad() + " años)");
            agregarFila(tablaDatos, "Condiciones / Alergias:", p.getAlergiasCondiciones() != null ? p.getAlergiasCondiciones() : "Ninguna reportada");

            if (t != null) {
                agregarFila(tablaDatos, "Padre / Madre / Tutor:", t.getNombreCompleto() + " (" + t.getParentesco() + ")");
                agregarFila(tablaDatos, "Cédula / Documento:", t.getCedula());
                agregarFila(tablaDatos, "Teléfono / WhatsApp:", t.getTelefonoWhatsapp());
                agregarFila(tablaDatos, "Dirección:", t.getDireccion() != null ? t.getDireccion() : "No especificada");
            }
            document.add(tablaDatos);

            // Cláusulas del contrato
            Paragraph clausulas = new Paragraph("""
                CLÁUSULA PRIMERA (OBJETO): Esta otorgando el permiso de realizar fotos o videos al participante y dando el permiso de publicar imágenes o videos en las redes sociales de la Academia.
                
                CLÁUSULA SEGUNDA (COMPROMISO DE PAGO): El contratante se compromete a abonar puntualmente las mensualidades los días 30 de cada mes. A partir del día 30 la cuota se considera vencida y exigible, causando los recargos estipulados por administración en caso de mora.
                
                CLÁUSULA TERCERA (BAJAS Y SUSPENSIONES): Cualquier solicitud de baja o pausa deberá notificarse formalmente antes del inicio del siguiente período mensual para suspender la emisión de cuotas.
                
                CLÁUSULA CUARTA (NORMAS Y ASISTENCIA): El estudiante deberá respetar el reglamento de convivencia, cuidado de instrumentos e instalaciones, manteniendo una asistencia regular.
                \n
                """, FONT_CUERPO);
            document.add(clausulas);

            // Firma digital
            if (rutaFirmaPng != null && !rutaFirmaPng.isBlank()) {
                Paragraph cert = new Paragraph("CONSTANCIA DE FIRMA ELECTRÓNICA Y ACEPTACIÓN EN PANTALLA", FONT_SUBTITULO);
                cert.setAlignment(Element.ALIGN_CENTER);
                document.add(cert);

                Image imgFirma = Image.getInstance(rutaFirmaPng);
                imgFirma.scaleToFit(200, 80);
                imgFirma.setAlignment(Element.ALIGN_CENTER);
                document.add(imgFirma);

                String firmante = (t != null && t.getNombreCompleto() != null) ? t.getNombreCompleto() : p.getNombreCompleto();
                String cedula = (t != null && t.getCedula() != null) ? t.getCedula() : "N/A";
                Paragraph pieFirma = new Paragraph("Firmado digitalmente por: " + firmante + " | Cédula: " + cedula +
                        "\nFecha y Hora de Certificación: " + LocalDateTime.now().format(FORMATO_FECHA) +
                        "\nVersión de Términos: v1.0-2026", FONT_MINI);
                pieFirma.setAlignment(Element.ALIGN_CENTER);
                document.add(pieFirma);
            }

            document.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Genera el Comprobante / Recibo Oficial de Pago con Folio Consecutivo.
     */
    public static boolean generarReciboPagoPdf(PagoAbono pago, double saldoRestante, String rutaDestinoPdf) {
        Document document = new Document(PageSize.A5, 30, 30, 30, 30);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(rutaDestinoPdf));
            document.open();

            // Encabezado
            Paragraph titulo = new Paragraph("ACADEMIA EDUCATIVA", FONT_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            Paragraph reciboNum = new Paragraph("RECIBO DE PAGO OFICIAL: " + pago.getNumeroRecibo(), FONT_SUBTITULO);
            reciboNum.setAlignment(Element.ALIGN_CENTER);
            document.add(reciboNum);

            Paragraph fecha = new Paragraph("Fecha: " + pago.getFechaPago().format(FORMATO_FECHA) + "\n\n", FONT_MINI);
            fecha.setAlignment(Element.ALIGN_CENTER);
            document.add(fecha);

            // Tabla de detalle del recibo
            PdfPTable tabla = new PdfPTable(2);
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(10f);
            tabla.setSpacingAfter(15f);

            agregarFila(tabla, "Estudiante:", pago.getNombreEstudiante());
            agregarFila(tabla, "Concepto:", pago.getConceptoDetalle());
            agregarFila(tabla, "Método de Pago:", pago.getMetodoPago() + (pago.getReferenciaPago() != null ? " (Ref: " + pago.getReferenciaPago() + ")" : ""));
            agregarFila(tabla, "Atendido por (Cajero):", pago.getNombreCajero());
            agregarFila(tabla, "MONTO ABONADO:", String.format("$%.2f", pago.getMontoAbonado()));
            agregarFila(tabla, "Saldo Pendiente Restante:", String.format("$%.2f", saldoRestante));

            document.add(tabla);

            Paragraph pie = new Paragraph("\n* Este recibo constituye constancia de pago válida ante la administración.\nConserve este comprobante para cualquier aclaración.", FONT_MINI);
            pie.setAlignment(Element.ALIGN_CENTER);
            document.add(pie);

            document.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void agregarFila(PdfPTable tabla, String clave, String valor) {
        PdfPCell c1 = new PdfPCell(new Phrase(clave, FONT_CUERPO_BOLD));
        c1.setBackgroundColor(new Color(241, 245, 249));
        c1.setPadding(6f);

        PdfPCell c2 = new PdfPCell(new Phrase(valor != null ? valor : "", FONT_CUERPO));
        c2.setPadding(6f);

        tabla.addCell(c1);
        tabla.addCell(c2);
    }
}
