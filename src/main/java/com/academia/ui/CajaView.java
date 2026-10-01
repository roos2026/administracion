package com.academia.ui;

import com.academia.config.AppSession;
import com.academia.dao.CajaDAO;
import com.academia.model.CierreCaja;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class CajaView {

    private final CajaDAO cajaDAO = new CajaDAO();
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public VBox crearVista(Stage stage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        Label lblTitulo = new Label("Cierre y Control Diario de Caja (Efectivo)");
        lblTitulo.getStyleClass().add("header-title");

        CierreCaja caja = cajaDAO.obtenerCajaAbierta();

        if (caja == null) {
            // No hay caja abierta: pantalla para abrir turno
            VBox boxApertura = new VBox(16);
            boxApertura.setPadding(new Insets(30));
            boxApertura.setMaxWidth(500);
            boxApertura.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #e2e8f0;");

            Label lblNoCaja = new Label("⚠️ No hay ningún turno de caja abierto en este momento.");
            lblNoCaja.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #b45309;");

            Label lblInst = new Label("Para comenzar a cobrar en efectivo y registrar egresos del día, por favor indique el fondo inicial de cambio en caja:");
            lblInst.setWrapText(true);

            TextField txtFondo = new TextField("0.00");
            txtFondo.setPromptText("Monto inicial de efectivo");

            Button btnAbrir = new Button("Abrir Turno de Caja");
            btnAbrir.getStyleClass().add("btn-primary");
            btnAbrir.setOnAction(e -> {
                try {
                    double fondo = Double.parseDouble(txtFondo.getText().trim());
                    int uid = AppSession.getUsuarioActual() != null ? AppSession.getUsuarioActual().getId() : 1;
                    boolean ok = cajaDAO.abrirCaja(uid, fondo);
                    if (ok) {
                        new Alert(Alert.AlertType.INFORMATION, "Turno de caja iniciado exitosamente.").showAndWait();
                        root.getChildren().clear();
                        root.getChildren().addAll(crearVista(stage).getChildren());
                    }
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR, "Monto numérico no válido.").showAndWait();
                }
            });

            boxApertura.getChildren().addAll(lblNoCaja, lblInst, new Label("Fondo Inicial ($):"), txtFondo, btnAbrir);
            root.getChildren().addAll(lblTitulo, boxApertura);
        } else {
            // Hay caja abierta: mostrar arqueo en vivo y botón de cierre
            VBox boxArqueo = new VBox(18);
            boxArqueo.setPadding(new Insets(24));
            boxArqueo.setMaxWidth(650);
            boxArqueo.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #e2e8f0;");

            Label lblEstado = new Label("🟢 TURNO DE CAJA ACTIVO");
            lblEstado.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #16a34a;");

            Label lblInfo = new Label("Cajero: " + caja.getNombreUsuario() + " | Abierta el: " + caja.getFechaApertura().format(FORMATO));
            lblInfo.setStyle("-fx-text-fill: #64748b;");

            GridPane grid = new GridPane();
            grid.setHgap(20);
            grid.setVgap(12);
            grid.setPadding(new Insets(10, 0, 10, 0));

            grid.add(new Label("Fondo Inicial de Apertura:"), 0, 0);
            Label valInicial = new Label(String.format("$%.2f", caja.getMontoInicialEfectivo()));
            valInicial.setStyle("-fx-font-weight: bold;");
            grid.add(valInicial, 1, 0);

            grid.add(new Label("(+) Total Cobros en Efectivo hoy:"), 0, 1);
            Label valIngresos = new Label(String.format("+$%.2f", caja.getTotalIngresosEfectivo()));
            valIngresos.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a;");
            grid.add(valIngresos, 1, 1);

            grid.add(new Label("(-) Total Gastos en Efectivo hoy:"), 0, 2);
            Label valEgresos = new Label(String.format("-$%.2f", caja.getTotalEgresosEfectivo()));
            valEgresos.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc2626;");
            grid.add(valEgresos, 1, 2);

            grid.add(new Separator(), 0, 3, 2, 1);

            grid.add(new Label("(=) SALDO TEÓRICO QUE DEBE HABER EN CAJA:"), 0, 4);
            Label valTeorico = new Label(String.format("$%.2f", caja.getSaldoEsperadoEfectivo()));
            valTeorico.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #2563eb;");
            grid.add(valTeorico, 1, 4);

            Button btnCerrarCaja = new Button("Realizar Arqueo y Cierre Diario de Caja");
            btnCerrarCaja.getStyleClass().add("btn-danger");
            btnCerrarCaja.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
            btnCerrarCaja.setOnAction(e -> abrirModalCierre(stage, caja, root));

            boxArqueo.getChildren().addAll(lblEstado, lblInfo, new Separator(), grid, btnCerrarCaja);
            root.getChildren().addAll(lblTitulo, boxArqueo);
        }

        return root;
    }

    private void abrirModalCierre(Stage stage, CierreCaja caja, VBox parentRoot) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(stage);
        modal.setTitle("Arqueo Final de Caja - Conteo Físico");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        Label lblInfo = new Label("Saldo Teórico Calculado por el Sistema: $" + String.format("%.2f", caja.getSaldoEsperadoEfectivo()));
        lblInfo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2563eb;");

        Label lblPrompt = new Label("Ingrese la cantidad de dinero físico real contado en el cajón de efectivo:");
        TextField txtContado = new TextField();
        txtContado.setPromptText("0.00");

        Label lblDiferencia = new Label("Diferencia: $0.00");
        lblDiferencia.setStyle("-fx-font-weight: bold;");

        txtContado.textProperty().addListener((obs, old, val) -> {
            try {
                double contado = Double.parseDouble(val.trim());
                double dif = contado - caja.getSaldoEsperadoEfectivo();
                if (Math.abs(dif) < 0.01) {
                    lblDiferencia.setText("Diferencia: $0.00 (Caja Cuadrada Perfecta)");
                    lblDiferencia.setStyle("-fx-font-weight: bold; -fx-text-fill: #16a34a;");
                } else if (dif > 0) {
                    lblDiferencia.setText(String.format("Diferencia: +$%.2f (Sobrante)", dif));
                    lblDiferencia.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb;");
                } else {
                    lblDiferencia.setText(String.format("Diferencia: -$%.2f (Faltante)", Math.abs(dif)));
                    lblDiferencia.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc2626;");
                }
            } catch (Exception ex) {
                lblDiferencia.setText("Diferencia: -");
            }
        });

        TextArea txtNotas = new TextArea();
        txtNotas.setPromptText("Observaciones o notas del arqueo (opcional)");
        txtNotas.setPrefRowCount(2);

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> modal.close());

        Button btnConfirmar = new Button("Confirmar Cierre de Caja");
        btnConfirmar.getStyleClass().add("btn-danger");
        btnConfirmar.setOnAction(e -> {
            try {
                double real = Double.parseDouble(txtContado.getText().trim());
                boolean ok = cajaDAO.cerrarCaja(caja.getId(), real, txtNotas.getText().trim());
                if (ok) {
                    new Alert(Alert.AlertType.INFORMATION, "Cierre de caja completado con éxito.").showAndWait();
                    modal.close();
                    parentRoot.getChildren().clear();
                    parentRoot.getChildren().addAll(crearVista(stage).getChildren());
                }
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Ingrese un monto físico contado válido.").showAndWait();
            }
        });

        botones.getChildren().addAll(btnCancelar, btnConfirmar);
        root.getChildren().addAll(lblInfo, lblPrompt, txtContado, lblDiferencia, new Label("Notas:"), txtNotas, botones);

        modal.setScene(new Scene(root, 440, 320));
        modal.showAndWait();
    }
}
