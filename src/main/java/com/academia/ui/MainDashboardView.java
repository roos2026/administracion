package com.academia.ui;

import com.academia.config.AppSession;
import com.academia.dao.GastoDAO;
import com.academia.dao.MensualidadDAO;
import com.academia.dao.ParticipanteDAO;
import com.academia.service.BackupService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.YearMonth;

public class MainDashboardView {

    private final Stage stage;
    private final BorderPane rootLayout = new BorderPane();
    private final ParticipanteDAO participanteDAO = new ParticipanteDAO();
    private final MensualidadDAO mensualidadDAO = new MensualidadDAO();
    private final GastoDAO gastoDAO = new GastoDAO();

    public MainDashboardView(Stage stage) {
        this.stage = stage;
    }

    public void mostrar() {
        stage.setTitle("Sistema de Administración Integral - Academia");

        // Construcción de la vista
        rootLayout.setLeft(crearSidebar());
        rootLayout.setTop(crearHeader());
        rootLayout.setCenter(crearDashboardResumen());

        Scene scene = new Scene(rootLayout, 1780, 1000);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    private VBox crearHeader() {
        VBox box = new VBox();
        HBox header = new HBox(16);
        header.getStyleClass().add("top-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblAppName = new Label("ACADEMIA ADMINISTRACIÓN");
        lblAppName.setStyle("-fx-font-weight: 800; -fx-font-size: 16px; -fx-text-fill: #1e293b;");
        HBox.setHgrow(lblAppName, Priority.ALWAYS);

        String rol = AppSession.getUsuarioActual() != null ? AppSession.getUsuarioActual().getRol() : "ADMIN";
        String nombre = AppSession.getUsuarioActual() != null ? AppSession.getUsuarioActual().getNombreCompleto() : "Usuario";

        Label lblUser = new Label("👤 " + nombre + " (" + rol + ")");
        lblUser.getStyleClass().add("header-user-tag");

        Button btnSalir = new Button("Cerrar Sesión");
        btnSalir.getStyleClass().add("btn-secondary");
        btnSalir.setOnAction(e -> {
            AppSession.setUsuarioActual(null);
            new LoginView(stage).mostrar();
        });

        header.getChildren().addAll(lblAppName, lblUser, btnSalir);
        box.getChildren().add(header);
        return box;
    }

    private VBox crearSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");

        Label lblTitle = new Label("MENÚ PRINCIPAL");
        lblTitle.getStyleClass().add("sidebar-title");

        Button btnDash = crearBotonMenu("📊  Dashboard / Métricas", () -> rootLayout.setCenter(crearDashboardResumen()));
        Button btnGrupos = crearBotonMenu("🏫  Grupos, Clases & Cupos", () -> rootLayout.setCenter(new GruposView().crearVista(stage)));
        Button btnAlumnos = crearBotonMenu("👥  Participantes & Fichas", () -> rootLayout.setCenter(new ParticipantesView().crearVista(stage)));
        Button btnCobros = crearBotonMenu("💳  Mensualidades & Pagos", () -> rootLayout.setCenter(new CobranzasView().crearVista(stage)));

        // Módulo de nómina (Profesores y Secretaría)
        Button btnNomina = crearBotonMenu("💵  Pago de Nómina", () -> {
            if (AppSession.isSecretaria()) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Acceso restringido: Las secretarias no tienen acceso al módulo de nóminas.");
                a.showAndWait();
                return;
            }
            rootLayout.setCenter(new NominaView().crearVista(stage));
        });

        // Módulo de gastos y compras
        Button btnGastos = crearBotonMenu("💸  Gastos & Compras", () -> {
            if (AppSession.isSecretaria()) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Acceso restringido: Las secretarias no tienen acceso al módulo confidencial de gastos.");
                a.showAndWait();
                return;
            }
            rootLayout.setCenter(new GastosView().crearVista(stage));
        });

        Button btnCaja = crearBotonMenu("🗄️  Cierre de Caja Diario", () -> rootLayout.setCenter(new CajaView().crearVista(stage)));

        // Módulo de Usuarios y Seguridad (Solo Admin)
        Button btnUsuarios = crearBotonMenu("🔐  Usuarios & Accesos", () -> {
            if (AppSession.isSecretaria()) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Acceso restringido: Solo administradores pueden gestionar usuarios y accesos.");
                a.showAndWait();
                return;
            }
            rootLayout.setCenter(new UsuariosView().crearVista(stage));
        });

        Button btnBackup = crearBotonMenu("💾  Respaldar Base de Datos", this::ejecutarRespaldo);

        sidebar.getChildren().addAll(lblTitle, btnDash, btnGrupos, btnAlumnos, btnCobros, btnNomina, btnGastos, btnCaja, btnUsuarios, new Separator(), btnBackup);
        return sidebar;
    }

    private Button crearBotonMenu(String texto, Runnable accion) {
        Button btn = new Button(texto);
        btn.getStyleClass().add("sidebar-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> accion.run());
        return btn;
    }

    private ScrollPane crearDashboardResumen() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(24);
        content.setPadding(new Insets(24));

        Label lblTitulo = new Label("Panel de Control y Estadísticas Clave");
        lblTitulo.getStyleClass().add("header-title");

        // Datos en tiempo real
        int activos = participanteDAO.contarPorEstado("ACTIVO");
        int pausa = participanteDAO.contarPorEstado("PAUSA");
        int inactivos = participanteDAO.contarPorEstado("INACTIVO");
        int morosos = mensualidadDAO.obtenerMorosos().size();

        // Tarjetas Métricas Superiores
        FlowPane flowTarjetas = new FlowPane();
        flowTarjetas.setHgap(16);
        flowTarjetas.setVgap(16);

        flowTarjetas.getChildren().addAll(
                crearTarjeta("Estudiantes Activos", String.valueOf(activos), "#2563eb"),
                crearTarjeta("En Pausa / Vacaciones", String.valueOf(pausa), "#f59e0b"),
                crearTarjeta("Estudiantes Inactivos / Bajas", String.valueOf(inactivos), "#64748b"),
                crearTarjeta("Cuotas Morosas Vencidas", String.valueOf(morosos), "#dc2626")
        );

        // Resumen Financiero Mes (Restringido para Secretaria, visible para Admin)
        VBox secFinanzas = new VBox(14);
        if (AppSession.isAdmin()) {
            Label lblFin = new Label("Resumen Financiero del Mes (" + YearMonth.now() + ")");
            lblFin.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            double totalGastos = gastoDAO.totalGastosMes(YearMonth.now().toString());

            FlowPane flowFin = new FlowPane();
            flowFin.setHgap(16);
            flowFin.setVgap(16);

            flowFin.getChildren().addAll(
                    crearTarjeta("Total Gastos y Compras del Mes", String.format("$%.2f", totalGastos), "#dc2626"),
                    crearTarjeta("Estado de Caja", "Monitoreo en vivo", "#16a34a")
            );
            secFinanzas.getChildren().addAll(lblFin, flowFin);
        }

        // Acciones Rápidas
        VBox secAcciones = new VBox(12);
        Label lblAcc = new Label("Acciones Rápidas del Día");
        lblAcc.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        HBox boxBotones = new HBox(12);
        Button btnGruposAcc = new Button("+ Crear / Ver Grupos");
        btnGruposAcc.getStyleClass().add("btn-secondary");
        btnGruposAcc.setOnAction(e -> rootLayout.setCenter(new GruposView().crearVista(stage)));

        Button btnInscribir = new Button("+ Registrar Alumno");
        btnInscribir.getStyleClass().add("btn-primary");
        btnInscribir.setOnAction(e -> rootLayout.setCenter(new ParticipantesView().crearVista(stage)));

        Button btnCobrar = new Button("Cobrar Mensualidad");
        btnCobrar.getStyleClass().add("btn-success");
        btnCobrar.setOnAction(e -> rootLayout.setCenter(new CobranzasView().crearVista(stage)));

        Button btnArqueo = new Button("Verificar Caja Diaria");
        btnArqueo.getStyleClass().add("btn-secondary");
        btnArqueo.setOnAction(e -> rootLayout.setCenter(new CajaView().crearVista(stage)));

        boxBotones.getChildren().addAll(btnGruposAcc, btnInscribir, btnCobrar, btnArqueo);

        if (AppSession.isAdmin()) {
            Button btnNominaAcc = new Button("💵 Pagar Nómina");
            btnNominaAcc.getStyleClass().add("btn-secondary");
            btnNominaAcc.setOnAction(e -> rootLayout.setCenter(new NominaView().crearVista(stage)));

            Button btnUsuariosAcc = new Button("🔐 Usuarios");
            btnUsuariosAcc.getStyleClass().add("btn-secondary");
            btnUsuariosAcc.setOnAction(e -> rootLayout.setCenter(new UsuariosView().crearVista(stage)));

            boxBotones.getChildren().addAll(btnNominaAcc, btnUsuariosAcc);
        }

        secAcciones.getChildren().addAll(lblAcc, boxBotones);

        content.getChildren().addAll(lblTitulo, flowTarjetas, secFinanzas, secAcciones);
        scroll.setContent(content);
        return scroll;
    }

    private VBox crearTarjeta(String titulo, String valor, String colorHex) {
        VBox card = new VBox(6);
        card.getStyleClass().add("metric-card");
        card.setPrefWidth(240);

        Label lblTit = new Label(titulo);
        lblTit.getStyleClass().add("metric-title");

        Label lblVal = new Label(valor);
        lblVal.getStyleClass().add("metric-value");
        lblVal.setStyle("-fx-text-fill: " + colorHex + ";");

        card.getChildren().addAll(lblTit, lblVal);
        return card;
    }

    private void ejecutarRespaldo() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleccione carpeta para guardar el respaldo");
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            try {
                File backup = BackupService.generarRespaldo(dir);
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Respaldo generado con éxito en:\n" + backup.getAbsolutePath());
                a.showAndWait();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Error al generar respaldo: " + ex.getMessage());
                a.showAndWait();
            }
        }
    }
}
