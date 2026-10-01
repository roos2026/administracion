package com.academia.ui;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;

public class FirmaDigitalDialog {

    private String rutaArchivoFirma = null;
    private boolean firmaRealizada = false;

    public static String capturarFirma(Stage parentStage, String identificador) {
        FirmaDigitalDialog dialog = new FirmaDigitalDialog();
        dialog.mostrar(parentStage, identificador);
        return dialog.rutaArchivoFirma;
    }

    private void mostrar(Stage parentStage, String identificador) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentStage);
        stage.setTitle("Firma Digital en Pantalla - Contrato");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #ffffff;");

        Label lblInstruccion = new Label("Por favor, el tutor o representante debe firmar con el mouse, lápiz o dedo en el recuadro:");
        lblInstruccion.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155; -fx-font-size: 13px;");

        // Lienzo Canvas
        Canvas canvas = new Canvas(480, 180);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        limpiarCanvas(gc, canvas.getWidth(), canvas.getHeight());

        // Eventos de dibujo a mano alzada
        canvas.setOnMousePressed(e -> {
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
            firmaRealizada = true;
        });

        canvas.setOnMouseDragged(e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
        });

        // Contenedor con borde para el canvas
        VBox canvasContainer = new VBox(canvas);
        canvasContainer.setStyle("-fx-border-color: #94a3b8; -fx-border-width: 2px; -fx-border-style: dashed; -fx-background-color: #fafafa;");

        // Botones
        HBox botones = new HBox(12);
        botones.setAlignment(Pos.CENTER_RIGHT);

        Button btnLimpiar = new Button("Limpiar");
        btnLimpiar.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-padding: 8 16;");
        btnLimpiar.setOnAction(e -> {
            limpiarCanvas(gc, canvas.getWidth(), canvas.getHeight());
            firmaRealizada = false;
        });

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-padding: 8 16;");
        btnCancelar.setOnAction(e -> {
            rutaArchivoFirma = null;
            stage.close();
        });

        Button btnAceptar = new Button("Aceptar y Guardar Firma");
        btnAceptar.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 18;");
        btnAceptar.setOnAction(e -> {
            if (!firmaRealizada) {
                lblInstruccion.setText("⚠️ Debe realizar la firma antes de continuar:");
                lblInstruccion.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                return;
            }

            try {
                WritableImage image = canvas.snapshot(null, null);
                File dir = new File("datos/firmas");
                if (!dir.exists()) dir.mkdirs();

                File archivoFirma = new File(dir, "firma_" + identificador + "_" + System.currentTimeMillis() + ".png");
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", archivoFirma);

                rutaArchivoFirma = archivoFirma.getAbsolutePath();
                stage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        botones.getChildren().addAll(btnLimpiar, btnCancelar, btnAceptar);
        root.getChildren().addAll(lblInstruccion, canvasContainer, botones);

        Scene scene = new Scene(root, 520, 310);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.showAndWait();
    }

    private void limpiarCanvas(GraphicsContext gc, double width, double height) {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        gc.setStroke(Color.rgb(15, 23, 42)); // Tinta oscura
        gc.setLineWidth(2.5);
    }
}
