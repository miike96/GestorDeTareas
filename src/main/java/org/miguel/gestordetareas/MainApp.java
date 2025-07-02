package org.miguel.gestordetareas;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL fxmlLocation = getClass().getResource("/org/miguel/gestordetareas/view/MainView.fxml");
        System.out.println("FXML Location: " + fxmlLocation);

        if (fxmlLocation == null) {
            throw new RuntimeException("No se encontró el archivo MainView.fxml en /org/miguel/gestordetareas/view/");
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Scene scene = new Scene(loader.load());

        stage.setTitle("Gestor de Tareas - Miguel López");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
