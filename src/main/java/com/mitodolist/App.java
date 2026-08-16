package com.mitodolist;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {

    private static Stage escenarioPrincipal;
    
    // Variables para calcular el movimiento del mouse
    private static double xOffset = 0;
    private static double yOffset = 0;
    
    // Variables para el Auto-Bloqueo
    private static long ultimoMovimiento = System.currentTimeMillis();
    private static Thread hiloMonitor;
    
    // 🚨 VARIABLES DE DEFENSA (SIN PUERTOS DE RED = SIN FIREWALL)
    private static boolean trayConfigurado = false; 
    private static FileChannel canalLock;
    private static FileLock lockApp;
    private static final String CARPETA_APP = System.getenv("APPDATA") + File.separator + "MiTodoList";

    @Override
    public void start(Stage ventana) {
        try {
            // 1. Evita que JavaFX muera si la ventana se oculta
            javafx.application.Platform.setImplicitExit(false); 
            
            // 2. 🚨 EL ESCUDO ANTI-CLONES SILENCIOSO (Zero Firewall)
            verificarInstanciaUnica();

            escenarioPrincipal = ventana;
            ventana.initStyle(javafx.stage.StageStyle.UNDECORATED); 

            GestorBaseDatos.restaurarBackupSiEsNecesario();        
            GestorBaseDatos.inicializarEstructura();
            GestorConfiguracion.inicializarTabla();
            GestorBaseDatos.migrarDatosAntiguos();

            boolean tieneUsuario = GestorBaseDatos.existeUsuarioRegistrado();
            boolean recordarSesion = GestorBaseDatos.isSesionRecordada();

            if (tieneUsuario && recordarSesion) {
                cambiarEscena("VentanaPrincipal.fxml", "Mi TodoList V8.0.0e");
            } else {
                cambiarEscena("VentanaLogin.fxml", "Acceso - Mi TodoList");
            }
            
            ventana.setMinWidth(900);
            ventana.setMinHeight(600);
            
            ventana.setOnCloseRequest(evento -> {
                evento.consume(); 
                escenarioPrincipal.hide(); 
            });

            configurarAtajosGlobales();
            iniciarMonitorInactividad();

            // ==========================================
            // 🌐 INICIAR SERVIDOR P2P AUTOMÁTICO 
            // ==========================================
            Configuracion configRed = GestorConfiguracion.cargarConfiguracion();
            if (configRed.isSincronizacionAutomatica()) {
                ServidorSync servidorFondo = new ServidorSync(true); // true = Modo Automático (Silencioso)
                servidorFondo.iniciarServidor();
            }
            
        } catch (Throwable t) { // 🚨 ATRAPA CUALQUIER MUERTE SILENCIOSA
            javax.swing.JOptionPane.showMessageDialog(null, 
                "Error Crítico al iniciar la aplicación:\n" + t.toString(), 
                "Fallo del Sistema", 
                javax.swing.JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public static void cambiarEscena(String nombreFxml, String titulo) {
        try {
            URL archivoFxml = App.class.getResource("/com/mitodolist/" + nombreFxml);
            if (archivoFxml == null) {
                throw new RuntimeException("No se encontró el FXML: " + nombreFxml);
            }
            Parent raiz = FXMLLoader.load(archivoFxml);

            javafx.scene.layout.HBox barraSuperior = new javafx.scene.layout.HBox();
            barraSuperior.setPrefHeight(35);
            barraSuperior.setStyle("-fx-background-color: #121212; -fx-padding: 5 15 5 15; -fx-alignment: center-right; -fx-spacing: 12;");

            javafx.scene.control.Label lblTitulo = new javafx.scene.control.Label(titulo);
            lblTitulo.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 14px; -fx-font-weight: bold;");
            javafx.scene.layout.HBox.setHgrow(lblTitulo, javafx.scene.layout.Priority.ALWAYS);
            lblTitulo.setMaxWidth(Double.MAX_VALUE);

            Configuracion config = GestorConfiguracion.cargarConfiguracion();

            javafx.scene.control.Button btnMinimizar = new javafx.scene.control.Button("—");
            estilizarBotonBarra(btnMinimizar, "#333333");
            btnMinimizar.setOnAction(e -> escenarioPrincipal.setIconified(true));

            javafx.scene.control.Button btnMaximizar = new javafx.scene.control.Button("⬜");
            estilizarBotonBarra(btnMaximizar, "#333333");
            btnMaximizar.setOnAction(e -> {
                boolean estaMaximizada = escenarioPrincipal.isMaximized();
                escenarioPrincipal.setMaximized(!estaMaximizada);
                btnMaximizar.setText(estaMaximizada ? "⬜" : "❐");
            });

            javafx.scene.control.Button btnCerrar = new javafx.scene.control.Button("X");
            estilizarBotonBarra(btnCerrar, "#E81123");
            btnCerrar.setOnAction(e -> escenarioPrincipal.hide());

            barraSuperior.getChildren().addAll(lblTitulo, btnMinimizar, btnMaximizar, btnCerrar);

            barraSuperior.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            
            barraSuperior.setOnMouseDragged(event -> {
                if (escenarioPrincipal.isMaximized()) {
                    escenarioPrincipal.setMaximized(false);
                    btnMaximizar.setText("⬜");
                    xOffset = escenarioPrincipal.getWidth() / 2;
                }
                escenarioPrincipal.setX(event.getScreenX() - xOffset);
                escenarioPrincipal.setY(event.getScreenY() - yOffset);
            });

            javafx.scene.layout.BorderPane contenedorMaestro = new javafx.scene.layout.BorderPane();
            contenedorMaestro.setStyle("-fx-background-color: #0F0F0F; -fx-border-color: #333333; -fx-border-width: 1;"); 
            contenedorMaestro.setTop(barraSuperior); 
            
            if (raiz instanceof javafx.scene.layout.Region) {
                javafx.scene.layout.Region region = (javafx.scene.layout.Region) raiz;
                region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); 
                region.setMinSize(0, 0); 
            }
            
            contenedorMaestro.setCenter(raiz); 

            habilitarRedimension(escenarioPrincipal, contenedorMaestro);

            Scene escena = new Scene(contenedorMaestro);
            escena.getStylesheets().add(App.class.getResource("/com/mitodolist/estilos.css").toExternalForm());

            contenedorMaestro.setStyle("-color-acento: " + config.getColorAcento() + ";");
            
            if (config.isTemaClaro()) {
                contenedorMaestro.getStyleClass().add("tema-claro");
            }

            escena.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, e -> ultimoMovimiento = System.currentTimeMillis());
            escena.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> ultimoMovimiento = System.currentTimeMillis());
            escena.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> ultimoMovimiento = System.currentTimeMillis());

            escenarioPrincipal.setScene(escena);

            if (!trayConfigurado) {
                trayConfigurado = true;
                java.awt.EventQueue.invokeLater(() -> configurarSystemTray());
            }
            
            restaurarVentana();

        } catch (Throwable t) { // 🚨 ATRAPA CUALQUIER MUERTE SILENCIOSA DE INTERFAZ
            javax.swing.JOptionPane.showMessageDialog(null, 
                "Error Crítico al cambiar de pantalla:\n" + t.toString(), 
                "Fallo del Sistema", 
                javax.swing.JOptionPane.ERROR_MESSAGE);
            t.printStackTrace();
        }
    }

    // ==========================================
    // 🛡️ MOTOR ANTI-CLONES (ARCHIVOS DE BLOQUEO)
    // Cero alertas de Firewall
    // ==========================================
    private static void verificarInstanciaUnica() throws Exception {
        File carpeta = new File(CARPETA_APP);
        if (!carpeta.exists()) carpeta.mkdirs();

        File archivoLock = new File(carpeta, "app.lock");
        canalLock = new RandomAccessFile(archivoLock, "rw").getChannel();
        lockApp = canalLock.tryLock();

        if (lockApp == null) {
            // 🚨 ¡Otra instancia ya está corriendo!
            File archivoSenal = new File(carpeta, "wakeup.signal");
            archivoSenal.createNewFile();
            System.exit(0); // Nos suicidamos inmediatamente
        }

        // Si logramos bloquear el archivo, somos la App ORIGINAL.
        iniciarVigilanteDeSenal(carpeta);
    }

    private static void iniciarVigilanteDeSenal(File carpeta) {
        Thread hiloSenal = new Thread(() -> {
            File archivoSenal = new File(carpeta, "wakeup.signal");
            while (true) {
                try {
                    // Si el clon creó el archivo "wakeup.signal"...
                    if (archivoSenal.exists()) {
                        archivoSenal.delete(); // Lo consumimos
                        restaurarVentana();    // Despertamos la ventana a la fuerza
                    }
                    Thread.sleep(500); // Revisamos medio segundo
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        hiloSenal.setDaemon(true);
        hiloSenal.start();
    }

    // ==========================================
    // 🛡️ MOTOR DE RESURRECCIÓN DE VENTANA
    // ==========================================
    public static void restaurarVentana() {
        javafx.application.Platform.runLater(() -> {
            try {
                if (escenarioPrincipal != null) {
                    if (!escenarioPrincipal.isShowing()) {
                        escenarioPrincipal.show();
                    }
                    escenarioPrincipal.setIconified(false);
                    escenarioPrincipal.setAlwaysOnTop(true);
                    escenarioPrincipal.toFront();
                    escenarioPrincipal.requestFocus();
                    escenarioPrincipal.setAlwaysOnTop(false);
                }
            } catch (Throwable t) {
                System.out.println("Error al intentar restaurar la ventana: " + t.getMessage());
            }
        });
    }

    private static void estilizarBotonBarra(javafx.scene.control.Button btn, String colorHover) {
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #AAAAAA; -fx-font-weight: bold; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + colorHover + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #AAAAAA; -fx-font-weight: bold; -fx-cursor: hand;"));
    }

    private static void habilitarRedimension(Stage stage, Parent root) {
        final int margen = 8; 
        final double[] estadoResize = new double[6]; 

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

        root.setOnMousePressed(event -> {
            estadoResize[0] = event.getScreenX();
            estadoResize[1] = event.getScreenY();
            estadoResize[2] = stage.getWidth();
            estadoResize[3] = stage.getHeight();
            estadoResize[4] = stage.getX();
            estadoResize[5] = stage.getY();
        });

        root.setOnMouseDragged(event -> {
            if (stage.isMaximized()) return;
            
            javafx.scene.Cursor cursor = root.getCursor();
            if (cursor == javafx.scene.Cursor.DEFAULT) return;
            
            double difX = event.getScreenX() - estadoResize[0];
            double difY = event.getScreenY() - estadoResize[1];
            
            double minAncho = 900;
            double minAlto = 600;

            if (cursor == javafx.scene.Cursor.E_RESIZE || cursor == javafx.scene.Cursor.NE_RESIZE || cursor == javafx.scene.Cursor.SE_RESIZE) {
                stage.setWidth(Math.max(minAncho, estadoResize[2] + difX));
            }
            
            if (cursor == javafx.scene.Cursor.S_RESIZE || cursor == javafx.scene.Cursor.SW_RESIZE || cursor == javafx.scene.Cursor.SE_RESIZE) {
                stage.setHeight(Math.max(minAlto, estadoResize[3] + difY));
            }

            if (cursor == javafx.scene.Cursor.W_RESIZE || cursor == javafx.scene.Cursor.NW_RESIZE || cursor == javafx.scene.Cursor.SW_RESIZE) {
                double nuevoAncho = Math.max(minAncho, estadoResize[2] - difX);
                if (nuevoAncho > minAncho) {
                    stage.setX(estadoResize[4] + difX);
                    stage.setWidth(nuevoAncho);
                }
            }
            
            if (cursor == javafx.scene.Cursor.N_RESIZE || cursor == javafx.scene.Cursor.NW_RESIZE || cursor == javafx.scene.Cursor.NE_RESIZE) {
                double nuevoAlto = Math.max(minAlto, estadoResize[3] - difY);
                if (nuevoAlto > minAlto) {
                    stage.setY(estadoResize[5] + difY);
                    stage.setHeight(nuevoAlto);
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static void iniciarMonitorInactividad() {
        if (hiloMonitor != null) return; 
        
        hiloMonitor = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); 
                    
                    if (GestorBaseDatos.idUsuarioActual != -1) {
                        Configuracion config = GestorConfiguracion.cargarConfiguracion();
                        int minutosBloqueo = config.getBloqueoInactividad();
                        
                        if (minutosBloqueo > 0) {
                            long limiteMs = minutosBloqueo * 60 * 1000L;
                            long tiempoInactivo = System.currentTimeMillis() - ultimoMovimiento;
                            
                            if (tiempoInactivo > limiteMs) {
                                javafx.application.Platform.runLater(() -> {
                                    System.out.println("🔒 Tiempo de inactividad superado. Bloqueando sesión...");
                                    GestorBaseDatos.idUsuarioActual = -1; 
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
            // 🚨 FIX TRAY 1: Forzamos el arranque del motor AWT de fondo para evitar conflictos con JavaFX
            java.awt.Toolkit.getDefaultToolkit();

            java.awt.SystemTray bandeja = java.awt.SystemTray.getSystemTray();
            
            java.awt.image.BufferedImage imagenIcono = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = imagenIcono.createGraphics();
            g.setColor(java.awt.Color.decode("#C2185B")); 
            g.fillRect(0, 0, 16, 16);
            g.setColor(java.awt.Color.WHITE);
            g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            g.drawString("M", 4, 13);
            g.dispose();

            java.awt.TrayIcon iconoTray = new java.awt.TrayIcon(imagenIcono, "Mi ToDo List");
            iconoTray.setImageAutoSize(true);
            iconoTray.setToolTip("Mi ToDo List - Clic izquierdo para abrir"); // Feedback visual

            // 🚨 FIX TRAY 2: Evento nativo del Mouse. ¡Cualquier clic izquierdo despierta la app al instante!
            iconoTray.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) { // Clic izquierdo
                        restaurarVentana();
                    }
                }
            });
            
            // Lo dejamos por si el usuario está acostumbrado a hacer doble clic
            iconoTray.addActionListener(e -> restaurarVentana()); 

            java.awt.PopupMenu menuClicDerecho = new java.awt.PopupMenu();
            
            java.awt.MenuItem itemAbrir = new java.awt.MenuItem("Abrir Aplicación");
            itemAbrir.addActionListener(e -> restaurarVentana());
            
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

        } catch (Throwable t) {
            System.out.println("Error al configurar el System Tray: " + t.getMessage());
        }
    }

    // ==========================================
    // ⌨️ MOTOR DE ATAJOS GLOBALES (HOTKEYS)
    // ==========================================
    private static void configurarAtajosGlobales() {
        try {
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false);

            GlobalScreen.registerNativeHook();

            GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
                @Override
                public void nativeKeyPressed(NativeKeyEvent e) {
                    if (e.getKeyCode() == NativeKeyEvent.VC_T && (e.getModifiers() & NativeKeyEvent.ALT_MASK) != 0) {
                       restaurarVentana();
                    } 
                }

                @Override
                public void nativeKeyReleased(NativeKeyEvent e) {}

                @Override
                public void nativeKeyTyped(NativeKeyEvent e) {}
            });
            
        } catch (Throwable t) { 
            System.out.println("No se pudieron cargar los atajos nativos: " + t.getMessage());
        }
    }

}