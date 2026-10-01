package com.academia;

import com.academia.ui.LoginView;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        new LoginView(primaryStage).mostrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
