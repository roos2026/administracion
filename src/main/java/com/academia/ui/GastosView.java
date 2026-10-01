package com.academia.ui;

import com.academia.config.AppSession;
import com.academia.dao.GastoDAO;
import com.academia.model.Gasto;
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
import java.util.List;
import java.util.Map;

public class GastosView {

    private final GastoDAO gastoDAO = new GastoDAO();
    private final TableView<Gasto> tabla = new TableView<>();
    private final DatePicker dpDesde = new DatePicker(LocalDate.now().withDayOfMonth(1));
    private final DatePicker dpHasta = new DatePicker(LocalDate.now());

    public VBox crearVista(Stage stage) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        // Encabezado
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label lblTitulo = new Label("Gastos, Compras y Nómina");
        lblTitulo.getStyleClass().add("header-title");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);

        Button btnNuevoGasto = new Button("+ Registrar Gasto / Compra");
        btnNuevoGasto.getStyleClass().add("btn-primary");
        btnNuevoGasto.setOnAction(e -> abrirModalNuevoGasto(stage));

        topBar.getChildren().addAll(lblTitulo, btnNuevoGasto);

        // Barra de Filtros
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        dpDesde.setOnAction(e -> cargarDatos());
        dpHasta.setOnAction(e -> cargarDatos());

        filterBar.getChildren().addAll(new Label("Desde:"), dpDesde, new Label("Hasta:"), dpHasta);

        configurarTabla();

        root.getChildren().addAll(topBar, filterBar, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        cargarDatos();
        return root;
    }

    private void configurarTabla() {
        TableColumn<Gasto, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFechaGasto().toString()));
        colFecha.setPrefWidth(95);

        TableColumn<Gasto, String> colCat = new TableColumn<>("Categoría");
        colCat.setCellValueFactory(new PropertyValueFactory<>("nombreCategoria"));
        colCat.setPrefWidth(140);

        TableColumn<Gasto, String> colConcepto = new TableColumn<>("Concepto");
        colConcepto.setCellValueFactory(new PropertyValueFactory<>("concepto"));
        colConcepto.setPrefWidth(220);

        TableColumn<Gasto, String> colProv = new TableColumn<>("Proveedor / Beneficiario");
        colProv.setCellValueFactory(new PropertyValueFactory<>("proveedor"));
        colProv.setPrefWidth(160);

        TableColumn<Gasto, String> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(cell -> new SimpleStringProperty(String.format("$%.2f", cell.getValue().getMonto())));
        colMonto.setPrefWidth(100);

        TableColumn<Gasto, String> colMetodo = new TableColumn<>("Método");
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));
        colMetodo.setPrefWidth(100);

        TableColumn<Gasto, String> colRecurrente = new TableColumn<>("Recurrente");
        colRecurrente.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isEsRecurrente() ? "Sí" : "No"));
        colRecurrente.setPrefWidth(85);

        TableColumn<Gasto, Void> colComprobante = new TableColumn<>("Comprobante");
        colComprobante.setPrefWidth(120);
        colComprobante.setCellFactory(col -> new TableCell<>() {
            private final Button btnVer = new Button("Ver Archivo");
            {
                btnVer.getStyleClass().add("btn-secondary");
                btnVer.setOnAction(e -> {
                    Gasto g = getTableView().getItems().get(getIndex());
                    if (g.getComprobanteRuta() != null) {
                        try {
                            java.awt.Desktop.getDesktop().open(new File(g.getComprobanteRuta()));
                        } catch (Exception ex) {
                            Alert a = new Alert(Alert.AlertType.ERROR, "No se pudo abrir el comprobante: " + ex.getMessage());
                            a.showAndWait();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Gasto g = getTableView().getItems().get(getIndex());
                    btnVer.setDisable(g.getComprobanteRuta() == null || g.getComprobanteRuta().isBlank());
                    setGraphic(btnVer);
                }
            }
        });

        tabla.getColumns().addAll(colFecha, colCat, colConcepto, colProv, colMonto, colMetodo, colRecurrente, colComprobante);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void cargarDatos() {
        if (dpDesde.getValue() != null && dpHasta.getValue() != null) {
            List<Gasto> gastos = gastoDAO.listarGastos(dpDesde.getValue(), dpHasta.getValue());
            tabla.setItems(FXCollections.observableArrayList(gastos));
        }
    }

    private void abrirModalNuevoGasto(Stage stage) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(stage);
        modal.setTitle("Registrar Nuevo Gasto / Compra / Nómina");

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));

        ComboBox<String> cbCategoria = new ComboBox<>();
        List<Map<String, Object>> categorias = gastoDAO.listarCategorias();
        for (Map<String, Object> cat : categorias) {
            cbCategoria.getItems().add((String) cat.get("nombre"));
        }
        if (!cbCategoria.getItems().isEmpty()) cbCategoria.setValue(cbCategoria.getItems().get(0));

        TextField txtConcepto = new TextField();
        txtConcepto.setPromptText("Ej. Pago renta local mes octubre, Factura luz, Compra de hojas");

        TextField txtProveedor = new TextField();
        txtProveedor.setPromptText("Proveedor o Profesor / Empleado");

        TextField txtMonto = new TextField();
        txtMonto.setPromptText("0.00");

        ComboBox<String> cbMetodo = new ComboBox<>();
        cbMetodo.getItems().addAll("EFECTIVO", "TRANSFERENCIA", "TARJETA", "OTRO");
        cbMetodo.setValue("EFECTIVO");

        DatePicker dpFecha = new DatePicker(LocalDate.now());

        CheckBox chkRecurrente = new CheckBox("Es un gasto recurrente mensual (ej. luz, alquiler)");

        Label lblArchivo = new Label("Ningún comprobante seleccionado.");
        lblArchivo.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        final String[] rutaArchivo = new String[1];

        Button btnAdjuntar = new Button("📎 Adjuntar Foto o PDF de Factura/Recibo");
        btnAdjuntar.getStyleClass().add("btn-secondary");
        btnAdjuntar.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar comprobante");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Imágenes y PDFs", "*.png", "*.jpg", "*.jpeg", "*.pdf")
            );
            File f = fc.showOpenDialog(modal);
            if (f != null) {
                rutaArchivo[0] = f.getAbsolutePath();
                lblArchivo.setText("Adjunto: " + f.getName());
                lblArchivo.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            }
        });

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> modal.close());

        Button btnGuardar = new Button("Guardar Gasto");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setOnAction(e -> {
            try {
                double monto = Double.parseDouble(txtMonto.getText().trim());
                if (txtConcepto.getText().isBlank() || monto <= 0) {
                    mostrarAlerta("Campos Requeridos", "Ingrese el concepto y un monto válido mayor a 0.", Alert.AlertType.WARNING);
                    return;
                }

                int catId = 1;
                for (Map<String, Object> cat : categorias) {
                    if (cbCategoria.getValue().equals(cat.get("nombre"))) {
                        catId = (int) cat.get("id");
                        break;
                    }
                }

                Gasto g = new Gasto();
                g.setCategoriaId(catId);
                g.setConcepto(txtConcepto.getText().trim());
                g.setProveedor(txtProveedor.getText().trim());
                g.setMonto(monto);
                g.setMetodoPago(cbMetodo.getValue());
                g.setFechaGasto(dpFecha.getValue());
                g.setEsRecurrente(chkRecurrente.isSelected());
                g.setComprobanteRuta(rutaArchivo[0]);
                g.setUsuarioId(AppSession.getUsuarioActual() != null ? AppSession.getUsuarioActual().getId() : 1);

                boolean ok = gastoDAO.registrarGasto(g);
                if (ok) {
                    modal.close();
                    cargarDatos();
                } else {
                    mostrarAlerta("Error", "No se pudo guardar el gasto.", Alert.AlertType.ERROR);
                }
            } catch (NumberFormatException ex) {
                mostrarAlerta("Formato", "El monto debe ser numérico.", Alert.AlertType.ERROR);
            }
        });

        botones.getChildren().addAll(btnCancelar, btnGuardar);
        form.getChildren().addAll(
                new Label("Categoría:"), cbCategoria,
                new Label("Concepto del Gasto / Compra:"), txtConcepto,
                new Label("Proveedor / Beneficiario:"), txtProveedor,
                new Label("Monto:"), txtMonto,
                new Label("Método de Pago:"), cbMetodo,
                new Label("Fecha:"), dpFecha,
                chkRecurrente,
                btnAdjuntar, lblArchivo,
                new Separator(), botones
        );

        modal.setScene(new Scene(form, 460, 480));
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
