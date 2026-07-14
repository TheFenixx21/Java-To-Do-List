package com.mitodolist;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage ventana) throws Exception {
        URL archivoFxml = getClass().getResource("/com/mitodolist/VentanaPrincipal.fxml");
        
        if (archivoFxml == null) {
            System.out.println("¡ERROR! No se encontró el archivo FXML en la carpeta resources.");
            return;
        }

        Parent raiz = FXMLLoader.load(archivoFxml);
        
        Scene escena = new Scene(raiz);
        escena.setFill(javafx.scene.paint.Color.web("#0F0F0F"));
        escena.getStylesheets().add(getClass().getResource("/com/mitodolist/estilos.css").toExternalForm());

        ventana.setTitle("Mi TodoList V1.0.0e");
        ventana.setScene(escena);
        
        ventana.setMinWidth(1280);
        ventana.setMinHeight(720);
        
        ventana.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}