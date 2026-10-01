package com.academia.ui;

import com.academia.config.AppSession;
import com.academia.dao.MensualidadDAO;
import com.academia.dao.PagoDAO;
import com.academia.model.Mensualidad;
import com.academia.model.PagoAbono;
import com.academia.service.ExcelExportService;
import com.academia.service.PDFGeneratorService;
import com.academia.service.WhatsAppService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class CobranzasView {

    private final MensualidadDAO mensualidadDAO = new MensualidadDAO();
    private final PagoDAO pagoDAO = new PagoDAO();
    private final TableView<Mensualidad> tabla = new TableView<>();
    private final ComboBox<String> cbSemaforo = new ComboBox<>();
    private final TextField txtBusqueda = new TextField();

    public VBox crearVista(Stage stage) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        // Encabezado
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label lblTitulo = new Label("Mensualidades y Cobranzas");
        lblTitulo.getStyleClass().add("header-title");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);

        Button btnGenerarMes = new Button("⚡ Generar Cuotas del Mes");
        btnGenerarMes.getStyleClass().add("btn-secondary");
        btnGenerarMes.setOnAction(e -> abrirModalGenerarCuotas(stage));

        Button btnMorosos = new Button("🚨 Ver Morosos (1 Clic)");
        btnMorosos.getStyleClass().add("btn-danger");
        btnMorosos.setOnAction(e -> {
            cbSemaforo.setValue("VENCIDO");
            cargarDatos();
        });

        Button btnHistorialPagos = new Button("Historial de Recibos");
        btnHistorialPagos.getStyleClass().add("btn-secondary");
        btnHistorialPagos.setOnAction(e -> abrirModalHistorialRecibos(stage));

        topBar.getChildren().addAll(lblTitulo, btnGenerarMes, btnMorosos, btnHistorialPagos);

        // Barra de Filtros y Semáforo
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        txtBusqueda.setPromptText("Buscar estudiante o matrícula...");
        txtBusqueda.setPrefWidth(260);
        txtBusqueda.textProperty().addListener((obs, old, val) -> cargarDatos());

        cbSemaforo.getItems().addAll("TODOS", "PAGADO", "VENCE_PRONTO", "VENCIDO");
        cbSemaforo.setValue("TODOS");
        cbSemaforo.setOnAction(e -> cargarDatos());

        Label lblFiltro = new Label("Semáforo:");
        lblFiltro.getStyleClass().add("form-label");

        // Leyenda visual del semáforo
        Label legVerde = new Label("🟢 Al día / Pagado");
        Label legAmarillo = new Label("🟡 Por Vencer (Antes del 30)");
        Label legRojo = new Label("🔴 Vencido (Llegó el 30 o posterior)");
        legVerde.setStyle("-fx-font-size: 11px; -fx-text-fill: #065f46;");
        legAmarillo.setStyle("-fx-font-size: 11px; -fx-text-fill: #92400e;");
        legRojo.setStyle("-fx-font-size: 11px; -fx-text-fill: #991b1b;");

        HBox leyenda = new HBox(8, legVerde, legAmarillo, legRojo);
        leyenda.setAlignment(Pos.CENTER_LEFT);

        filterBar.getChildren().addAll(new Label("Buscar:"), txtBusqueda, lblFiltro, cbSemaforo, new Separator(), leyenda);

        configurarTabla(stage);

        root.getChildren().addAll(topBar, filterBar, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        cargarDatos();
        return root;
    }

    private void configurarTabla(Stage stage) {
        // Semáforo en las filas de la tabla
        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Mensualidad m, boolean empty) {
                super.updateItem(m, empty);
                getStyleClass().removeAll("row-pagado", "row-vence-pronto", "row-vencido");
                if (m != null && !empty) {
                    switch (m.getEstadoPago()) {
                        case "PAGADO" -> getStyleClass().add("row-pagado");
                        case "VENCE_PRONTO" -> getStyleClass().add("row-vence-pronto");
                        case "VENCIDO" -> getStyleClass().add("row-vencido");
                    }
                }
            }
        });

        TableColumn<Mensualidad, String> colPeriodo = new TableColumn<>("Período");
        colPeriodo.setCellValueFactory(new PropertyValueFactory<>("periodo"));
        colPeriodo.setPrefWidth(85);

        TableColumn<Mensualidad, String> colMatricula = new TableColumn<>("Matrícula");
        colMatricula.setCellValueFactory(new PropertyValueFactory<>("matriculaEstudiante"));
        colMatricula.setPrefWidth(110);

        TableColumn<Mensualidad, String> colEstudiante = new TableColumn<>("Estudiante");
        colEstudiante.setCellValueFactory(new PropertyValueFactory<>("nombreEstudiante"));
        colEstudiante.setPrefWidth(180);

        TableColumn<Mensualidad, String> colVencimiento = new TableColumn<>("Vencimiento");
        colVencimiento.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFechaVencimiento().toString()));
        colVencimiento.setPrefWidth(100);

        TableColumn<Mensualidad, String> colTotal = new TableColumn<>("Total Cuota");
        colTotal.setCellValueFactory(cell -> new SimpleStringProperty(String.format("$%.2f", cell.getValue().getMontoTotal())));
        colTotal.setPrefWidth(100);

        TableColumn<Mensualidad, String> colSaldo = new TableColumn<>("Saldo Pendiente");
        colSaldo.setCellValueFactory(cell -> new SimpleStringProperty(String.format("$%.2f", cell.getValue().getSaldoPendiente())));
        colSaldo.setPrefWidth(110);

        TableColumn<Mensualidad, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoPago"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(switch (item) {
                        case "PAGADO" -> "🟢 PAGADO";
                        case "VENCE_PRONTO" -> "🟡 POR VENCER (Antes del 30)";
                        case "VENCIDO" -> "🔴 VENCIDO (Exigible desde el 30)";
                        default -> item;
                    });
                }
            }
        });
        colEstado.setPrefWidth(125);

        TableColumn<Mensualidad, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(210);
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnCobrar = new Button("Abonar");
            private final Button btnWhatsApp = new Button("WhatsApp");
            private final HBox box = new HBox(6, btnCobrar, btnWhatsApp);

            {
                btnCobrar.getStyleClass().add("btn-primary");
                btnWhatsApp.getStyleClass().add("btn-success");

                btnCobrar.setOnAction(e -> {
                    Mensualidad m = getTableView().getItems().get(getIndex());
                    abrirModalAbono(stage, m);
                });

                btnWhatsApp.setOnAction(e -> {
                    Mensualidad m = getTableView().getItems().get(getIndex());
                    enviarNotificacionWhatsApp(m);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Mensualidad m = getTableView().getItems().get(getIndex());
                    btnCobrar.setDisable(m.getSaldoPendiente() <= 0.001);
                    setGraphic(box);
                }
            }
        });

        tabla.getColumns().addAll(colPeriodo, colMatricula, colEstudiante, colVencimiento, colTotal, colSaldo, colEstado, colAcciones);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void cargarDatos() {
        String semaforo = cbSemaforo.getValue();
        String busqueda = txtBusqueda.getText();
        List<Mensualidad> mensualidades = mensualidadDAO.listar(semaforo, busqueda);
        tabla.setItems(FXCollections.observableArrayList(mensualidades));
    }

    private void abrirModalGenerarCuotas(Stage stage) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(stage);
        modal.setTitle("Generar Cuotas Mensuales Automáticas");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        YearMonth ym = YearMonth.now();
        int diaCorte = Math.min(30, ym.lengthOfMonth());
        TextField txtPeriodo = new TextField(ym.toString());
        DatePicker dpVencimiento = new DatePicker(ym.atDay(diaCorte));

        txtPeriodo.textProperty().addListener((obs, old, val) -> {
            try {
                YearMonth nuevoYm = YearMonth.parse(val.trim());
                int d = Math.min(30, nuevoYm.lengthOfMonth());
                dpVencimiento.setValue(nuevoYm.atDay(d));
            } catch (Exception ignored) {}
        });

        Label lblInfo = new Label("Las cuotas se generarán con fecha límite de pago el día 30. A partir de esa fecha, el semáforo pasará automáticamente a estado VENCIDO si no se ha recibido el pago.");
        lblInfo.setWrapText(true);
        lblInfo.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> modal.close());

        Button btnGenerar = new Button("Generar Ahora");
        btnGenerar.getStyleClass().add("btn-primary");
        btnGenerar.setOnAction(e -> {
            int generadas = mensualidadDAO.generarMensualidadesParaActivos(txtPeriodo.getText().trim(), dpVencimiento.getValue());
            mostrarAlerta("Resultado", "Se generaron " + generadas + " nuevas cuotas para el período " + txtPeriodo.getText(), Alert.AlertType.INFORMATION);
            modal.close();
            cargarDatos();
        });

        botones.getChildren().addAll(btnCancelar, btnGenerar);
        root.getChildren().addAll(new Label("Período (YYYY-MM):"), txtPeriodo, new Label("Fecha de Vencimiento:"), dpVencimiento, lblInfo, botones);

        modal.setScene(new Scene(root, 400, 240));
        modal.showAndWait();
    }

    private void abrirModalAbono(Stage stage, Mensualidad m) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(stage);
        modal.setTitle("Registrar Pago / Abono - " + m.getNombreEstudiante());

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));

        Label lblEst = new Label("Estudiante: " + m.getNombreEstudiante() + " (" + m.getMatriculaEstudiante() + ")");
        lblEst.setStyle("-fx-font-weight: bold;");

        Label lblSaldo = new Label("Saldo Pendiente Actual: $" + String.format("%.2f", m.getSaldoPendiente()));
        lblSaldo.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc2626;");

        TextField txtMonto = new TextField(String.valueOf(m.getSaldoPendiente()));
        txtMonto.setPromptText("Monto a abonar");

        ComboBox<String> cbMetodo = new ComboBox<>();
        cbMetodo.getItems().addAll("EFECTIVO", "TRANSFERENCIA", "TARJETA", "OTRO");
        cbMetodo.setValue("EFECTIVO");

        TextField txtReferencia = new TextField();
        txtReferencia.setPromptText("Número de referencia / transferencia (opcional)");

        CheckBox chkEnviarWhatsApp = new CheckBox("Enviar confirmación por WhatsApp al tutor");
        chkEnviarWhatsApp.setSelected(m.getTelefonoTutor() != null && !m.getTelefonoTutor().isBlank());

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> modal.close());

        Button btnGuardar = new Button("Emitir Recibo y Cobrar");
        btnGuardar.getStyleClass().add("btn-success");
        btnGuardar.setOnAction(e -> {
            try {
                double monto = Double.parseDouble(txtMonto.getText().trim());
                if (monto <= 0 || monto > m.getSaldoPendiente() + 0.01) {
                    mostrarAlerta("Monto inválido", "El monto a abonar debe ser mayor a 0 y no exceder el saldo pendiente.", Alert.AlertType.WARNING);
                    return;
                }

                PagoAbono pago = new PagoAbono();
                pago.setParticipanteId(m.getParticipanteId());
                pago.setNombreEstudiante(m.getNombreEstudiante());
                pago.setMensualidadId(m.getId());
                pago.setConceptoDetalle("Mensualidad Período " + m.getPeriodo());
                pago.setMontoAbonado(monto);
                pago.setMetodoPago(cbMetodo.getValue());
                pago.setReferenciaPago(txtReferencia.getText().trim());
                pago.setUsuarioId(AppSession.getUsuarioActual() != null ? AppSession.getUsuarioActual().getId() : 1);
                pago.setNombreCajero(AppSession.getUsuarioActual() != null ? AppSession.getUsuarioActual().getNombreCompleto() : "Cajero");

                boolean ok = pagoDAO.registrarAbonoPago(pago);
                if (ok) {
                    double saldoRestante = Math.max(0.0, m.getSaldoPendiente() - monto);

                    // Generar PDF del recibo
                    File dirRecibos = new File("datos/recibos");
                    if (!dirRecibos.exists()) dirRecibos.mkdirs();
                    String rutaPdf = new File(dirRecibos, "recibo_" + pago.getNumeroRecibo() + ".pdf").getAbsolutePath();
                    PDFGeneratorService.generarReciboPagoPdf(pago, saldoRestante, rutaPdf);

                    if (chkEnviarWhatsApp.isSelected() && m.getTelefonoTutor() != null) {
                        WhatsAppService.enviarConfirmacionPago(m.getTelefonoTutor(), m.getNombreEstudiante(), pago.getNumeroRecibo(), monto, saldoRestante);
                    }

                    mostrarAlerta("Cobro Exitoso",
                            "Pago registrado con éxito.\nRecibo N°: " + pago.getNumeroRecibo() +
                            "\nPDF guardado en:\n" + rutaPdf,
                            Alert.AlertType.INFORMATION);
                    modal.close();
                    cargarDatos();
                } else {
                    mostrarAlerta("Error", "No se pudo registrar el pago.", Alert.AlertType.ERROR);
                }
            } catch (NumberFormatException ex) {
                mostrarAlerta("Formato", "Ingrese un valor numérico válido para el monto.", Alert.AlertType.ERROR);
            }
        });

        botones.getChildren().addAll(btnCancelar, btnGuardar);
        form.getChildren().addAll(lblEst, lblSaldo, new Label("Monto a pagar:"), txtMonto,
                new Label("Método de Pago:"), cbMetodo, new Label("Referencia:"), txtReferencia,
                chkEnviarWhatsApp, botones);

        modal.setScene(new Scene(form, 440, 340));
        modal.showAndWait();
    }

    private void enviarNotificacionWhatsApp(Mensualidad m) {
        if (m.getTelefonoTutor() == null || m.getTelefonoTutor().isBlank()) {
            mostrarAlerta("Sin WhatsApp", "El participante o tutor no tiene número de teléfono registrado.", Alert.AlertType.WARNING);
            return;
        }

        if ("VENCIDO".equalsIgnoreCase(m.getEstadoPago())) {
            WhatsAppService.enviarAvisoMora(m.getTelefonoTutor(), null, m.getNombreEstudiante(), m.getPeriodo(), m.getSaldoPendiente(), m.getFechaVencimiento());
        } else {
            WhatsAppService.enviarRecordatorioVencimiento(m.getTelefonoTutor(), null, m.getNombreEstudiante(), m.getPeriodo(), m.getSaldoPendiente(), m.getFechaVencimiento());
        }
    }

    private void abrirModalHistorialRecibos(Stage stage) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(stage);
        modal.setTitle("Historial de Recibos Emitidos y Anulaciones");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        TableView<PagoAbono> tablaRecibos = new TableView<>();
        tablaRecibos.setItems(FXCollections.observableArrayList(pagoDAO.listarHistorial(50)));

        TableColumn<PagoAbono, String> colNum = new TableColumn<>("Recibo N°");
        colNum.setCellValueFactory(new PropertyValueFactory<>("numeroRecibo"));

        TableColumn<PagoAbono, String> colEst = new TableColumn<>("Estudiante");
        colEst.setCellValueFactory(new PropertyValueFactory<>("nombreEstudiante"));

        TableColumn<PagoAbono, String> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(cell -> new SimpleStringProperty(String.format("$%.2f", cell.getValue().getMontoAbonado())));

        TableColumn<PagoAbono, String> colMetodo = new TableColumn<>("Método");
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));

        TableColumn<PagoAbono, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        TableColumn<PagoAbono, Void> colAnular = new TableColumn<>("Anulación");
        colAnular.setCellFactory(col -> new TableCell<>() {
            private final Button btnAnular = new Button("Anular Recibo");
            {
                btnAnular.getStyleClass().add("btn-danger");
                btnAnular.setOnAction(e -> {
                    PagoAbono p = getTableView().getItems().get(getIndex());
                    if (p.isAnulado()) {
                        mostrarAlerta("Aviso", "Este recibo ya se encuentra anulado.", Alert.AlertType.INFORMATION);
                        return;
                    }
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Anular Recibo: " + p.getNumeroRecibo());
                    dialog.setHeaderText("Reversión de Cobro");
                    dialog.setContentText("Motivo de anulación (obligatorio por auditoría):");
                    dialog.showAndWait().ifPresent(motivo -> {
                        if (motivo.isBlank()) return;
                        int uid = AppSession.getUsuarioActual() != null ? AppSession.getUsuarioActual().getId() : 1;
                        boolean ok = pagoDAO.anularPago(p.getId(), motivo, uid);
                        if (ok) {
                            mostrarAlerta("Anulado", "El recibo fue anulado y el saldo se restableció.", Alert.AlertType.INFORMATION);
                            modal.close();
                            cargarDatos();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    PagoAbono p = getTableView().getItems().get(getIndex());
                    btnAnular.setDisable(p.isAnulado());
                    setGraphic(btnAnular);
                }
            }
        });

        tablaRecibos.getColumns().addAll(colNum, colEst, colMonto, colMetodo, colEstado, colAnular);
        tablaRecibos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        root.getChildren().addAll(new Label("Últimos 50 pagos registrados:"), tablaRecibos);
        VBox.setVgrow(tablaRecibos, Priority.ALWAYS);

        modal.setScene(new Scene(root, 720, 450));
        modal.showAndWait();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }
}
