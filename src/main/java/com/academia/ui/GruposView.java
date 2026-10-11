package com.academia.ui;

import com.academia.dao.GrupoDAO;
import com.academia.dao.ProfesorDAO;
import com.academia.model.Grupo;
import com.academia.model.Participante;
import com.academia.model.Profesor;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class GruposView {

    private final GrupoDAO grupoDAO = new GrupoDAO();
    private final ProfesorDAO profesorDAO = new ProfesorDAO();
    private final TableView<Grupo> tabla = new TableView<>();
    private final TextField txtBusqueda = new TextField();

    public VBox crearVista(Stage stage) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        // Barra Superior
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label lblTitulo = new Label("Grupos, Clases y Horarios");
        lblTitulo.getStyleClass().add("header-title");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);

        Button btnNuevo = new Button("+ Crear Nuevo Grupo");
        btnNuevo.getStyleClass().add("btn-primary");
        btnNuevo.setOnAction(e -> abrirModalGrupo(stage, null));

        topBar.getChildren().addAll(lblTitulo, btnNuevo);

        // Barra de Búsqueda
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        txtBusqueda.setPromptText("Buscar por nombre, código, días o profesor...");
        txtBusqueda.setPrefWidth(320);
        txtBusqueda.textProperty().addListener((obs, old, val) -> cargarDatos());

        filterBar.getChildren().addAll(new Label("Buscar:"), txtBusqueda);

        configurarTabla(stage);

        root.getChildren().addAll(topBar, filterBar, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        cargarDatos();
        return root;
    }

    private void configurarTabla(Stage stage) {
        TableColumn<Grupo, String> colCodigo = new TableColumn<>("Código");
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCodigo.setPrefWidth(90);

        TableColumn<Grupo, String> colNombre = new TableColumn<>("Nombre del Grupo / Curso");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colNombre.setPrefWidth(180);

        TableColumn<Grupo, String> colDias = new TableColumn<>("Días");
        colDias.setCellValueFactory(new PropertyValueFactory<>("dias"));
        colDias.setPrefWidth(130);

        TableColumn<Grupo, String> colHorario = new TableColumn<>("Horario");
        colHorario.setCellValueFactory(new PropertyValueFactory<>("horario"));
        colHorario.setPrefWidth(110);

        TableColumn<Grupo, String> colProfesor = new TableColumn<>("Profesor Asignado");
        colProfesor.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getNombreProfesor() != null ? cell.getValue().getNombreProfesor() : "Sin asignar"
        ));
        colProfesor.setPrefWidth(160);

        TableColumn<Grupo, String> colCupos = new TableColumn<>("Inscritos / Cupo");
        colCupos.setCellValueFactory(cell -> {
            Grupo g = cell.getValue();
            int disponibles = g.getCuposDisponibles();
            String info = g.getTotalInscritos() + " / " + g.getCupoMaximo();
            return new SimpleStringProperty(disponibles > 0 ? info + " (" + disponibles + " disp.)" : info + " (LLENO)");
        });
        colCupos.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("LLENO")) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    }
                }
            }
        });
        colCupos.setPrefWidth(150);

        TableColumn<Grupo, String> colCosto = new TableColumn<>("Mensualidad");
        colCosto.setCellValueFactory(cell -> new SimpleStringProperty(String.format("$%.2f", cell.getValue().getCostoMensualidad())));
        colCosto.setPrefWidth(100);

        TableColumn<Grupo, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(210);
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnAlumnos = new Button("Alumnos");
            private final Button btnEditar = new Button("Editar");
            private final Button btnToggle = new Button();
            private final HBox panel = new HBox(6, btnAlumnos, btnEditar, btnToggle);

            {
                btnAlumnos.getStyleClass().add("btn-secondary");
                btnEditar.getStyleClass().add("btn-secondary");

                btnAlumnos.setOnAction(e -> {
                    Grupo g = getTableView().getItems().get(getIndex());
                    mostrarAlumnosDelGrupo(stage, g);
                });

                btnEditar.setOnAction(e -> {
                    Grupo g = getTableView().getItems().get(getIndex());
                    abrirModalGrupo(stage, g);
                });

                btnToggle.setOnAction(e -> {
                    Grupo g = getTableView().getItems().get(getIndex());
                    grupoDAO.cambiarEstadoGrupo(g.getId(), !g.isActivo());
                    cargarDatos();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Grupo g = getTableView().getItems().get(getIndex());
                    if (g.isActivo()) {
                        btnToggle.setText("Pausar");
                        btnToggle.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-padding: 4 8;");
                    } else {
                        btnToggle.setText("Activar");
                        btnToggle.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 8;");
                    }
                    setGraphic(panel);
                }
            }
        });

        tabla.getColumns().addAll(java.util.List.of(
            colCodigo, 
            colNombre, 
            colDias, 
            colHorario, 
            colProfesor, 
            colCupos, 
            colCosto, 
            colAcciones)
        );
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void cargarDatos() {
        List<Grupo> grupos = grupoDAO.listarTodos(txtBusqueda.getText());
        tabla.setItems(FXCollections.observableArrayList(grupos));
    }

    private void abrirModalGrupo(Stage stage, Grupo existente) {
        boolean esEdicion = existente != null;
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(stage);
        modal.setTitle(esEdicion ? "Editar Grupo: " + existente.getNombre() : "Crear Nuevo Grupo o Clase");

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));

        TextField txtCodigo = new TextField(esEdicion ? existente.getCodigo() : grupoDAO.generarSiguienteCodigo());
        TextField txtNombre = new TextField(esEdicion ? existente.getNombre() : "");
        txtNombre.setPromptText("Ej. Piano Nivel 1, Danza Moderna, Inglés Principiantes");

        ComboBox<String> cbDias = new ComboBox<>();
        cbDias.setEditable(true);
        cbDias.getItems().addAll("Lunes y Miércoles", "Martes y Jueves", "Viernes", "Sábados", "Lunes a Viernes");
        cbDias.setValue(esEdicion ? existente.getDias() : "Lunes y Miércoles");

        ComboBox<String> cbHorario = new ComboBox<>();
        cbHorario.setEditable(true);
        cbHorario.getItems().addAll("08:00 - 10:00", "10:00 - 12:00", "14:00 - 16:00", "16:00 - 18:00", "18:00 - 20:00");
        cbHorario.setValue(esEdicion ? existente.getHorario() : "15:00 - 17:00");

        TextField txtCupo = new TextField(esEdicion ? String.valueOf(existente.getCupoMaximo()) : "20");
        TextField txtCostoMensual = new TextField(esEdicion ? String.valueOf(existente.getCostoMensualidad()) : "2500.00");
        TextField txtCostoInscripcion = new TextField(esEdicion ? String.valueOf(existente.getCostoInscripcion()) : "1500.00");

        // Selección de Profesor
        ComboBox<Profesor> cbProfesores = new ComboBox<>();
        cargarProfesores(cbProfesores);

        if (esEdicion && existente.getProfesorId() != null) {
            for (Profesor p : cbProfesores.getItems()) {
                if (p.getId() == existente.getProfesorId()) {
                    cbProfesores.setValue(p);
                    break;
                }
            }
        }

        Button btnNuevoProfesor = new Button("+ Docente");
        btnNuevoProfesor.getStyleClass().add("btn-secondary");
        btnNuevoProfesor.setOnAction(e -> abrirModalNuevoProfesor(modal, cbProfesores));

        HBox boxProfesor = new HBox(8, cbProfesores, btnNuevoProfesor);
        HBox.setHgrow(cbProfesores, Priority.ALWAYS);

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> modal.close());

        Button btnGuardar = new Button(esEdicion ? "Guardar Cambios" : "Crear Grupo");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setOnAction(e -> {
            try {
                String nombre = txtNombre.getText().trim();
                String codigo = txtCodigo.getText().trim();
                String dias = cbDias.getValue() != null ? cbDias.getValue().trim() : "";
                String horario = cbHorario.getValue() != null ? cbHorario.getValue().trim() : "";
                int cupo = Integer.parseInt(txtCupo.getText().trim());
                double costoMensual = Double.parseDouble(txtCostoMensual.getText().trim());
                double costoInscripcion = Double.parseDouble(txtCostoInscripcion.getText().trim());

                if (nombre.isBlank() || codigo.isBlank() || dias.isBlank() || horario.isBlank()) {
                    mostrarAlerta("Campos requeridos", "Por favor completa código, nombre, días y horario.", Alert.AlertType.WARNING);
                    return;
                }

                Grupo g = esEdicion ? existente : new Grupo();
                g.setCodigo(codigo);
                g.setNombre(nombre);
                g.setDias(dias);
                g.setHorario(horario);
                g.setCupoMaximo(cupo);
                g.setCostoMensualidad(costoMensual);
                g.setCostoInscripcion(costoInscripcion);
                if (cbProfesores.getValue() != null) {
                    g.setProfesorId(cbProfesores.getValue().getId());
                } else {
                    g.setProfesorId(null);
                }

                boolean ok = esEdicion ? grupoDAO.actualizarGrupo(g) : grupoDAO.crearGrupo(g);
                if (ok) {
                    mostrarAlerta("Éxito", "Grupo guardado correctamente.", Alert.AlertType.INFORMATION);
                    modal.close();
                    cargarDatos();
                } else {
                    mostrarAlerta("Error", "No se pudo guardar el grupo.", Alert.AlertType.ERROR);
                }
            } catch (NumberFormatException ex) {
                mostrarAlerta("Formato", "Cupo y costos deben ser valores numéricos válidos.", Alert.AlertType.ERROR);
            }
        });

        botones.getChildren().addAll(btnCancelar, btnGuardar);
        form.getChildren().addAll(
                new Label("Código Único:"), txtCodigo,
                new Label("Nombre del Curso / Grupo:"), txtNombre,
                new Label("Días de Clase:"), cbDias,
                new Label("Horario:"), cbHorario,
                new Label("Profesor Responsable:"), boxProfesor,
                new Label("Cupo Máximo de Alumnos:"), txtCupo,
                new Label("Costo Cuota Mensual ($):"), txtCostoMensual,
                new Label("Costo Inscripción / Matrícula ($):"), txtCostoInscripcion,
                new Separator(), botones
        );

        modal.setScene(new Scene(form, 480, 520));
        modal.showAndWait();
    }

    private void cargarProfesores(ComboBox<Profesor> cb) {
        cb.getItems().clear();
        cb.getItems().addAll(profesorDAO.listarActivos());
        cb.setPromptText("Seleccionar profesor (opcional)");
    }

    private void abrirModalNuevoProfesor(Stage parent, ComboBox<Profesor> cbProfesores) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(parent);
        modal.setTitle("Registrar Nuevo Docente");

        VBox form = new VBox(10);
        form.setPadding(new Insets(20));

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre completo");

        TextField txtEspecialidad = new TextField();
        txtEspecialidad.setPromptText("Especialidad (ej. Música, Piano, Danza)");

        TextField txtTel = new TextField();
        txtTel.setPromptText("Teléfono / WhatsApp");

        Button btnGuardar = new Button("Guardar Docente");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setOnAction(e -> {
            if (txtNombre.getText().isBlank()) return;
            Profesor p = new Profesor();
            p.setNombreCompleto(txtNombre.getText().trim());
            p.setEspecialidad(txtEspecialidad.getText().trim());
            p.setTelefono(txtTel.getText().trim());
            if (profesorDAO.crearProfesor(p)) {
                cargarProfesores(cbProfesores);
                cbProfesores.setValue(p);
                modal.close();
            }
        });

        form.getChildren().addAll(new Label("Nombre Completo:"), txtNombre, new Label("Especialidad:"), txtEspecialidad, new Label("Teléfono:"), txtTel, btnGuardar);
        modal.setScene(new Scene(form, 350, 240));
        modal.showAndWait();
    }

    private void mostrarAlumnosDelGrupo(Stage stage, Grupo g) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(stage);
        modal.setTitle("Alumnos Inscritos en: " + g.getNombre());

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        List<Participante> alumnos = grupoDAO.obtenerEstudiantesDelGrupo(g.getId());

        Label lblInfo = new Label("Grupo: " + g.getNombre() + " (" + g.getDias() + " " + g.getHorario() + ") - Total: " + alumnos.size() + "/" + g.getCupoMaximo() + " alumnos");
        lblInfo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TableView<Participante> tablaAlumnos = new TableView<>();
        tablaAlumnos.setItems(FXCollections.observableArrayList(alumnos));

        TableColumn<Participante, String> cMat = new TableColumn<>("Matrícula");
        cMat.setCellValueFactory(new PropertyValueFactory<>("matricula"));

        TableColumn<Participante, String> cNom = new TableColumn<>("Nombre Completo");
        cNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNombreCompleto()));

        TableColumn<Participante, String> cEst = new TableColumn<>("Estado");
        cEst.setCellValueFactory(new PropertyValueFactory<>("estado"));

        tablaAlumnos.getColumns().addAll(java.util.List.of(
            cMat, 
            cNom, 
            cEst)
        );
        tablaAlumnos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        root.getChildren().addAll(lblInfo, tablaAlumnos);
        VBox.setVgrow(tablaAlumnos, Priority.ALWAYS);

        modal.setScene(new Scene(root, 480, 360));
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
