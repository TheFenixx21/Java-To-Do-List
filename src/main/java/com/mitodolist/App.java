package com.mitodolist;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {

    private static Stage escenarioPrincipal;
    
    // Variables para calcular el movimiento del mouse (Drag & Drop)
    private static double xOffset = 0;
    private static double yOffset = 0;
    // Variables para el Auto-Bloqueo por Inactividad
    private static long ultimoMovimiento = System.currentTimeMillis();
    private static Thread hiloMonitor;

    @Override
    public void start(Stage ventana) throws Exception {
        escenarioPrincipal = ventana;

        // --- 1. V5.0.0e: DESTRUIR LA BARRA DE WINDOWS ---
        ventana.initStyle(javafx.stage.StageStyle.UNDECORATED); 

        // --- 2. CICLO DE INICIO ---
        GestorBaseDatos.restaurarBackupSiEsNecesario();        
        GestorBaseDatos.inicializarEstructura();
        GestorConfiguracion.inicializarTabla();
        GestorBaseDatos.migrarDatosAntiguos();

        // --- 3. ENRUTAMIENTO ---
        boolean tieneUsuario = GestorBaseDatos.existeUsuarioRegistrado();
        boolean recordarSesion = GestorBaseDatos.isSesionRecordada();

        if (tieneUsuario && recordarSesion) {
            cambiarEscena("VentanaPrincipal.fxml", "Mi TodoList V7.0.0e");
        } else {
            cambiarEscena("VentanaLogin.fxml", "Acceso - Mi TodoList");
        }
        
        ventana.setMinWidth(900);
        ventana.setMinHeight(600);
        
       // --- 4. CICLO DE CIERRE Y SEGUNDO PLANO (V7.0.0e) ---
        // 🚨 CRÍTICO: Evitamos que la Máquina Virtual muera al ocultar la ventana
        javafx.application.Platform.setImplicitExit(false); 
        
        ventana.setOnCloseRequest(evento -> {
            evento.consume(); // Interceptamos y anulamos la orden de destrucción
            escenarioPrincipal.hide(); // Ocultamos la ventana en lugar de cerrarla
        });

        configurarSystemTray(); // Levantamos el ícono en la barra de Windows
        configurarAtajosGlobales();
        iniciarMonitorInactividad();
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

            // Leemos la configuración de Apariencia global
            Configuracion config = GestorConfiguracion.cargarConfiguracion();

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

            // Botón Cerrar (V7: Ahora oculta la app en lugar de matarla)
            javafx.scene.control.Button btnCerrar = new javafx.scene.control.Button("X");
            estilizarBotonBarra(btnCerrar, "#E81123");
            btnCerrar.setOnAction(e -> escenarioPrincipal.hide());

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

            // ==============================================================
            // 🎨 INYECCIÓN DINÁMICA DEL TEMA Y COLOR DE ACENTO
            // ==============================================================
            // Usamos la variable 'config' que ya cargamos al principio del método
            
            // 1. Inyectamos la variable CSS global con el color del usuario
            contenedorMaestro.setStyle("-color-acento: " + config.getColorAcento() + ";");
            
            // 2. Si el usuario eligió Modo Claro, encendemos la clase maestra
            if (config.isTemaClaro()) {
                contenedorMaestro.getStyleClass().add("tema-claro");
            }
            // ==============================================================
            // 🚨 NUEVO: Sensores globales de actividad para el Auto-Bloqueo
            escena.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, e -> ultimoMovimiento = System.currentTimeMillis());
            escena.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> ultimoMovimiento = System.currentTimeMillis());
            escena.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> ultimoMovimiento = System.currentTimeMillis());

            escenarioPrincipal.setScene(escena);
            escenarioPrincipal.show();

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

    // --- NUEVO MOTOR DE REDIMENSIÓN 360° (Todos los bordes y esquinas) ---
    private static void habilitarRedimension(Stage stage, Parent root) {
        final int margen = 8; // Aumentamos un poquito el margen para que sea más fácil agarrarlo
        
        // Usamos un arreglo para guardar el "estado inicial" del clic sin contaminar las variables globales
        final double[] estadoResize = new double[6]; // [inicioX, inicioY, inicioAncho, inicioAlto, inicioStageX, inicioStageY]

        // 1. Radar Visual: Cambia el icono del cursor según el borde
        root.setOnMouseMoved(event -> {
            if (stage.isMaximized()) {
                root.setCursor(javafx.scene.Cursor.DEFAULT);
                return;
            }
            
            double x = event.getX();
            double y = event.getY();
            double ancho = stage.getWidth();
            double alto = stage.getHeight();

            boolean bordeIzquierdo = x <= margen;
            boolean bordeDerecho = x >= ancho - margen;
            boolean bordeSuperior = y <= margen;
            boolean bordeInferior = y >= alto - margen;

            if (bordeIzquierdo && bordeSuperior) root.setCursor(javafx.scene.Cursor.NW_RESIZE);
            else if (bordeDerecho && bordeSuperior) root.setCursor(javafx.scene.Cursor.NE_RESIZE);
            else if (bordeIzquierdo && bordeInferior) root.setCursor(javafx.scene.Cursor.SW_RESIZE);
            else if (bordeDerecho && bordeInferior) root.setCursor(javafx.scene.Cursor.SE_RESIZE);
            else if (bordeIzquierdo) root.setCursor(javafx.scene.Cursor.W_RESIZE);
            else if (bordeDerecho) root.setCursor(javafx.scene.Cursor.E_RESIZE);
            else if (bordeSuperior) root.setCursor(javafx.scene.Cursor.N_RESIZE);
            else if (bordeInferior) root.setCursor(javafx.scene.Cursor.S_RESIZE);
            else root.setCursor(javafx.scene.Cursor.DEFAULT);
        });

        // 2. Anclaje: Guardamos las coordenadas exactas de la pantalla en el momento en que se hace clic
        root.setOnMousePressed(event -> {
            estadoResize[0] = event.getScreenX();
            estadoResize[1] = event.getScreenY();
            estadoResize[2] = stage.getWidth();
            estadoResize[3] = stage.getHeight();
            estadoResize[4] = stage.getX();
            estadoResize[5] = stage.getY();
        });

        // 3. Motor Físico: Estira y mueve la ventana en tiempo real
        root.setOnMouseDragged(event -> {
            if (stage.isMaximized()) return;
            
            javafx.scene.Cursor cursor = root.getCursor();
            if (cursor == javafx.scene.Cursor.DEFAULT) return;
            
            double difX = event.getScreenX() - estadoResize[0];
            double difY = event.getScreenY() - estadoResize[1];
            
            double minAncho = 900;
            double minAlto = 600;

            // Arrastre hacia la Derecha
            if (cursor == javafx.scene.Cursor.E_RESIZE || cursor == javafx.scene.Cursor.NE_RESIZE || cursor == javafx.scene.Cursor.SE_RESIZE) {
                stage.setWidth(Math.max(minAncho, estadoResize[2] + difX));
            }
            
            // Arrastre hacia Abajo
            if (cursor == javafx.scene.Cursor.S_RESIZE || cursor == javafx.scene.Cursor.SW_RESIZE || cursor == javafx.scene.Cursor.SE_RESIZE) {
                stage.setHeight(Math.max(minAlto, estadoResize[3] + difY));
            }

            // Arrastre hacia la Izquierda (Requiere mover la ventana y encoger el ancho al mismo tiempo)
            if (cursor == javafx.scene.Cursor.W_RESIZE || cursor == javafx.scene.Cursor.NW_RESIZE || cursor == javafx.scene.Cursor.SW_RESIZE) {
                double nuevoAncho = Math.max(minAncho, estadoResize[2] - difX);
                if (nuevoAncho > minAncho) {
                    stage.setX(estadoResize[4] + difX);
                    stage.setWidth(nuevoAncho);
                }
            }
            
            // Arrastre hacia Arriba (Requiere mover la ventana y encoger el alto al mismo tiempo)
            if (cursor == javafx.scene.Cursor.N_RESIZE || cursor == javafx.scene.Cursor.NW_RESIZE || cursor == javafx.scene.Cursor.NE_RESIZE) {
                double nuevoAlto = Math.max(minAlto, estadoResize[3] - difY);
                if (nuevoAlto > minAlto) {
                    stage.setY(estadoResize[5] + difY);
                    stage.setHeight(nuevoAlto);
                }
            }
        });
    }

    // ==========================================
    // Función main obsoleta, pero necesaria para empaquetar el JAR ejecutable
    // ==========================================
    public static void main(String[] args) {
        launch(args);
    }

    private static void iniciarMonitorInactividad() {
        if (hiloMonitor != null) return; // Evita crear múltiples monitores
        
        hiloMonitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); // Revisa cada 5 segundos para no consumir recursos
                    
                    // Solo revisa si hay un usuario dentro (logueado)
                    if (GestorBaseDatos.idUsuarioActual != -1) {
                        Configuracion config = GestorConfiguracion.cargarConfiguracion();
                        int minutosBloqueo = config.getBloqueoInactividad();
                        
                        if (minutosBloqueo > 0) {
                            long limiteMs = minutosBloqueo * 60 * 1000L;
                            long tiempoInactivo = System.currentTimeMillis() - ultimoMovimiento;
                            
                            // Si superaste el límite de tiempo...
                            if (tiempoInactivo > limiteMs) {
                                
                                // El cambio de escena DEBE hacerse en el hilo principal de JavaFX
                                javafx.application.Platform.runLater(() -> {
                                    System.out.println("🔒 Tiempo de inactividad superado. Bloqueando sesión...");
                                    GestorBaseDatos.idUsuarioActual = -1; // Cierre de sesión lógico
                                    cambiarEscena("VentanaLogin.fxml", "Acceso - Mi TodoList");
                                });
                                
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        hiloMonitor.setDaemon(true);
        hiloMonitor.start();
    }

    // ==========================================
    // ⚙️ MOTOR DEL SYSTEM TRAY (SEGUNDO PLANO)
    // ==========================================
    private static void configurarSystemTray() {
        if (!java.awt.SystemTray.isSupported()) {
            System.out.println("El sistema operativo no soporta System Tray.");
            return;
        }

        try {
            java.awt.SystemTray bandeja = java.awt.SystemTray.getSystemTray();
            
            // Creamos un ícono básico programático (Evitamos NullPointerExceptions si no hay imagen física)
            java.awt.image.BufferedImage imagenIcono = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = imagenIcono.createGraphics();
            g.setColor(java.awt.Color.decode("#C2185B")); // Color base
            g.fillRect(0, 0, 16, 16);
            g.setColor(java.awt.Color.WHITE);
            g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            g.drawString("M", 4, 13);
            g.dispose();

            // Si tienes un archivo .png, puedes usar esto en su lugar:
            // java.awt.Image imagenIcono = java.awt.Toolkit.getDefaultToolkit().getImage(App.class.getResource("/com/mitodolist/icono.png"));

            java.awt.TrayIcon iconoTray = new java.awt.TrayIcon(imagenIcono, "Mi ToDo List");
            iconoTray.setImageAutoSize(true);

            // Acción 1: Doble clic izquierdo en el ícono para despertar la app
            iconoTray.addActionListener(e -> {
                // AWT y JavaFX corren en hilos distintos. Platform.runLater sincroniza la orden.
                javafx.application.Platform.runLater(() -> escenarioPrincipal.show());
            });

            // Acción 2: Menú de clic derecho
            java.awt.PopupMenu menuClicDerecho = new java.awt.PopupMenu();
            
            java.awt.MenuItem itemAbrir = new java.awt.MenuItem("Abrir Aplicacion");
            itemAbrir.addActionListener(e -> javafx.application.Platform.runLater(() -> escenarioPrincipal.show()));
            
            java.awt.MenuItem itemSalir = new java.awt.MenuItem("Salir Completamente");
            itemSalir.addActionListener(e -> {
                System.out.println("🔒 Apagando sistema desde la bandeja... Generando backup.");
                GestorBaseDatos.realizarBackup();
                System.exit(0);
            });

            menuClicDerecho.add(itemAbrir);
            menuClicDerecho.addSeparator();
            menuClicDerecho.add(itemSalir);

            iconoTray.setPopupMenu(menuClicDerecho);
            bandeja.add(iconoTray);

        } catch (Exception e) {
            System.out.println("Error al configurar el System Tray: " + e.getMessage());
        }
    }

    // ==========================================
    // ⌨️ MOTOR DE ATAJOS GLOBALES (HOTKEYS)
    // ==========================================
    private static void configurarAtajosGlobales() {
        try {
            // 1. Silenciamos el Logger de JNativeHook (por defecto imprime CADA tecla que presionas en la consola)
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false);

            // 2. Registramos el "Gancho" (Hook) en el sistema operativo Windows
            GlobalScreen.registerNativeHook();

            // 3. Añadimos el oyente que estará atento a nuestro comando específico
            GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
                @Override
                public void nativeKeyPressed(NativeKeyEvent e) {
                    // Verificamos si presionaron la tecla 'T' MIENTRAS mantenían 'ALT' presionado
                    if (e.getKeyCode() == NativeKeyEvent.VC_T && (e.getModifiers() & NativeKeyEvent.ALT_MASK) != 0) {
                        
                       // Como estamos en un hilo externo nativo, usamos Platform.runLater para hablar con JavaFX
                        javafx.application.Platform.runLater(() -> {
                            if (escenarioPrincipal != null) {
                                // Si está minimizada en la barra de tareas, la restauramos
                                escenarioPrincipal.setIconified(false);
                                
                                // La mostramos
                                escenarioPrincipal.show();
                                
                                // 🚨 TRUCO AGRESIVO DE WINDOWS (Z-Order Hack)
                                // 1. La declaramos como ventana suprema obligatoria
                                escenarioPrincipal.setAlwaysOnTop(true); 
                                // 2. La empujamos al frente
                                escenarioPrincipal.toFront(); 
                                // 3. Le robamos el foco al teclado de la otra app
                                escenarioPrincipal.requestFocus(); 
                                // 4. Apagamos el estado supremo para que no se quede pegada encima de todo para siempre
                                escenarioPrincipal.setAlwaysOnTop(false); 
                            }
                        });
                    } // <-- FALTABA ESTA LLAVE (Cierra el IF)
                } // <-- FALTABA ESTA LLAVE (Cierra el nativeKeyPressed)

                @Override
                public void nativeKeyReleased(NativeKeyEvent e) {
                    // No necesitamos rastrear cuando suelta la tecla
                }

                @Override
                public void nativeKeyTyped(NativeKeyEvent e) {
                    // No necesitamos rastrear tipeo
                }
            });
            
            System.out.println("⌨️ Atajos Globales listos. Presiona [Alt + T] en cualquier momento.");

        } catch (NativeHookException e) {
            System.out.println("Hubo un problema al registrar el teclado global: " + e.getMessage());
        }
    }
}