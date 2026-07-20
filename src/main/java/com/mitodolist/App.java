package com.mitodolist;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    // 1. Guardamos una referencia estática de la ventana para poder cambiar de escena en cualquier momento
    private static Stage escenarioPrincipal;

   @Override
    public void start(Stage ventana) throws Exception {
        escenarioPrincipal = ventana;

        // --- 1. CICLO DE INICIO (AUTO-RESTAURACIÓN) ---
        GestorBaseDatos.restaurarBackupSiEsNecesario();        
        GestorBaseDatos.inicializarEstructura();
        GestorBaseDatos.migrarDatosAntiguos();

        // --- 2. EL GUARDIA DE TRÁFICO (ENRUTAMIENTO V4.0.0e) ---
        boolean tieneUsuario = GestorBaseDatos.existeUsuarioRegistrado();
        boolean recordarSesion = GestorBaseDatos.isSesionRecordada();

        if (tieneUsuario && recordarSesion) {
            cambiarEscena("VentanaPrincipal.fxml", "Mi TodoList V4.0.0e");
        } else {
            cambiarEscena("VentanaLogin.fxml", "Acceso - Mi TodoList");
        }
        
        ventana.setMinWidth(1280);
        ventana.setMinHeight(720);
        
        // --- 3. CICLO DE CIERRE (BACKUP AL SALIR) ---
        ventana.setOnCloseRequest(evento -> {
            System.out.println("🔒 Cerrando aplicación... Generando backup final de la sesión.");
            GestorBaseDatos.realizarBackup();
        });

        ventana.show();
    }

    /**
     * 3. MÉTODO MAESTRO DE NAVEGACIÓN
     * Permite a cualquier controlador del programa cambiar la pantalla instantáneamente.
     */
    public static void cambiarEscena(String nombreFxml, String titulo) {
        try {
            URL archivoFxml = App.class.getResource("/com/mitodolist/" + nombreFxml);
            
            if (archivoFxml == null) {
                System.out.println("¡ERROR! No se encontró el archivo FXML: " + nombreFxml);
                return;
            }

            Parent raiz = FXMLLoader.load(archivoFxml);
            Scene escena = new Scene(raiz);
            
            // Mantenemos el fondo oscuro y tus estilos globales
            escena.setFill(javafx.scene.paint.Color.web("#0F0F0F"));
            escena.getStylesheets().add(App.class.getResource("/com/mitodolist/estilos.css").toExternalForm());

            // Actualizamos la ventana actual con la nueva escena
            escenarioPrincipal.setTitle(titulo);
            escenarioPrincipal.setScene(escena);

        } catch (IOException e) {
            System.out.println("Error grave al intentar cargar la escena " + nombreFxml + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}