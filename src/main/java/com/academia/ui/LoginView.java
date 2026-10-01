package com.academia.ui;

import com.academia.config.AppSession;
import com.academia.dao.UsuarioDAO;
import com.academia.model.Usuario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LoginView {

    private final Stage stage;
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public LoginView(Stage stage) {
        this.stage = stage;
    }

    public void mostrar() {
        stage.setTitle("Acceso al Sistema - Administración de Academia");

        // Contenedor principal con fondo suave
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0f172a;");

        // Tarjeta de login centrada
        VBox card = new VBox(18);
        card.setMaxWidth(420);
        card.setPadding(new Insets(35, 35, 35, 35));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 14px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 8);");

        Label lblTitulo = new Label("ACADEMIA ADMIN");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

        Label lblSubtitulo = new Label("Ingresa tus credenciales para acceder");
        lblSubtitulo.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-padding: 0 0 10 0;");

        // Campos
        VBox form = new VBox(12);
        form.setAlignment(Pos.CENTER_LEFT);

        Label lblUser = new Label("Usuario:");
        lblUser.getStyleClass().add("form-label");
        TextField txtUser = new TextField();
        txtUser.setPromptText("ej. admin o secretaria");
        txtUser.setText("admin"); // Conveniencia inicial

        Label lblPass = new Label("Contraseña:");
        lblPass.getStyleClass().add("form-label");
        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Contraseña");
        txtPass.setText("admin123");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
        lblError.setVisible(false);

        Button btnIngresar = new Button("Iniciar Sesión");
        btnIngresar.getStyleClass().add("btn-primary");
        btnIngresar.setMaxWidth(Double.MAX_VALUE);
        btnIngresar.setStyle("-fx-font-size: 14px; -fx-padding: 10 0 10 0;");

        Runnable intentarLogin = () -> {
            String user = txtUser.getText().trim();
            String pass = txtPass.getText();

            if (user.isEmpty() || pass.isEmpty()) {
                lblError.setText("Por favor complete todos los campos.");
                lblError.setVisible(true);
                return;
            }

            Usuario u = usuarioDAO.autenticar(user, pass);
            if (u != null) {
                AppSession.setUsuarioActual(u);
                new MainDashboardView(stage).mostrar();
            } else {
                lblError.setText("Usuario o contraseña incorrectos.");
                lblError.setVisible(true);
            }
        };

        btnIngresar.setOnAction(e -> intentarLogin.run());
        txtPass.setOnAction(e -> intentarLogin.run());
        txtUser.setOnAction(e -> intentarLogin.run());

        form.getChildren().addAll(lblUser, txtUser, lblPass, txtPass, lblError);
        card.getChildren().addAll(lblTitulo, lblSubtitulo, form, btnIngresar);
        root.getChildren().add(card);

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
}
