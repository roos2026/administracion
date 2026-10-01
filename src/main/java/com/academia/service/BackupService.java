package com.academia.service;

import com.academia.config.AppSession;
import com.academia.config.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupService {

    private static final DateTimeFormatter FORMATO_BACKUP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static File generarRespaldo(File carpetaDestino) throws IOException {
        if (!carpetaDestino.exists()) {
            carpetaDestino.mkdirs();
        }

        File dbOrigen = new File(DatabaseManager.getDatabasePath());
        if (!dbOrigen.exists()) {
            throw new IOException("El archivo de base de datos no fue encontrado.");
        }

        String nombreBackup = "academia_backup_" + LocalDateTime.now().format(FORMATO_BACKUP) + ".db";
        File archivoDestino = new File(carpetaDestino, nombreBackup);

        Files.copy(dbOrigen.toPath(), archivoDestino.toPath(), StandardCopyOption.REPLACE_EXISTING);
        AppSession.registrarAuditoria("BACKUP_BD", "sistema", "Respaldo generado con éxito: " + nombreBackup);

        return archivoDestino;
    }
}
