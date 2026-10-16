package com.academia.ui;

import com.academia.config.AppSession;
import com.academia.dao.UsuarioDAO;
import com.academia.model.Usuario;
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

public class UsuariosView {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final TableView<Usuario> tabla = new TableView<>();

    public VBox crearVista(Stage stage) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: transparent;");

        // Barra Superior
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label lblTitulo = new Label("Gestión de Usuarios y Accesos");
        lblTitulo.getStyleClass().add("header-title");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);

        Button btnNuevo = new Button("+ Crear Nuevo Usuario");
        btnNuevo.getStyleClass().add("btn-primary");
        btnNuevo.setOnAction(e -> abrirModalNuevoUsuario(stage));

        topBar.getChildren().addAll(lblTitulo, btnNuevo);

        Label lblInfo = new Label("Administra las cuentas del personal. Puedes crear los usuarios reales de la academia y eliminar los de prueba cuando ya no los necesites.");
        lblInfo.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        configurarTabla(stage);

        root.getChildren().addAll(topBar, lblInfo, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        cargarDatos();
        return root;
    }

    private void configurarTabla(Stage stage) {
        TableColumn<Usuario, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Usuario, String> colUser = new TableColumn<>("Usuario (Login)");
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUser.setPrefWidth(140);

        TableColumn<Usuario, String> colNombre = new TableColumn<>("Nombre Completo");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colNombre.setPrefWidth(220);

        TableColumn<Usuario, String> colRol = new TableColumn<>("Rol de Acceso");
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colRol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.setStyle(switch (item) {
                        case "ADMIN" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af; -fx-padding: 3 8; -fx-background-radius: 10px; -fx-font-weight: bold;";
                        case "SECRETARIA" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 3 8; -fx-background-radius: 10px; -fx-font-weight: bold;";
                        default -> "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-padding: 3 8; -fx-background-radius: 10px; -fx-font-weight: bold;";
                    });
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });
        colRol.setPrefWidth(130);

        TableColumn<Usuario, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isActivo() ? "Activo" : "Inactivo"));
        colEstado.setPrefWidth(85);

        TableColumn<Usuario, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(260);
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnClave = new Button("Clave");
            private final Button btnEliminar = new Button("Eliminar");
            private final HBox panel = new HBox(6, btnEditar, btnClave, btnEliminar);

            {
                btnEditar.getStyleClass().add("btn-secondary");
                btnClave.getStyleClass().add("btn-secondary");
                btnEliminar.getStyleClass().add("btn-danger");

                btnEditar.setOnAction(e -> {
                    Usuario u = getTableView().getItems().get(getIndex());
                    abrirModalEditarUsuario(stage, u);
                });

                btnClave.setOnAction(e -> {
                    Usuario u = getTableView().getItems().get(getIndex());
                    abrirModalCambiarClave(stage, u);
                });

                btnEliminar.setOnAction(e -> {
                    Usuario u = getTableView().getItems().get(getIndex());
                    confirmarEliminacion(stage, u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Usuario u = getTableView().getItems().get(getIndex());
                    // Solo se deshabilita la eliminación para el usuario que tiene su propia sesión activa
                    boolean esUsuarioActual = AppSession.getUsuarioActual() != null && AppSession.getUsuarioActual().getId() == u.getId();
                    btnEliminar.setDisable(esUsuarioActual);
                    if (esUsuarioActual) {
                        btnEliminar.setTooltip(new Tooltip("No puedes eliminar tu propia sesión activa"));
                    }
                    setGraphic(panel);
                }
            }
        });

        tabla.getColumns().addAll(colId, colUser, colNombre, colRol, colEstado, colAcciones);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void cargarDatos() {
        List<Usuario> usuarios = usuarioDAO.listarTodos();
        tabla.setItems(FXCollections.observableArrayList(usuarios));
    }

    private void abrirModalNuevoUsuario(Stage stage) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(stage);
        modal.setTitle("Crear Nuevo Usuario");

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));

        TextField txtUser = new TextField();
        txtUser.setPromptText("Nombre de usuario (ej. mariagonzalez)");

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre completo (ej. María González)");

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Contraseña inicial");

        ComboBox<String> cbRol = new ComboBox<>();
        cbRol.getItems().addAll("ADMIN", "SECRETARIA", "PROFESOR");
        cbRol.setValue("SECRETARIA");

        Label lblRolDesc = new Label("SECRETARIA: Registra alumnos, cobra y emite recibos. No ve ganancias ni nóminas.");
        lblRolDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        lblRolDesc.setWrapText(true);

        cbRol.setOnAction(e -> {
            switch (cbRol.getValue()) {
                case "ADMIN" -> lblRolDesc.setText("ADMIN: Acceso total al sistema, nóminas, cierres y balances.");
                case "SECRETARIA" -> lblRolDesc.setText("SECRETARIA: Registra alumnos, cobra y emite recibos. No ve ganancias ni nóminas.");
                case "PROFESOR" -> lblRolDesc.setText("PROFESOR: Solo accede a sus grupos y toma asistencia.");
            }
        });

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> modal.close());

        Button btnGuardar = new Button("Crear Usuario");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setOnAction(e -> {
            String uName = txtUser.getText().trim();
            String nComp = txtNombre.getText().trim();
            String pass = txtPass.getText().trim();

            if (uName.isBlank() || nComp.isBlank() || pass.isBlank()) {
                mostrarAlerta("Campos requeridos", "Todos los campos son obligatorios.", Alert.AlertType.WARNING);
                return;
            }

            Usuario u = new Usuario();
            u.setUsername(uName.toLowerCase());
            u.setNombreCompleto(nComp);
            u.setRol(cbRol.getValue());
            u.setActivo(true);

            boolean ok = usuarioDAO.crearUsuario(u, pass);
            if (ok) {
                mostrarAlerta("Usuario Creado", "El usuario '" + uName + "' fue creado exitosamente con rol " + cbRol.getValue(), Alert.AlertType.INFORMATION);
                modal.close();
                cargarDatos();
            } else {
                mostrarAlerta("Error", "No se pudo crear el usuario. Es posible que el nombre de usuario ya exista.", Alert.AlertType.ERROR);
            }
        });

        botones.getChildren().addAll(btnCancelar, btnGuardar);
        form.getChildren().addAll(
                new Label("Nombre de Usuario (Login):"), txtUser,
                new Label("Nombre Completo:"), txtNombre,
                new Label("Contraseña Inicial:"), txtPass,
                new Label("Rol Asignado:"), cbRol, lblRolDesc,
                new Separator(), botones
        );

        modal.setScene(new Scene(form, 400, 360));
        modal.showAndWait();
    }

    private void abrirModalEditarUsuario(Stage stage, Usuario u) {
        Stage modal = new Stage();
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initOwner(stage);
        modal.setTitle("Editar Usuario: " + u.getUsername());

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));

        TextField txtUser = new TextField(u.getUsername());
        TextField txtNombre = new TextField(u.getNombreCompleto());
        ComboBox<String> cbRol = new ComboBox<>();
        cbRol.getItems().addAll("ADMIN", "SECRETARIA", "PROFESOR");
        cbRol.setValue(u.getRol());

        PasswordField txtNuevaClave = new PasswordField();
        txtNuevaClave.setPromptText("Dejar en blanco para mantener la clave actual");

        CheckBox chkActivo = new CheckBox("Usuario Activo");
        chkActivo.setSelected(u.isActivo());

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> modal.close());

        Button btnGuardar = new Button("Guardar Cambios");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setOnAction(e -> {
            String uName = txtUser.getText().trim();
            String nComp = txtNombre.getText().trim();
            if (uName.isBlank() || nComp.isBlank()) {
                mostrarAlerta("Campos requeridos", "Usuario y nombre completo no pueden estar vacíos.", Alert.AlertType.WARNING);
                return;
            }

            u.setUsername(uName.toLowerCase());
            u.setNombreCompleto(nComp);
            u.setRol(cbRol.getValue());
            u.setActivo(chkActivo.isSelected());

            boolean ok = usuarioDAO.actualizarUsuario(u, txtNuevaClave.getText());
            if (ok) {
                mostrarAlerta("Éxito", "Usuario '" + u.getUsername() + "' actualizado correctamente.", Alert.AlertType.INFORMATION);
                modal.close();
                cargarDatos();
            } else {
                mostrarAlerta("Error", "No se pudo actualizar el usuario.", Alert.AlertType.ERROR);
            }
        });

        botones.getChildren().addAll(btnCancelar, btnGuardar);
        form.getChildren().addAll(
                new Label("Usuario (Login):"), txtUser,
                new Label("Nombre Completo:"), txtNombre,
                new Label("Rol Asignado:"), cbRol,
                new Label("Nueva Contraseña (opcional):"), txtNuevaClave,
                chkActivo,
                new Separator(), botones
        );

        modal.setScene(new Scene(form, 400, 360));
        modal.showAndWait();
    }

    private void abrirModalCambiarClave(Stage stage, Usuario u) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(stage);
        dialog.setTitle("Cambiar Contraseña");
        dialog.setHeaderText("Nueva contraseña para el usuario: " + u.getUsername());
        dialog.setContentText("Nueva clave:");
        dialog.showAndWait().ifPresent(nuevaClave -> {
            if (nuevaClave.isBlank()) return;
            usuarioDAO.actualizarPassword(u.getId(), nuevaClave.trim());
            mostrarAlerta("Contraseña Actualizada", "La contraseña de '" + u.getUsername() + "' ha sido actualizada correctamente.", Alert.AlertType.INFORMATION);
        });
    }

    private void confirmarEliminacion(Stage stage, Usuario u) {
        if (AppSession.getUsuarioActual() != null && AppSession.getUsuarioActual().getId() == u.getId()) {
            mostrarAlerta("Acción denegada", "No puedes eliminar tu propia cuenta mientras tienes la sesión iniciada.", Alert.AlertType.WARNING);
            return;
        }

        if ("ADMIN".equalsIgnoreCase(u.getRol()) && usuarioDAO.contarAdmins() <= 1) {
            mostrarAlerta("Acción denegada", "No puedes eliminar el único Administrador del sistema. Crea primero otro usuario con rol ADMIN.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(stage);
        confirm.setTitle("Confirmar Eliminación");
        confirm.setHeaderText("¿Estás seguro de eliminar permanentemente al usuario '" + u.getUsername() + "'?");
        confirm.setContentText("Esta acción borrará el acceso para " + u.getNombreCompleto() + " (" + u.getRol() + ").");

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                boolean ok = usuarioDAO.eliminarUsuario(u.getId());
                if (ok) {
                    mostrarAlerta("Eliminado", "Usuario eliminado con éxito de la base de datos.", Alert.AlertType.INFORMATION);
                    cargarDatos();
                } else {
                    mostrarAlerta("Error", "No se pudo eliminar el usuario.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }
}
