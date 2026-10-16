package com.academia.ui;

import com.academia.config.AppSession;
import com.academia.dao.NominaDAO;
import com.academia.dao.ProfesorDAO;
import com.academia.dao.UsuarioDAO;
import com.academia.model.Profesor;
import com.academia.model.Usuario;
import com.academia.service.PDFGeneratorService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public class NominaView {

    private final NominaDAO nominaDAO = new NominaDAO();
    private final ProfesorDAO profesorDAO = new ProfesorDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private final TableView<Map<String, Object>> tabla = new TableView<>();
    private final Label lblTotalProfesores = new Label("$0.00");
    private final Label lblTotalSecretaria = new Label("$0.00");
    private final Label lblTotalNomina = new Label("$0.00");

    public VBox crearVista(Stage stage) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        // Barra Superior
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label lblTitulo = new Label("Gestión y Pago de Nómina (Profesores y Secretaría)");
        lblTitulo.getStyleClass().add("header-title");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);

        Button btnNuevoPago = new Button("+ Registrar Pago de Nómina");
        btnNuevoPago.getStyleClass().add("btn-primary");
        btnNuevoPago.setOnAction(e -> abrirModalNuevoPago(stage));

        topBar.getChildren().addAll(lblTitulo, btnNuevoPago);

        Label lblSub = new Label("Registra pagos de honorarios a profesores y sueldos al personal administrativo, con comprobantes PDF oficiales y trazabilidad de caja.");
        lblSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        // Tarjetas Resumen Métricas
        FlowPane flowTarjetas = new FlowPane();
        flowTarjetas.setHgap(16);
        flowTarjetas.setVgap(16);

        flowTarjetas.getChildren().addAll(
                crearTarjeta("Nómina Profesores (Total)", lblTotalProfesores, "#2563eb"),
                crearTarjeta("Nómina Secretaría / Admin", lblTotalSecretaria, "#7c3aed"),
                crearTarjeta("Total Desembolsado Nómina", lblTotalNomina, "#059669")
        );

        configurarTabla();

        root.getChildren().addAll(topBar, lblSub, flowTarjetas, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        cargarDatos();
        return root;
    }

    private VBox crearTarjeta(String titulo, Label lblValor, String colorBorde) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        card.setStyle(card.getStyle() + "; -fx-border-color: transparent transparent transparent " + colorBorde + "; -fx-border-width: 0 0 0 4px;");
        card.setPrefWidth(260);

        Label lblT = new Label(titulo);
        lblT.getStyleClass().add("stat-label");

        lblValor.getStyleClass().add("stat-value");
        lblValor.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        card.getChildren().addAll(lblT, lblValor);
        return card;
    }

    private void configurarTabla() {
        TableColumn<Map<String, Object>, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().get("id"))));
        colId.setPrefWidth(50);

        TableColumn<Map<String, Object>, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().get("fecha"))));
        colFecha.setPrefWidth(95);

        TableColumn<Map<String, Object>, String> colBeneficiario = new TableColumn<>("Beneficiario");
        colBeneficiario.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().get("beneficiario"))));
        colBeneficiario.setPrefWidth(210);

        TableColumn<Map<String, Object>, String> colCategoria = new TableColumn<>("Tipo de Nómina");
        colCategoria.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().get("categoria"))));
        colCategoria.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label(item);
                    if (item.contains("Profesor")) {
                        lbl.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 3 8; -fx-background-radius: 10px; -fx-font-weight: bold;");
                    } else {
                        lbl.setStyle("-fx-background-color: #f3e8ff; -fx-text-fill: #6b21a8; -fx-padding: 3 8; -fx-background-radius: 10px; -fx-font-weight: bold;");
                    }
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });
        colCategoria.setPrefWidth(160);

        TableColumn<Map<String, Object>, String> colConcepto = new TableColumn<>("Concepto");
        colConcepto.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().get("concepto"))));
        colConcepto.setPrefWidth(240);

        TableColumn<Map<String, Object>, String> colMonto = new TableColumn<>("Monto Pagado");
        colMonto.setCellValueFactory(cell -> {
            Object m = cell.getValue().get("monto");
            double val = m instanceof Number ? ((Number) m).doubleValue() : 0.0;
            return new SimpleStringProperty(String.format("$%.2f", val));
        });
        colMonto.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
        colMonto.setPrefWidth(110);

        TableColumn<Map<String, Object>, String> colMetodo = new TableColumn<>("Método");
        colMetodo.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().get("metodo"))));
        colMetodo.setPrefWidth(110);

        TableColumn<Map<String, Object>, String> colUsuario = new TableColumn<>("Registrado Por");
        colUsuario.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().get("registrado_por"))));
        colUsuario.setPrefWidth(140);

        TableColumn<Map<String, Object>, Void> colAccion = new TableColumn<>("Comprobante");
        colAccion.setPrefWidth(150);
        colAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btnPdf = new Button("📄 Ver PDF");

            {
                btnPdf.getStyleClass().add("btn-secondary");
                btnPdf.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
                btnPdf.setOnAction(e -> {
                    Map<String, Object> fila = getTableView().getItems().get(getIndex());
                    Object rutaObj = fila.get("comprobante");
                    if (rutaObj != null && !rutaObj.toString().isBlank()) {
                        File f = new File(rutaObj.toString());
                        if (f.exists()) {
                            try {
                                Desktop.getDesktop().open(f);
                            } catch (Exception ex) {
                                new Alert(Alert.AlertType.ERROR, "No se pudo abrir el archivo PDF: " + ex.getMessage()).showAndWait();
                            }
                        } else {
                            new Alert(Alert.AlertType.WARNING, "El archivo PDF no fue encontrado en: " + f.getAbsolutePath()).showAndWait();
                        }
                    } else {
                        new Alert(Alert.AlertType.INFORMATION, "Este registro no cuenta con un archivo PDF generado asociado.").showAndWait();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnPdf);
                }
            }
        });

        tabla.getColumns().addAll(colId, colFecha, colBeneficiario, colCategoria, colConcepto, colMonto, colMetodo, colUsuario, colAccion);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void cargarDatos() {
        List<Map<String, Object>> lista = nominaDAO.listarPagosNomina();
        tabla.setItems(FXCollections.observableArrayList(lista));

        double sumProf = 0.0;
        double sumSec = 0.0;

        for (Map<String, Object> p : lista) {
            Object mObj = p.get("monto");
            double m = mObj instanceof Number ? ((Number) mObj).doubleValue() : 0.0;
            String cat = String.valueOf(p.get("categoria"));
            if (cat.contains("Profesor")) {
                sumProf += m;
            } else {
                sumSec += m;
            }
        }

        lblTotalProfesores.setText(String.format("$%.2f", sumProf));
        lblTotalSecretaria.setText(String.format("$%.2f", sumSec));
        lblTotalNomina.setText(String.format("$%.2f", (sumProf + sumSec)));
    }

    private void abrirModalNuevoPago(Stage stage) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(stage);
        modal.setTitle("Registrar Pago de Nómina / Honorarios");

        VBox form = new VBox(14);
        form.setPadding(new Insets(20));
        form.setPrefWidth(480);
        form.setStyle("-fx-background-color: white;");

        Label lblTitulo = new Label("Emisión de Pago de Nómina");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        // Tipo de personal
        Label lblTipo = new Label("Seleccione Tipo de Personal:");
        lblTipo.setStyle("-fx-font-weight: bold;");

        ToggleGroup tgTipo = new ToggleGroup();
        RadioButton rbProfesor = new RadioButton("Profesor / Docente");
        RadioButton rbSecretaria = new RadioButton("Secretaria / Personal");
        RadioButton rbOtro = new RadioButton("Otro Personal");
        rbProfesor.setToggleGroup(tgTipo);
        rbSecretaria.setToggleGroup(tgTipo);
        rbOtro.setToggleGroup(tgTipo);
        rbProfesor.setSelected(true);

        HBox boxRbs = new HBox(12, rbProfesor, rbSecretaria, rbOtro);

        // Selector dinámico
        Label lblSelect = new Label("Seleccionar Docente:");
        lblSelect.setStyle("-fx-font-weight: bold;");

        ComboBox<Profesor> cbProfesores = new ComboBox<>();
        cbProfesores.setMaxWidth(Double.MAX_VALUE);
        List<Profesor> listaProfes = profesorDAO.listarActivos();
        cbProfesores.setItems(FXCollections.observableArrayList(listaProfes));
        cbProfesores.setPromptText("Seleccione un profesor...");

        ComboBox<Usuario> cbSecretarias = new ComboBox<>();
        cbSecretarias.setMaxWidth(Double.MAX_VALUE);
        List<Usuario> listaUsuarios = usuarioDAO.listarTodos().stream()
                .filter(u -> "SECRETARIA".equalsIgnoreCase(u.getRol()))
                .toList();
        cbSecretarias.setItems(FXCollections.observableArrayList(listaUsuarios));
        cbSecretarias.setPromptText("Seleccione secretaria...");

        TextField txtBeneficiario = new TextField();
        txtBeneficiario.setPromptText("Nombre completo del beneficiario");

        TextField txtCargo = new TextField("Profesor / Instructor");
        txtCargo.setPromptText("Ej: Docente de Música, Secretaria General...");

        TextField txtConcepto = new TextField("Pago de quincena / honorarios");
        txtConcepto.setPromptText("Concepto del pago");

        TextField txtMonto = new TextField();
        txtMonto.setPromptText("Ej: 5000.00");

        ComboBox<String> cbMetodo = new ComboBox<>();
        cbMetodo.setItems(FXCollections.observableArrayList("EFECTIVO", "TRANSFERENCIA", "TARJETA", "OTRO"));
        cbMetodo.setValue("EFECTIVO");
        cbMetodo.setMaxWidth(Double.MAX_VALUE);

        TextField txtRef = new TextField();
        txtRef.setPromptText("N° comprobante bancario, cheque o referencia");

        DatePicker dpFecha = new DatePicker(LocalDate.now());
        dpFecha.setMaxWidth(Double.MAX_VALUE);

        // Contenedor selector
        VBox boxSelector = new VBox(6, lblSelect, cbProfesores);

        // Eventos al cambiar tipo
        tgTipo.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (rbProfesor.isSelected()) {
                lblSelect.setText("Seleccionar Docente:");
                boxSelector.getChildren().setAll(lblSelect, cbProfesores);
                txtCargo.setText("Profesor / Docente");
                txtBeneficiario.clear();
            } else if (rbSecretaria.isSelected()) {
                lblSelect.setText("Seleccionar Secretaria:");
                boxSelector.getChildren().setAll(lblSelect, cbSecretarias);
                txtCargo.setText("Secretaria Administrativa");
                txtBeneficiario.clear();
            } else {
                lblSelect.setText("Personal Externo:");
                boxSelector.getChildren().clear();
                txtCargo.setText("Personal de Apoyo");
                txtBeneficiario.clear();
            }
        });

        // Evento al seleccionar profesor
        cbProfesores.setOnAction(e -> {
            Profesor p = cbProfesores.getValue();
            if (p != null) {
                txtBeneficiario.setText(p.getNombreCompleto() + (p.getCedula() != null && !p.getCedula().isBlank() ? " (C.I: " + p.getCedula() + ")" : ""));
                if (p.getEspecialidad() != null && !p.getEspecialidad().isBlank()) {
                    txtCargo.setText("Profesor de " + p.getEspecialidad());
                } else {
                    txtCargo.setText("Profesor / Instructor");
                }
                if (p.getSueldoFijo() > 0) {
                    txtMonto.setText(String.format("%.2f", p.getSueldoFijo()));
                    txtConcepto.setText("Sueldo mensual fijo (" + LocalDate.now().getMonth() + ")");
                } else if (p.getPagoPorHora() > 0) {
                    txtConcepto.setText("Honorarios por horas impartidas ($" + p.getPagoPorHora() + "/hr)");
                }
            }
        });

        // Evento al seleccionar secretaria
        cbSecretarias.setOnAction(e -> {
            Usuario u = cbSecretarias.getValue();
            if (u != null) {
                txtBeneficiario.setText(u.getNombreCompleto());
                txtCargo.setText("Secretaria Administrativa");
                txtConcepto.setText("Sueldo quincenal / mensual (" + LocalDate.now().getMonth() + ")");
            }
        });

        // Botones de acción
        Button btnGuardar = new Button("💾 Registrar Pago y Generar Comprobante PDF");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setMaxWidth(Double.MAX_VALUE);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-secondary");
        btnCancelar.setMaxWidth(Double.MAX_VALUE);
        btnCancelar.setOnAction(e -> modal.close());

        btnGuardar.setOnAction(e -> {
            String beneficiario = txtBeneficiario.getText().trim();
            String cargo = txtCargo.getText().trim();
            String concepto = txtConcepto.getText().trim();
            String montoStr = txtMonto.getText().trim();
            LocalDate fecha = dpFecha.getValue() != null ? dpFecha.getValue() : LocalDate.now();
            String metodo = cbMetodo.getValue();
            String ref = txtRef.getText().trim();

            if (beneficiario.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Validación", "Por favor indique o seleccione el beneficiario.");
                return;
            }
            if (concepto.isEmpty()) {
                mostrarAlerta(Alert.AlertType.WARNING, "Validación", "Por favor ingrese el concepto del pago.");
                return;
            }

            double monto;
            try {
                monto = Double.parseDouble(montoStr);
                if (monto <= 0) {
                    mostrarAlerta(Alert.AlertType.WARNING, "Validación", "El monto a pagar debe ser mayor a 0.");
                    return;
                }
            } catch (NumberFormatException ex) {
                mostrarAlerta(Alert.AlertType.WARNING, "Validación", "El monto ingresado no es un número válido.");
                return;
            }

            // Determinar categoría y profesorId
            int categoriaId = 5; // Por defecto: Nómina Administrativa
            Integer profesorId = null;

            if (rbProfesor.isSelected()) {
                categoriaId = 4; // Nómina Profesores
                if (cbProfesores.getValue() != null) {
                    profesorId = cbProfesores.getValue().getId();
                }
            } else if (rbSecretaria.isSelected()) {
                categoriaId = 5; // Nómina Administrativa
            }

            // Generar número de recibo consecutivo
            String nroComprobante = nominaDAO.generarSiguienteNumeroComprobante();

            // Directorio destino
            File dirRecibos = new File("datos/recibos");
            if (!dirRecibos.exists()) {
                dirRecibos.mkdirs();
            }
            String rutaPdf = "datos/recibos/Nomina_" + nroComprobante + ".pdf";

            String pagadoPor = AppSession.getUsuarioActual() != null ? AppSession.getUsuarioActual().getNombreCompleto() : "Administración";
            int usuarioId = AppSession.getUsuarioActual() != null ? AppSession.getUsuarioActual().getId() : 1;

            // 1. Generar Comprobante PDF oficial
            boolean pdfOk = PDFGeneratorService.generarComprobanteNominaPdf(
                    nroComprobante, beneficiario, cargo, concepto, monto, metodo, ref, fecha, pagadoPor, rutaPdf
            );

            // 2. Guardar en Base de Datos
            boolean guardadoOk = nominaDAO.registrarPagoNomina(
                    categoriaId, profesorId, beneficiario, concepto, monto, metodo, fecha, rutaPdf, usuarioId
            );

            if (guardadoOk) {
                modal.close();
                cargarDatos();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Pago Registrado");
                alert.setHeaderText("¡Pago de nómina guardado correctamente!");
                alert.setContentText("Comprobante: " + nroComprobante + "\nMonto: $" + String.format("%.2f", monto) +
                        "\nBeneficiario: " + beneficiario +
                        "\n\n¿Deseas abrir el Comprobante Oficial en PDF para imprimirlo o enviarlo?");

                ButtonType btnAbrir = new ButtonType("Abrir PDF");
                ButtonType btnCerrar = new ButtonType("Aceptar", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(btnAbrir, btnCerrar);

                alert.showAndWait().ifPresent(tipo -> {
                    if (tipo == btnAbrir) {
                        try {
                            Desktop.getDesktop().open(new File(rutaPdf));
                        } catch (Exception ex) {
                            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir el archivo PDF: " + ex.getMessage());
                        }
                    }
                });
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Error", "Ocurrió un error al guardar el pago en la base de datos.");
            }
        });

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        VBox vFields = new VBox(10);
        vFields.getChildren().addAll(
                lblTipo, boxRbs,
                boxSelector,
                new Label("Beneficiario (Nombre):"), txtBeneficiario,
                new Label("Cargo / Rol:"), txtCargo,
                new Label("Concepto de Pago:"), txtConcepto,
                new Label("Monto a Pagar ($):"), txtMonto,
                new Label("Método de Pago:"), cbMetodo,
                new Label("Referencia / N° Cheque:"), txtRef,
                new Label("Fecha de Pago:"), dpFecha,
                new Separator(),
                btnGuardar, btnCancelar
        );
        sp.setContent(vFields);
        sp.setStyle("-fx-background-color: transparent;");

        form.getChildren().addAll(lblTitulo, sp);

        Scene scene = new Scene(form, 1024, 700);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ignored) {}

        modal.setScene(scene);
        modal.setMaximized(true);
        modal.showAndWait();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo, mensaje);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
