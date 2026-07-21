package com.mitodolist;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    private static Stage escenarioPrincipal;
    
    // Variables para calcular el movimiento del mouse (Drag & Drop)
    private static double xOffset = 0;
    private static double yOffset = 0;

    @Override
    public void start(Stage ventana) throws Exception {
        escenarioPrincipal = ventana;

        // --- 1. V5.0.0e: DESTRUIR LA BARRA DE WINDOWS ---
        ventana.initStyle(javafx.stage.StageStyle.UNDECORATED); 

        // --- 2. CICLO DE INICIO ---
        GestorBaseDatos.restaurarBackupSiEsNecesario();        
        GestorBaseDatos.inicializarEstructura();
        GestorBaseDatos.migrarDatosAntiguos();

        // --- 3. ENRUTAMIENTO ---
        boolean tieneUsuario = GestorBaseDatos.existeUsuarioRegistrado();
        boolean recordarSesion = GestorBaseDatos.isSesionRecordada();

        if (tieneUsuario && recordarSesion) {
            cambiarEscena("VentanaPrincipal.fxml", "Mi TodoList V5.0.0e");
        } else {
            cambiarEscena("VentanaLogin.fxml", "Acceso - Mi TodoList");
        }
        
        ventana.setMinWidth(900);
        ventana.setMinHeight(600);
        
        // --- 4. CICLO DE CIERRE (SEGURIDAD) ---
        ventana.setOnCloseRequest(evento -> {
            System.out.println("🔒 Cerrando aplicación... Generando backup final de la sesión.");
            GestorBaseDatos.realizarBackup();
        });

        ventana.show();
    }

    public static void cambiarEscena(String nombreFxml, String titulo) {
        try {
            URL archivoFxml = App.class.getResource("/com/mitodolist/" + nombreFxml);
            if (archivoFxml == null) return;
            Parent raiz = FXMLLoader.load(archivoFxml);

            // ==============================================================
            // INYECCIÓN DINÁMICA DE LA BARRA SUPERIOR CUSTOM
            // ==============================================================
            
            javafx.scene.layout.HBox barraSuperior = new javafx.scene.layout.HBox();
            barraSuperior.setPrefHeight(35);
            barraSuperior.setStyle("-fx-background-color: #121212; -fx-padding: 5 15 5 15; -fx-alignment: center-right; -fx-spacing: 12;");

            javafx.scene.control.Label lblTitulo = new javafx.scene.control.Label(titulo);
            lblTitulo.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 14px; -fx-font-weight: bold;");
            javafx.scene.layout.HBox.setHgrow(lblTitulo, javafx.scene.layout.Priority.ALWAYS);
            lblTitulo.setMaxWidth(Double.MAX_VALUE); 

            // Botón Minimizar
            javafx.scene.control.Button btnMinimizar = new javafx.scene.control.Button("—");
            estilizarBotonBarra(btnMinimizar, "#333333");
            btnMinimizar.setOnAction(e -> escenarioPrincipal.setIconified(true));

            // NUEVO: Botón Maximizar / Restaurar
            javafx.scene.control.Button btnMaximizar = new javafx.scene.control.Button("⬜");
            estilizarBotonBarra(btnMaximizar, "#333333");
            btnMaximizar.setOnAction(e -> {
                boolean estaMaximizada = escenarioPrincipal.isMaximized();
                escenarioPrincipal.setMaximized(!estaMaximizada);
                btnMaximizar.setText(estaMaximizada ? "⬜" : "❐");
            });

            // Botón Cerrar
            javafx.scene.control.Button btnCerrar = new javafx.scene.control.Button("X");
            estilizarBotonBarra(btnCerrar, "#E81123");
            btnCerrar.setOnAction(e -> {
                GestorBaseDatos.realizarBackup();
                System.exit(0);
            });

            barraSuperior.getChildren().addAll(lblTitulo, btnMinimizar, btnMaximizar, btnCerrar);

            // Motor Física: Lógica para arrastrar la ventana
            barraSuperior.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            
            barraSuperior.setOnMouseDragged(event -> {
                if (escenarioPrincipal.isMaximized()) {
                    // UX Premium: Si está maximizada y la arrastras, se restaura automáticamente
                    escenarioPrincipal.setMaximized(false);
                    btnMaximizar.setText("⬜");
                    
                    // Recalculamos el agarre del mouse para que no salte bruscamente
                    xOffset = escenarioPrincipal.getWidth() / 2;
                }
                escenarioPrincipal.setX(event.getScreenX() - xOffset);
                escenarioPrincipal.setY(event.getScreenY() - yOffset);
            });

            javafx.scene.layout.BorderPane contenedorMaestro = new javafx.scene.layout.BorderPane();
            contenedorMaestro.setStyle("-fx-background-color: #0F0F0F; -fx-border-color: #333333; -fx-border-width: 1;"); 
            contenedorMaestro.setTop(barraSuperior); 
            
            // --- FIX DE PANTALLA COMPLETA Y REDIMENSIÓN ---
            if (raiz instanceof javafx.scene.layout.Region) {
                javafx.scene.layout.Region region = (javafx.scene.layout.Region) raiz;
                region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); // Permitir crecer al infinito
                region.setMinSize(0, 0); // NUEVO: Permitir encogerse sin resistencia
            }
            
            contenedorMaestro.setCenter(raiz); // Ponemos tu FXML (Login o Principal) en el centro

            // --- NUEVO: Habilitar sensores de borde para redimensionar ---
            habilitarRedimension(escenarioPrincipal, contenedorMaestro);

            Scene escena = new Scene(contenedorMaestro);
            escena.getStylesheets().add(App.class.getResource("/com/mitodolist/estilos.css").toExternalForm());

            escenarioPrincipal.setTitle(titulo);
            escenarioPrincipal.setScene(escena);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método auxiliar para no repetir código de diseño en los botones
    private static void estilizarBotonBarra(javafx.scene.control.Button btn, String colorHover) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #AAAAAA; -fx-font-weight: bold; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + colorHover + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #AAAAAA; -fx-font-weight: bold; -fx-cursor: hand;"));
    }

    // --- NUEVO MOTOR DE REDIMENSIÓN (Borde Derecho e Inferior) ---
    private static void habilitarRedimension(Stage stage, Parent root) {
        final int margen = 6; // Grosor del "imán" del borde
        
        root.setOnMouseMoved(event -> {
            if (stage.isMaximized()) {
                root.setCursor(javafx.scene.Cursor.DEFAULT);
                return;
            }
            boolean esDerecha = event.getX() > stage.getWidth() - margen;
            boolean esAbajo = event.getY() > stage.getHeight() - margen;
            
            if (esDerecha && esAbajo) root.setCursor(javafx.scene.Cursor.SE_RESIZE);
            else if (esDerecha) root.setCursor(javafx.scene.Cursor.E_RESIZE);
            else if (esAbajo) root.setCursor(javafx.scene.Cursor.S_RESIZE);
            else root.setCursor(javafx.scene.Cursor.DEFAULT);
        });

        root.setOnMouseDragged(event -> {
            if (stage.isMaximized()) return;
            javafx.scene.Cursor cursor = root.getCursor();
            
            if (cursor == javafx.scene.Cursor.SE_RESIZE || cursor == javafx.scene.Cursor.E_RESIZE) {
                stage.setWidth(Math.max(900, event.getX())); // 900 es el ancho mínimo
            }
            if (cursor == javafx.scene.Cursor.SE_RESIZE || cursor == javafx.scene.Cursor.S_RESIZE) {
                stage.setHeight(Math.max(600, event.getY())); // 600 es el alto mínimo
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}