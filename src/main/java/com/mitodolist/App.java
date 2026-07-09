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
        // Buscamos el archivo de diseño que creaste
        URL archivoFxml = getClass().getResource("/com/mitodolist/VentanaPrincipal.fxml");
        
        if (archivoFxml == null) {
            System.out.println("¡ERROR! No se encontró el archivo FXML en la carpeta resources.");
            return;
        }

        // Cargamos el diseño
        Parent raiz = FXMLLoader.load(archivoFxml);
        
        // Creamos la escena y le quitamos el fondo blanco de carga
        Scene escena = new Scene(raiz);
        escena.setFill(javafx.scene.paint.Color.web("#0F0F0F"));

        // Configuramos la ventana
        ventana.setTitle("Mi TodoList V1.0.0");
        ventana.setScene(escena);
        
        // Evitamos que la ventana se haga más pequeña de lo que diseñaste
        ventana.setMinWidth(1280);
        ventana.setMinHeight(720);
        
        ventana.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}