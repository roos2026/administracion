package com.academia.ui;


import com.academia.dao.GrupoDAO;
import com.academia.dao.ParticipanteDAO;
import com.academia.model.Grupo;
import com.academia.model.Participante;
import com.academia.model.Tutor;
import com.academia.service.ExcelExportService;
import com.academia.service.PDFGeneratorService;
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
import java.time.Period;
import java.util.List;

public class ParticipantesView {

    private final ParticipanteDAO participanteDAO = new ParticipanteDAO();
    private final GrupoDAO grupoDAO = new GrupoDAO();
    private final TableView<Participante> tabla = new TableView<>();
    private final ComboBox<String> cbFiltroEstado = new ComboBox<>();
    private final TextField txtBusqueda = new TextField();

    public VBox crearVista(Stage stage) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        // Barra Superior: Título y Botón Nuevo
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label lblTitulo = new Label("Gestión de Participantes");
        lblTitulo.getStyleClass().add("header-title");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);

        Button btnNuevo = new Button("+ Inscribir Participante");
        btnNuevo.getStyleClass().add("btn-primary");
        btnNuevo.setOnAction(e -> abrirModalNuevoParticipante(stage));

        Button btnExportar = new Button("Exportar a Excel");
        btnExportar.getStyleClass().add("btn-secondary");
        btnExportar.setOnAction(e -> exportarExcel(stage));

        topBar.getChildren().addAll(lblTitulo, btnExportar, btnNuevo);

        // Barra de Filtros
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        txtBusqueda.setPromptText("Buscar por nombre, matrícula o tutor...");
        txtBusqueda.setPrefWidth(280);
        txtBusqueda.textProperty().addListener((obs, oldVal, newVal) -> cargarDatos());

        cbFiltroEstado.getItems().addAll("TODOS", "ACTIVO", "PAUSA", "INACTIVO");
        cbFiltroEstado.setValue("ACTIVO");
        cbFiltroEstado.setOnAction(e -> cargarDatos());

        Label lblFiltrar = new Label("Estado:");
        lblFiltrar.getStyleClass().add("form-label");

        filterBar.getChildren().addAll(new Label("Buscar:"), txtBusqueda, lblFiltrar, cbFiltroEstado);

        // Configuración de Tabla
        configurarTabla(stage);

        root.getChildren().addAll(topBar, filterBar, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        cargarDatos();
        return root;
    }

    private void configurarTabla(Stage stage) {
        TableColumn<Participante, String> colMatricula = new TableColumn<>("Matrícula");
        colMatricula.setCellValueFactory(new PropertyValueFactory<>("matricula"));
        colMatricula.setPrefWidth(120);

        TableColumn<Participante, String> colNombre = new TableColumn<>("Nombre Completo");
        colNombre.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNombreCompleto()));
        colNombre.setPrefWidth(200);

        TableColumn<Participante, String> colEdad = new TableColumn<>("Edad");
        colEdad.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEdad() + " años"));
        colEdad.setPrefWidth(80);

        TableColumn<Participante, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.getStyleClass().add(switch (item) {
                        case "ACTIVO" -> "badge-activo";
                        case "PAUSA" -> "badge-pausa";
                        default -> "badge-inactivo";
                    });
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });
        colEstado.setPrefWidth(90);

        TableColumn<Participante, String> colGrupo = new TableColumn<>("Grupo");
        colGrupo.setCellValueFactory(new PropertyValueFactory<>("nombreGrupo"));
        colGrupo.setPrefWidth(140);

        TableColumn<Participante, String> colTutor = new TableColumn<>("Tutor / WhatsApp");
        colTutor.setCellValueFactory(cellData -> {
            Tutor t = cellData.getValue().getTutor();
            return new SimpleStringProperty(t != null ? t.getNombreCompleto() + " (" + t.getTelefonoWhatsapp() + ")" : "Propio");
        });
        colTutor.setPrefWidth(210);

        TableColumn<Participante, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(190);
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEstado = new Button("Cambiar Estado");
            private final Button btnVerFicha = new Button("Ficha");
            private final HBox panel = new HBox(6, btnVerFicha, btnEstado);

            {
                btnVerFicha.getStyleClass().add("btn-secondary");
                btnEstado.getStyleClass().add("btn-secondary");

                btnEstado.setOnAction(e -> {
                    Participante p = getTableView().getItems().get(getIndex());
                    abrirModalCambiarEstado(stage, p);
                });

                btnVerFicha.setOnAction(e -> {
                    Participante p = getTableView().getItems().get(getIndex());
                    mostrarFichaEstudiante(stage, p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : panel);
            }
        });

        tabla.getColumns().addAll(java.util.List.of(colMatricula, colNombre, colEdad, colEstado, colGrupo, colTutor, colAcciones));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void cargarDatos() {
        String estado = cbFiltroEstado.getValue();
        String busqueda = txtBusqueda.getText();
        List<Participante> participantes = participanteDAO.listar(estado, busqueda);
        tabla.setItems(FXCollections.observableArrayList(participantes));
    }

    private void abrirModalNuevoParticipante(Stage stage) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(stage);
        modal.setTitle("Inscripción de Participante y Firma de Contrato");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);

        VBox form = new VBox(14);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: #ffffff;");

        // 1. Datos del Participante
        Label lblSec1 = new Label("1. Datos del Estudiante");
        lblSec1.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");

        TextField txtMatricula = new TextField(participanteDAO.generarSiguienteMatricula());
        txtMatricula.setEditable(false);
        txtMatricula.setStyle("-fx-background-color: #f1f5f9;");

        TextField txtNombres = new TextField();
        txtNombres.setPromptText("Nombres");

        TextField txtApellidos = new TextField();
        txtApellidos.setPromptText("Apellidos");

        DatePicker dpNacimiento = new DatePicker(LocalDate.now().minusYears(10));
        Label lblEdadCalculada = new Label("Edad: 10 años (Menor de edad)");
        lblEdadCalculada.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563eb;");

        TextField txtEmergenciaNombre = new TextField();
        txtEmergenciaNombre.setPromptText("Nombre contacto emergencia");

        TextField txtEmergenciaTel = new TextField();
        txtEmergenciaTel.setPromptText("Teléfono emergencia");

        TextField txtAlergias = new TextField();
        txtAlergias.setPromptText("Alergias, asma o condiciones médicas especiales");

        // Grupos
        ComboBox<Grupo> cbGrupos = new ComboBox<>();
        cbGrupos.getItems().addAll(grupoDAO.listarGruposActivos());
        cbGrupos.setPromptText("Seleccionar Grupo");
        cbGrupos.setMaxWidth(Double.MAX_VALUE);

        Button btnCrearGrupoRapido = new Button("+ Nuevo Grupo");
        btnCrearGrupoRapido.getStyleClass().add("btn-secondary");
        btnCrearGrupoRapido.setOnAction(e -> {
            GruposView gv = new GruposView();
            // Abrir modal de creación de grupo
            Stage st = (Stage) modal.getOwner();
            // Refrescar combo tras crear
            TextInputDialog tid = new TextInputDialog();
            tid.setTitle("Creación Rápida de Grupo");
            tid.setHeaderText("Nombre del nuevo grupo o clase:");
            tid.setContentText("Nombre:");
            tid.showAndWait().ifPresent(nom -> {
                if (!nom.isBlank()) {
                    Grupo nuevoG = new Grupo();
                    nuevoG.setCodigo(grupoDAO.generarSiguienteCodigo());
                    nuevoG.setNombre(nom.trim());
                    nuevoG.setDias("Lunes y Miércoles");
                    nuevoG.setHorario("16:00 - 18:00");
                    nuevoG.setCupoMaximo(20);
                    nuevoG.setCostoMensualidad(2500.0);
                    nuevoG.setCostoInscripcion(1500.0);
                    grupoDAO.crearGrupo(nuevoG);
                    cbGrupos.getItems().clear();
                    cbGrupos.getItems().addAll(grupoDAO.listarGruposActivos());
                    for (Grupo gr : cbGrupos.getItems()) {
                        if (gr.getCodigo().equals(nuevoG.getCodigo())) {
                            cbGrupos.setValue(gr);
                            break;
                        }
                    }
                }
            });
        });

        HBox boxGrupoSel = new HBox(8, cbGrupos, btnCrearGrupoRapido);
        HBox.setHgrow(cbGrupos, Priority.ALWAYS);

        // 2. Datos del Tutor (Obligatorio si < 18)
        Label lblSec2 = new Label("2. Datos del Tutor / Representante (Obligatorio para menores de 18)");
        lblSec2.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");

        TextField txtTutorNombre = new TextField();
        txtTutorNombre.setPromptText("Nombre completo del padre/madre/tutor");

        TextField txtTutorCedula = new TextField();
        txtTutorCedula.setPromptText("Cédula de identidad / Pasaporte");

        TextField txtTutorWhatsApp = new TextField();
        txtTutorWhatsApp.setPromptText("Teléfono celular / WhatsApp");

        TextField txtTutorEmail = new TextField();
        txtTutorEmail.setPromptText("Correo electrónico");

        TextField txtTutorDireccion = new TextField();
        txtTutorDireccion.setPromptText("Dirección residencial");

        ComboBox<String> cbParentesco = new ComboBox<>();
        cbParentesco.getItems().addAll("PADRE", "MADRE", "TUTOR_LEGAL", "OTRO");
        cbParentesco.setValue("PADRE");

        // Listener dinámico de edad
        dpNacimiento.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int edad = Period.between(newVal, LocalDate.now()).getYears();
                boolean esMenor = edad < 18;
                lblEdadCalculada.setText("Edad: " + edad + " años " + (esMenor ? "(Menor de 18 - Tutor obligatorio)" : "(Mayor de edad)"));
                lblEdadCalculada.setStyle(esMenor ? "-fx-text-fill: #dc2626; -fx-font-weight: bold;" : "-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            }
        });

        // 3. Firma Digital del Contrato
        Label lblSec3 = new Label("3. Contrato Digital y Firma en Pantalla");
        lblSec3.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");

        Label lblEstadoFirma = new Label("⚠️ Contrato no firmado aún.");
        lblEstadoFirma.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");

        final String[] rutaFirmaHolder = new String[1];

        Button btnFirmar = new Button("✍️ Abrir Pantalla de Firma Digital");
        btnFirmar.getStyleClass().add("btn-secondary");
        btnFirmar.setOnAction(e -> {
            String firma = FirmaDigitalDialog.capturarFirma(modal, txtMatricula.getText());
            if (firma != null) {
                rutaFirmaHolder[0] = firma;
                lblEstadoFirma.setText("✅ Firma capturada exitosamente");
                lblEstadoFirma.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            }
        });

        // Botones Guardar
        HBox botones = new HBox(12);
        botones.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-secondary");
        btnCancelar.setOnAction(e -> modal.close());

        Button btnGuardar = new Button("Completar Inscripción y Generar Contrato PDF");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setOnAction(e -> {
            if (txtNombres.getText().isBlank() || txtApellidos.getText().isBlank() || dpNacimiento.getValue() == null) {
                mostrarAlerta("Campos obligatorios", "Por favor completa nombres, apellidos y fecha de nacimiento.", Alert.AlertType.WARNING);
                return;
            }

            int edad = Period.between(dpNacimiento.getValue(), LocalDate.now()).getYears();
            if (edad < 18) {
                if (txtTutorNombre.getText().isBlank() || txtTutorCedula.getText().isBlank() || txtTutorWhatsApp.getText().isBlank()) {
                    mostrarAlerta("Tutor Obligatorio", "Al ser menor de 18 años, el nombre, cédula y WhatsApp del tutor son obligatorios.", Alert.AlertType.WARNING);
                    return;
                }
            }

            Participante p = new Participante();
            p.setMatricula(txtMatricula.getText());
            p.setNombres(txtNombres.getText().trim());
            p.setApellidos(txtApellidos.getText().trim());
            p.setFechaNacimiento(dpNacimiento.getValue());
            p.setContactoEmergenciaNombre(txtEmergenciaNombre.getText().trim());
            p.setContactoEmergenciaTelefono(txtEmergenciaTel.getText().trim());
            p.setAlergiasCondiciones(txtAlergias.getText().trim());
            if (cbGrupos.getValue() != null) {
                p.setGrupoId(cbGrupos.getValue().getId());
            }

            Tutor t = null;
            if (!txtTutorNombre.getText().isBlank()) {
                t = new Tutor();
                t.setNombreCompleto(txtTutorNombre.getText().trim());
                t.setCedula(txtTutorCedula.getText().trim());
                t.setTelefonoWhatsapp(txtTutorWhatsApp.getText().trim());
                t.setEmail(txtTutorEmail.getText().trim());
                t.setDireccion(txtTutorDireccion.getText().trim());
                t.setParentesco(cbParentesco.getValue());
            }

            // Generar contrato PDF si se firmó
            String rutaPdfContrato = null;
            if (rutaFirmaHolder[0] != null) {
                File dirDocs = new File("datos/documentos");
                if (!dirDocs.exists()) dirDocs.mkdirs();
                rutaPdfContrato = new File(dirDocs, "contrato_" + p.getMatricula() + ".pdf").getAbsolutePath();
                PDFGeneratorService.generarContratoPdf(p, t, rutaFirmaHolder[0], rutaPdfContrato);
            }

            boolean ok = participanteDAO.registrarParticipante(p, t, rutaFirmaHolder[0], rutaPdfContrato);
            if (ok) {
                mostrarAlerta("Inscripción Exitosa",
                        "Estudiante inscrito con éxito.\nMatrícula asignada: " + p.getMatricula() +
                        (rutaPdfContrato != null ? "\nContrato PDF generado en:\n" + rutaPdfContrato : ""),
                        Alert.AlertType.INFORMATION);
                modal.close();
                cargarDatos();
            } else {
                mostrarAlerta("Error", "No se pudo registrar al estudiante.", Alert.AlertType.ERROR);
            }
        });

        botones.getChildren().addAll(btnCancelar, btnGuardar);

        form.getChildren().addAll(
                lblSec1,
                new Label("Matrícula Única:"), txtMatricula,
                new Label("Nombres:"), txtNombres,
                new Label("Apellidos:"), txtApellidos,
                new Label("Fecha de Nacimiento:"), dpNacimiento, lblEdadCalculada,
                new Label("Contacto de Emergencia:"), txtEmergenciaNombre, txtEmergenciaTel,
                new Label("Alergias o Condiciones Médicas:"), txtAlergias,
                new Label("Grupo Asignado:"), boxGrupoSel,
                new Separator(),
                lblSec2,
                new Label("Nombre Completo Tutor:"), txtTutorNombre,
                new Label("Cédula / Identificación:"), txtTutorCedula,
                new Label("Teléfono / WhatsApp:"), txtTutorWhatsApp,
                new Label("Correo Tutor:"), txtTutorEmail,
                new Label("Parentesco:"), cbParentesco,
                new Label("Dirección:"), txtTutorDireccion,
                new Separator(),
                lblSec3,
                lblEstadoFirma, btnFirmar,
                new Separator(),
                botones
        );

        scroll.setContent(form);
        modal.setScene(new Scene(scroll, 560, 680));
        modal.showAndWait();
    }

    private void abrirModalCambiarEstado(Stage stage, Participante p) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(stage);
        modal.setTitle("Cambiar Estado de: " + p.getNombreCompleto());

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        Label lbl = new Label("Seleccione el nuevo estado para el estudiante:");
        lbl.setStyle("-fx-font-weight: bold;");

        ComboBox<String> cbEstado = new ComboBox<>();
        cbEstado.getItems().addAll("ACTIVO", "PAUSA", "INACTIVO");
        cbEstado.setValue(p.getEstado());

        Label lblMotivo = new Label("Motivo de la baja o pausa (obligatorio si no es activo):");
        TextArea txtMotivo = new TextArea();
        txtMotivo.setPromptText("Ej. Mudanza, razones económicas, conflicto de horario, viaje...");
        txtMotivo.setPrefRowCount(3);

        cbEstado.setOnAction(e -> {
            boolean requiereMotivo = !"ACTIVO".equalsIgnoreCase(cbEstado.getValue());
            lblMotivo.setVisible(requiereMotivo);
            txtMotivo.setVisible(requiereMotivo);
        });

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> modal.close());

        Button btnConfirmar = new Button("Confirmar Cambio");
        btnConfirmar.getStyleClass().add("btn-primary");
        btnConfirmar.setOnAction(e -> {
            String nuevoEstado = cbEstado.getValue();
            String motivo = txtMotivo.getText().trim();

            if (!"ACTIVO".equalsIgnoreCase(nuevoEstado) && motivo.isBlank()) {
                mostrarAlerta("Motivo Requerido", "Por favor ingrese el motivo por el cual el estudiante pasa a " + nuevoEstado + ".", Alert.AlertType.WARNING);
                return;
            }

            participanteDAO.cambiarEstado(p.getId(), nuevoEstado, motivo);
            modal.close();
            cargarDatos();
        });

        botones.getChildren().addAll(btnCancelar, btnConfirmar);
        root.getChildren().addAll(lbl, cbEstado, lblMotivo, txtMotivo, botones);

        modal.setScene(new Scene(root, 420, 280));
        modal.showAndWait();
    }

    private void mostrarFichaEstudiante(Stage stage, Participante p) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Ficha del Estudiante: " + p.getMatricula());
        alert.setHeaderText(p.getNombreCompleto() + " (" + p.getEstado() + ")");
        String contenido = String.format(
                "Matrícula: %s\nEdad: %d años (Nacido: %s)\nGrupo: %s\nAlergias/Salud: %s\nContacto Emergencia: %s (%s)\n\n" +
                "Tutor: %s\nCédula: %s\nWhatsApp: %s\nEmail: %s\nDirección: %s\n" +
                (p.getMotivoBaja() != null ? "\nMotivo de Salida: " + p.getMotivoBaja() + " (" + p.getFechaBaja() + ")" : ""),
                p.getMatricula(), p.getEdad(), p.getFechaNacimiento(),
                p.getNombreGrupo() != null ? p.getNombreGrupo() : "Sin asignar",
                p.getAlergiasCondiciones() != null ? p.getAlergiasCondiciones() : "Ninguna",
                p.getContactoEmergenciaNombre() != null ? p.getContactoEmergenciaNombre() : "N/A",
                p.getContactoEmergenciaTelefono() != null ? p.getContactoEmergenciaTelefono() : "N/A",
                p.getTutor() != null ? p.getTutor().getNombreCompleto() : "No registrado",
                p.getTutor() != null ? p.getTutor().getCedula() : "N/A",
                p.getTutor() != null ? p.getTutor().getTelefonoWhatsapp() : "N/A",
                p.getTutor() != null ? p.getTutor().getEmail() : "N/A",
                p.getTutor() != null ? p.getTutor().getDireccion() : "N/A"
        );
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    private void exportarExcel(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Listado de Estudiantes");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos Excel (*.xlsx)", "*.xlsx"));
        fileChooser.setInitialFileName("Estudiantes_" + LocalDate.now() + ".xlsx");
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            boolean ok = ExcelExportService.exportarEstudiantes(tabla.getItems(), file.getAbsolutePath());
            if (ok) {
                mostrarAlerta("Exportado", "Archivo Excel generado con éxito en:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            }
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }
}
