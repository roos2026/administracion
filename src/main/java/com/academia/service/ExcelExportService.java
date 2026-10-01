package com.academia.service;

import com.academia.model.Mensualidad;
import com.academia.model.Participante;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.util.List;

public class ExcelExportService {

    public static boolean exportarEstudiantes(List<Participante> participantes, String rutaDestinoXlsx) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Estudiantes");

            // Estilo Encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] columnas = {"Matrícula", "Nombres", "Apellidos", "Edad", "Estado", "Grupo", "Tutor", "Tel. Tutor / WhatsApp", "Alergias / Condiciones"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Participante p : participantes) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getMatricula());
                row.createCell(1).setCellValue(p.getNombres());
                row.createCell(2).setCellValue(p.getApellidos());
                row.createCell(3).setCellValue(p.getEdad());
                row.createCell(4).setCellValue(p.getEstado());
                row.createCell(5).setCellValue(p.getNombreGrupo() != null ? p.getNombreGrupo() : "Sin asignar");
                row.createCell(6).setCellValue(p.getTutor() != null ? p.getTutor().getNombreCompleto() : "N/A");
                row.createCell(7).setCellValue(p.getTutor() != null ? p.getTutor().getTelefonoWhatsapp() : "");
                row.createCell(8).setCellValue(p.getAlergiasCondiciones() != null ? p.getAlergiasCondiciones() : "");
            }

            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(rutaDestinoXlsx)) {
                workbook.write(fos);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean exportarMorosos(List<Mensualidad> morosos, String rutaDestinoXlsx) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte Morosidad");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] columnas = {"Matrícula", "Estudiante", "Período", "Vencimiento", "Monto Base", "Recargo Mora", "Total", "Saldo Pendiente", "WhatsApp Tutor"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Mensualidad m : morosos) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(m.getMatriculaEstudiante());
                row.createCell(1).setCellValue(m.getNombreEstudiante());
                row.createCell(2).setCellValue(m.getPeriodo());
                row.createCell(3).setCellValue(m.getFechaVencimiento().toString());
                row.createCell(4).setCellValue(m.getMontoBase());
                row.createCell(5).setCellValue(m.getRecargoMora());
                row.createCell(6).setCellValue(m.getMontoTotal());
                row.createCell(7).setCellValue(m.getSaldoPendiente());
                row.createCell(8).setCellValue(m.getTelefonoTutor() != null ? m.getTelefonoTutor() : "");
            }

            for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(rutaDestinoXlsx)) {
                workbook.write(fos);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
