package com.mitodolist;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ConfiguracionController {

    @FXML private HBox barraSuperiorConfig;
    @FXML private Label lblNombreUsuario;
    
    // Paneles de Navegación
    @FXML private VBox vistaDashboard;
    @FXML private VBox vistaSistema;
    @FXML private VBox vistaPrivacidad;
    @FXML private VBox vistaApariencia;
    @FXML private VBox vistaCuenta;

    // Controles de la Sección: Sistema
    @FXML private CheckBox chkArranque;
    @FXML private ComboBox<String> comboIntervalo;
    @FXML private CheckBox chkSonido;
    @FXML private CheckBox chkOcultar;

    // Controles de la Sección: Privacidad
    @FXML private ComboBox<String> comboBloqueo;
    @FXML private CheckBox chkPrivacidad;

    // Controles de la Sección: Apariencia
    @FXML private ComboBox<String> comboTema;
    @FXML private ComboBox<String> comboColor;
    @FXML private ComboBox<String> comboFecha;

    // Controles de la Sección: Cuenta y Red
    @FXML private CheckBox chkSyncAuto;
    @FXML private Button btnSincronizar;
    @FXML private VBox panelInfoSync;
    @FXML private Label lblIpSync;
    @FXML private Label lblPinSync;

    private double xOffset = 0;
    private double yOffset = 0;
    
    private Configuracion configActual;
    private String colorTemporalHex = "";
    private boolean dialogoAbierto = false;      // 🚨 Escudo anti-doble ventana
    private boolean ignorarAccionCombo = false;  // 🚨 Escudo anti-bucles
    private ServidorSync servidorP2P;

    @FXML
    public void initialize() {
        lblNombreUsuario.setText(GestorBaseDatos.obtenerNombreUsuario());
        configActual = GestorConfiguracion.cargarConfiguracion();
        
        // Llenamos los ComboBox de las primeras secciones
        comboIntervalo.getItems().addAll("Desactivadas", "5 minutos", "15 minutos", "30 minutos", "60 minutos");
        comboBloqueo.getItems().addAll("Desactivadas", "5 minutos", "15 minutos", "30 minutos", "60 minutos");
        
        // Si ya agregaste el FXML de apariencia, llenamos sus opciones también
        if(comboTema != null) {
            comboTema.getItems().addAll("Modo Oscuro (Predeterminado)", "Modo Claro");
            comboColor.getItems().addAll("Magenta (Predeterminado)", "Azul", "Verde Esmeralda", "Naranja", "🎨 Personalizado...");
            comboFecha.getItems().addAll("dd/MM/yyyy (Latam/Europa)", "MM/dd/yyyy (EE.UU.)");

            // 🚨 Sensor en vivo: Tema Claro/Oscuro
            comboTema.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) aplicarTemaEnVivo(newVal.equals("Modo Claro"), null);
            });

            // 🚨 Sensor en vivo: Colores Predeterminados
            comboColor.setOnAction(e -> {
                if (ignorarAccionCombo) return;
                String seleccionado = comboColor.getValue();
                if (seleccionado == null) return;
                
                if (seleccionado.equals("Azul")) aplicarTemaEnVivo(null, "#2196F3");
                else if (seleccionado.equals("Verde Esmeralda")) aplicarTemaEnVivo(null, "#4CAF50");
                else if (seleccionado.equals("Naranja")) aplicarTemaEnVivo(null, "#FF9800");
                else if (seleccionado.equals("Magenta (Predeterminado)")) aplicarTemaEnVivo(null, "#C2185B");
                else if (seleccionado.equals("🎨 Personalizado...")) {
                    abrirSelectorColorPersonalizado();
                }
            });

            // 🚨 MAGIA 2: Hackeamos la celda "Personalizada" para obligarla a escuchar nuestros clics infinitos
            comboColor.setCellFactory(lv -> {
                javafx.scene.control.ListCell<String> celda = new javafx.scene.control.ListCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : item);
                    }
                };
                celda.setOnMousePressed(e -> {
                    if (celda.getItem() != null && celda.getItem().equals("🎨 Personalizado...")) {
                        javafx.application.Platform.runLater(() -> abrirSelectorColorPersonalizado());
                    }
                });
                return celda;
            });
        }

        barraSuperiorConfig.setOnMousePressed(event -> { xOffset = event.getSceneX(); yOffset = event.getSceneY(); });
        barraSuperiorConfig.setOnMouseDragged(event -> {
            Stage stage = (Stage) barraSuperiorConfig.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    @FXML
    public void cerrarVentana(ActionEvent evento) {
        accionDetenerSync();
        Stage stage = (Stage) ((Node) evento.getSource()).getScene().getWindow();
        stage.close();
    }

    // --- MOTOR DE NAVEGACIÓN CORREGIDO (StackPane) ---
    @FXML
    public void mostrarDashboard() {
        // Ahora sí, apagamos CUALQUIER panel que esté abierto antes de mostrar el principal
        if(vistaSistema != null) vistaSistema.setVisible(false);
        if(vistaPrivacidad != null) vistaPrivacidad.setVisible(false);
        if(vistaApariencia != null) vistaApariencia.setVisible(false);
        if(vistaCuenta != null) vistaCuenta.setVisible(false);
        
        if(vistaDashboard != null) vistaDashboard.setVisible(true);
    }

    // ==========================================
    //  SECCIÓN: SISTEMA
    // ==========================================
    @FXML
    public void abrirSeccionSistema() {
        chkArranque.setSelected(configActual.isArranqueAutomatico());
        int intervalo = configActual.getIntervaloNotificaciones();
        if (intervalo == 0) {
            comboIntervalo.setValue("Desactivadas");
        } else {
            comboIntervalo.setValue(intervalo + " minutos");
        }
        
        chkSonido.setSelected(configActual.isSonidoNotificaciones());
        chkOcultar.setSelected(configActual.isOcultarCompletadasAuto());
        
        vistaDashboard.setVisible(false);
        vistaSistema.setVisible(true);
    }
    
    @FXML
    public void guardarConfiguracionSistema() {
        configActual.setArranqueAutomatico(chkArranque.isSelected());
        
        String seleccion = comboIntervalo.getValue();
        if (seleccion.equals("Desactivadas")) {
            configActual.setIntervaloNotificaciones(0);
        } else {
            int num = Integer.parseInt(seleccion.split(" ")[0]);
            configActual.setIntervaloNotificaciones(num);
        }
        
        configActual.setSonidoNotificaciones(chkSonido.isSelected());
        configActual.setOcultarCompletadasAuto(chkOcultar.isSelected());
        
        GestorConfiguracion.guardarConfiguracion(configActual);
        GestorConfiguracion.configurarArranqueWindows(configActual.isArranqueAutomatico());
        System.out.println("✅ Configuración de sistema guardada.");
        
        mostrarDashboard();
    }

    // ==========================================
    // SECCIÓN: PRIVACIDAD Y SEGURIDAD
    // ==========================================
    @FXML
    public void abrirSeccionPrivacidad() {
        int bloqueo = configActual.getBloqueoInactividad();
        if (bloqueo == 0) {
            // CORRECCIÓN CLAVE: Ahora dice "Desactivadas" (Plural)
            comboBloqueo.setValue("Desactivadas"); 
        } else {
            comboBloqueo.setValue(bloqueo + " minutos");
        }
        
        chkPrivacidad.setSelected(configActual.isModoPrivacidad());
        
        vistaDashboard.setVisible(false);
        vistaPrivacidad.setVisible(true);
    }

    @FXML
    public void guardarConfiguracionPrivacidad() {
        String seleccion = comboBloqueo.getValue();
        
        // AHORA COINCIDE PERFECTAMENTE CON EL VALOR DEL COMBOBOX
        if (seleccion.equals("Desactivadas")) { 
            configActual.setBloqueoInactividad(0);
        } else {
            int num = Integer.parseInt(seleccion.split(" ")[0]);
            configActual.setBloqueoInactividad(num);
        }
        
        configActual.setModoPrivacidad(chkPrivacidad.isSelected());
        
        GestorConfiguracion.guardarConfiguracion(configActual);
        System.out.println("🛡️ Configuración de privacidad guardada.");
        
        mostrarDashboard();
    }

    // ==========================================
    //  SECCIÓN: APARIENCIA
    // ==========================================
    @FXML
    public void abrirSeccionApariencia() {
        if(comboTema == null) return;
        
        comboTema.setValue(configActual.isTemaClaro() ? "Modo Claro" : "Modo Oscuro (Predeterminado)");

        ignorarAccionCombo = true; // Pausamos los sensores temporalmente
        String color = configActual.getColorAcento();
        
        if(color.equalsIgnoreCase("#2196F3")) comboColor.setValue("Azul");
        else if(color.equalsIgnoreCase("#4CAF50")) comboColor.setValue("Verde Esmeralda");
        else if(color.equalsIgnoreCase("#FF9800")) comboColor.setValue("Naranja");
        else if(color.equalsIgnoreCase("#C2185B")) comboColor.setValue("Magenta (Predeterminado)");
        else {
            colorTemporalHex = color;
            comboColor.setValue("🎨 Personalizado...");
        }
        ignorarAccionCombo = false; // Reactivamos sensores

        comboFecha.setValue(configActual.getFormatoFecha().startsWith("dd") ? "dd/MM/yyyy (Latam/Europa)" : "MM/dd/yyyy (EE.UU.)");

        vistaDashboard.setVisible(false);
        vistaApariencia.setVisible(true);
    }

    @FXML
    public void guardarConfiguracionApariencia() {
        configActual.setTemaClaro(comboTema.getValue().equals("Modo Claro"));
        
        String colorSelec = comboColor.getValue();
        if (colorSelec == null) colorSelec = "Magenta (Predeterminado)"; // Escudo anti-nulos
        
        if(colorSelec.equals("Azul")) configActual.setColorAcento("#2196F3");
        else if(colorSelec.equals("Verde Esmeralda")) configActual.setColorAcento("#4CAF50");
        else if(colorSelec.equals("Naranja")) configActual.setColorAcento("#FF9800");
        else if(colorSelec.equals("Magenta (Predeterminado)")) configActual.setColorAcento("#C2185B");
        else if(colorSelec.equals("🎨 Personalizado...")) {
            if (!colorTemporalHex.isEmpty()) configActual.setColorAcento(colorTemporalHex);
        }
        
        configActual.setFormatoFecha(comboFecha.getValue().startsWith("dd") ? "dd/MM/yyyy" : "MM/dd/yyyy");
        GestorConfiguracion.guardarConfiguracion(configActual);
        
        System.out.println("🎨 Configuración de apariencia guardada.");
        mostrarDashboard();
    }

    // =======================================================
    // 🎨 MOTOR DE COLOR PERSONALIZADO
    // =======================================================
    private void abrirSelectorColorPersonalizado() {
        if (dialogoAbierto) return; // Evita que se abran 2 ventanas al mismo tiempo
        dialogoAbierto = true;

        javafx.scene.control.Dialog<String> dialogo = new javafx.scene.control.Dialog<>();
        dialogo.setTitle("Color Personalizado");
        dialogo.setHeaderText("Elige tu color de acento ideal:");
        
        try {
            if (comboColor != null && comboColor.getScene() != null && comboColor.getScene().getWindow() != null) {
                dialogo.initOwner(comboColor.getScene().getWindow());
            }
            dialogo.initStyle(javafx.stage.StageStyle.UNDECORATED);
            dialogo.getDialogPane().getStylesheets().add(getClass().getResource("/com/mitodolist/estilos.css").toExternalForm());
            dialogo.setGraphic(null); 
            dialogo.getDialogPane().getStyleClass().add("mi-dialogo");
            dialogo.getDialogPane().setStyle("-color-acento: " + configActual.getColorAcento() + ";");
            if (configActual.isTemaClaro()) dialogo.getDialogPane().getStyleClass().add("tema-claro");

            final double[] xOff = {0};
            final double[] yOff = {0};
            dialogo.getDialogPane().setOnMousePressed(event -> { xOff[0] = event.getSceneX(); yOff[0] = event.getSceneY(); });
            dialogo.getDialogPane().setOnMouseDragged(event -> {
                Stage stage = (Stage) dialogo.getDialogPane().getScene().getWindow();
                stage.setX(event.getScreenX() - xOff[0]);
                stage.setY(event.getScreenY() - yOff[0]);
            });
        } catch (Exception e) {}

        javafx.scene.control.ButtonType btnAceptar = new javafx.scene.control.ButtonType("Aceptar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnAceptar, javafx.scene.control.ButtonType.CANCEL);

        String colorInicial = colorTemporalHex.isEmpty() ? configActual.getColorAcento() : colorTemporalHex;
        javafx.scene.control.ColorPicker picker = new javafx.scene.control.ColorPicker(javafx.scene.paint.Color.web(colorInicial));
        picker.setStyle("-fx-background-color: -bg-caja; -fx-color-label-visible: false;");
        picker.setPrefHeight(40);
        picker.setPrefWidth(200);

        // 🚨 MAGIA 1: VISTA PREVIA EN VIVO MIENTRAS MUEVES EL MOUSE
        picker.valueProperty().addListener((obs, oldC, newC) -> {
            if (newC != null) {
                String hexEnVivo = String.format("#%02X%02X%02X", 
                    (int)(newC.getRed() * 255), (int)(newC.getGreen() * 255), (int)(newC.getBlue() * 255));
                aplicarTemaEnVivo(null, hexEnVivo);
            }
        });

        VBox contenedor = new VBox(picker);
        contenedor.setAlignment(javafx.geometry.Pos.CENTER);
        contenedor.setStyle("-fx-padding: 10;");
        dialogo.getDialogPane().setContent(contenedor);

        dialogo.setResultConverter(btn -> {
            if (btn == btnAceptar) {
                return String.format("#%02X%02X%02X", 
                    (int)(picker.getValue().getRed() * 255),
                    (int)(picker.getValue().getGreen() * 255),
                    (int)(picker.getValue().getBlue() * 255));
            }
            return null;
        });

        java.util.Optional<String> resultado = dialogo.showAndWait();
        resultado.ifPresent(hex -> {
            colorTemporalHex = hex;
            aplicarTemaEnVivo(null, hex);
            
            javafx.application.Platform.runLater(() -> {
                ignorarAccionCombo = true;
                comboColor.setValue("🎨 Personalizado...");
                ignorarAccionCombo = false;
            });
        });

        // 🚨 UX PRO: Si cancelas, la interfaz regresa sola a su color anterior original
        if (resultado.isEmpty()) {
            aplicarTemaEnVivo(null, configActual.getColorAcento());
            javafx.application.Platform.runLater(() -> {
                ignorarAccionCombo = true;
                abrirSeccionApariencia(); 
                ignorarAccionCombo = false;
            });
        }
        
        dialogoAbierto = false; // Bajamos el escudo
    }

    // ==========================================
    // SECCIÓN: CUENTA
    // ==========================================
    @FXML
    public void abrirSeccionCuenta() {
        if(vistaCuenta != null) {
            // Cargamos la preferencia de Sincronización Automática de SQLite
            chkSyncAuto.setSelected(configActual.isSincronizacionAutomatica());
            
            vistaDashboard.setVisible(false);
            vistaCuenta.setVisible(true);
        }
    }

    // ==========================================
    // 🔐 SISTEMA DE CRIPTOGRAFÍA DE CUENTA
    // ==========================================
    @FXML
    public void accionCambiarPin() {
        javafx.scene.control.Dialog<String[]> dialogo = new javafx.scene.control.Dialog<>();
        dialogo.setTitle("Seguridad de la Cuenta");
        dialogo.setHeaderText("Modificar PIN de Acceso");

        // --- DISEÑO Y FÍSICA DEL DIÁLOGO ---
        try {
            dialogo.initStyle(javafx.stage.StageStyle.UNDECORATED);
            dialogo.getDialogPane().getStylesheets().add(getClass().getResource("/com/mitodolist/estilos.css").toExternalForm());
            dialogo.setGraphic(null); 
            dialogo.getDialogPane().getStyleClass().add("mi-dialogo");
            
            dialogo.getDialogPane().setStyle("-color-acento: " + configActual.getColorAcento() + ";");
            if (configActual.isTemaClaro()) {
                dialogo.getDialogPane().getStyleClass().add("tema-claro");
            }

            // Motor de físicas para arrastrar la ventana
            final double[] xOffset = {0};
            final double[] yOffset = {0};
            dialogo.getDialogPane().setOnMousePressed(event -> { xOffset[0] = event.getSceneX(); yOffset[0] = event.getSceneY(); });
            dialogo.getDialogPane().setOnMouseDragged(event -> {
                Stage stage = (Stage) dialogo.getDialogPane().getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            });
        } catch (Exception e) {}

        // --- CAMPOS DE SEGURIDAD (PasswordFields) ---
        javafx.scene.control.ButtonType btnGuardar = new javafx.scene.control.ButtonType("Guardar Cambios", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnGuardar, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.control.PasswordField txtPinActual = new javafx.scene.control.PasswordField();
        txtPinActual.setPromptText("PIN Actual");
        txtPinActual.getStyleClass().add("caja-texto");
        txtPinActual.setPrefHeight(40);

        javafx.scene.control.PasswordField txtPinNuevo = new javafx.scene.control.PasswordField();
        txtPinNuevo.setPromptText("Nuevo PIN");
        txtPinNuevo.getStyleClass().add("caja-texto");
        txtPinNuevo.setPrefHeight(40);

        javafx.scene.control.PasswordField txtPinConfirmar = new javafx.scene.control.PasswordField();
        txtPinConfirmar.setPromptText("Confirmar Nuevo PIN");
        txtPinConfirmar.getStyleClass().add("caja-texto");
        txtPinConfirmar.setPrefHeight(40);

        VBox contenedor = new VBox(15, txtPinActual, txtPinNuevo, txtPinConfirmar);
        contenedor.setAlignment(javafx.geometry.Pos.CENTER);
        dialogo.getDialogPane().setContent(contenedor);

        // --- LÓGICA DE CAPTURA ---
        dialogo.setResultConverter(dialogButton -> {
            if (dialogButton == btnGuardar) {
                return new String[]{txtPinActual.getText(), txtPinNuevo.getText(), txtPinConfirmar.getText()};
            }
            return null;
        });

        // --- PROCESAMIENTO CRIPTOGRÁFICO ---
        java.util.Optional<String[]> resultado = dialogo.showAndWait();
        resultado.ifPresent(pines -> {
            String pinActual = pines[0];
            String pinNuevo = pines[1];
            String pinConf = pines[2];

            if (pinActual.isEmpty() || pinNuevo.isEmpty() || pinConf.isEmpty()) {
                mostrarAlertaSeguridad("Error", "Todos los campos son obligatorios.");
                return;
            }

            if (!pinNuevo.equals(pinConf)) {
                mostrarAlertaSeguridad("Error de Seguridad", "Los nuevos PINs no coinciden. Inténtalo de nuevo.");
                return;
            }

            // 1. Validamos identidad contra el Hash de la BD
            if (GestorBaseDatos.autenticarUsuario(GestorBaseDatos.obtenerNombreUsuario(), pinActual)) {
                
                // 2. Ejecutamos la inyección del nuevo Hash
                if(GestorBaseDatos.actualizarPin(pinNuevo)) {
                    mostrarAlertaSeguridad("Éxito", "🔒 Tu PIN ha sido encriptado y actualizado correctamente.");
                } else {
                    mostrarAlertaSeguridad("Error Crítico", "No se pudo actualizar la base de datos.");
                }
            } else {
                mostrarAlertaSeguridad("Acceso Denegado", "El PIN actual introducido es incorrecto.");
            }
        });
    }

    // Herramienta visual para lanzar notificaciones heredando el Modo Claro/Oscuro
    private void mostrarAlertaSeguridad(String titulo, String mensaje) {
        javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        
        try {
            alerta.initStyle(javafx.stage.StageStyle.UNDECORATED);
            alerta.getDialogPane().getStylesheets().add(getClass().getResource("/com/mitodolist/estilos.css").toExternalForm());
            alerta.getDialogPane().getStyleClass().add("mi-dialogo");
            alerta.getDialogPane().setStyle("-color-acento: " + configActual.getColorAcento() + ";");
            if (configActual.isTemaClaro()) alerta.getDialogPane().getStyleClass().add("tema-claro");
        } catch (Exception e) {}
        
        alerta.showAndWait();
    }

    @FXML
    public void accionEliminarCuenta() {
        // 1. Primera barrera: Advertencia de Peligro
        javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alerta.setTitle("Zona de Peligro");
        alerta.setHeaderText("¡Atención! Estás a punto de destruir tu cuenta.");
        alerta.setContentText("Esta acción eliminará permanentemente tu usuario, tus listas y todas tus tareas encriptadas.\n\nNo hay forma de deshacer esto. ¿Deseas continuar?");
        alerta.getButtonTypes().setAll(javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
        
        // Usamos nuestro inyector de CSS para que la alerta herede el diseño
        try {
            alerta.initStyle(javafx.stage.StageStyle.UNDECORATED);
            alerta.getDialogPane().getStylesheets().add(getClass().getResource("/com/mitodolist/estilos.css").toExternalForm());
            alerta.getDialogPane().getStyleClass().add("mi-dialogo");
            alerta.getDialogPane().setStyle("-color-acento: " + configActual.getColorAcento() + ";");
            if (configActual.isTemaClaro()) alerta.getDialogPane().getStyleClass().add("tema-claro");
        } catch (Exception e) {}

        java.util.Optional<javafx.scene.control.ButtonType> respuesta = alerta.showAndWait();
        
        if (respuesta.isPresent() && respuesta.get() == javafx.scene.control.ButtonType.YES) {
            
            // 2. Segunda barrera: Autenticación por PIN
            javafx.scene.control.Dialog<String> dialogoPin = new javafx.scene.control.Dialog<>();
            dialogoPin.setTitle("Confirmación de Seguridad");
            dialogoPin.setHeaderText("Ingresa tu PIN actual para autorizar la destrucción:");
            
            try {
                dialogoPin.initStyle(javafx.stage.StageStyle.UNDECORATED);
                dialogoPin.getDialogPane().getStylesheets().add(getClass().getResource("/com/mitodolist/estilos.css").toExternalForm());
                dialogoPin.getDialogPane().getStyleClass().add("mi-dialogo");
                dialogoPin.getDialogPane().setStyle("-color-acento: " + configActual.getColorAcento() + ";");
                if (configActual.isTemaClaro()) dialogoPin.getDialogPane().getStyleClass().add("tema-claro");
            } catch (Exception e) {}
            
            javafx.scene.control.PasswordField txtPin = new javafx.scene.control.PasswordField();
            txtPin.setPromptText("PIN de acceso");
            txtPin.getStyleClass().add("caja-texto");
            txtPin.setPrefHeight(40);
            
            dialogoPin.getDialogPane().setContent(txtPin);
            dialogoPin.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
            
            dialogoPin.setResultConverter(boton -> boton == javafx.scene.control.ButtonType.OK ? txtPin.getText() : null);
            
            java.util.Optional<String> pinIngresado = dialogoPin.showAndWait();
            
            pinIngresado.ifPresent(pin -> {
                // 3. Ejecución del Protocolo
                if (GestorBaseDatos.eliminarUsuarioCompleto(pin)) {
                    
                    // Cerramos la ventana de configuración modal primero
                    Stage stageModal = (Stage) vistaCuenta.getScene().getWindow();
                    stageModal.close();
                    
                    // Expulsamos al usuario a la pantalla de Login
                    App.cambiarEscena("VentanaLogin.fxml", "Acceso - Mi TodoList");
                    System.out.println("💀 Cuenta destruida satisfactoriamente.");
                    
                } else {
                    mostrarAlertaSeguridad("Acceso Denegado", "El PIN es incorrecto. La operación ha sido abortada para proteger la cuenta.");
                }
            });
        }
    }

    // =======================================================
    // 👁️ MOTOR DE VISTA PREVIA EN VIVO (LIVE PREVIEW)
    // =======================================================
    private void aplicarTemaEnVivo(Boolean esClaro, String colorHex) {
        if (vistaDashboard == null || vistaDashboard.getScene() == null) return;
        
        javafx.scene.Parent rootConfig = vistaDashboard.getScene().getRoot();
        javafx.scene.Parent rootPrincipal = null;
        
        // Buscamos la ventana principal (Tareas) que está anclada en el fondo
        if (vistaDashboard.getScene().getWindow() instanceof Stage) {
            Stage stageConfig = (Stage) vistaDashboard.getScene().getWindow();
            if (stageConfig.getOwner() != null && stageConfig.getOwner().getScene() != null) {
                rootPrincipal = stageConfig.getOwner().getScene().getRoot();
            }
        }

        // 1. Inyectar Color en Vivo al instante
        if (colorHex != null) {
            String estiloAcento = "-color-acento: " + colorHex + ";";
            rootConfig.setStyle(estiloAcento);
            if (rootPrincipal != null) rootPrincipal.setStyle(estiloAcento);
        }

        // 2. Inyectar Tema (Claro/Oscuro) en Vivo al instante
        if (esClaro != null) {
            if (esClaro) {
                if (!rootConfig.getStyleClass().contains("tema-claro")) rootConfig.getStyleClass().add("tema-claro");
                if (rootPrincipal != null && !rootPrincipal.getStyleClass().contains("tema-claro")) rootPrincipal.getStyleClass().add("tema-claro");
            } else {
                rootConfig.getStyleClass().remove("tema-claro");
                if (rootPrincipal != null) rootPrincipal.getStyleClass().remove("tema-claro");
            }
        }
    }

    // ==========================================
    // 🌐 SISTEMA DE RED Y SINCRONIZACIÓN
    // ==========================================
    
    @FXML
    public void guardarConfigSyncAuto() {
        // Se guarda en tiempo real cuando el usuario hace clic en el CheckBox
        configActual.setSincronizacionAutomatica(chkSyncAuto.isSelected());
        GestorConfiguracion.guardarConfiguracion(configActual);
        System.out.println("🔄 Sincronización Automática configurada: " + chkSyncAuto.isSelected());
    }

    @FXML
    public void accionSincronizar() {
        if (servidorP2P == null) {
            servidorP2P = new ServidorSync(false); // Falso porque es Manual
        }
        
        servidorP2P.iniciarServidor();
        
        // Actualizamos la Interfaz Gráfica con la IP y el PIN generado
        lblIpSync.setText("IP: " + ServidorSync.obtenerIPLocal());
        lblPinSync.setText("PIN: " + servidorP2P.getPinActual());
        
        // Magia visual: Ocultamos el botón principal y revelamos el cuadro de IP
        btnSincronizar.setVisible(false);
        btnSincronizar.setManaged(false);
        
        panelInfoSync.setVisible(true);
        panelInfoSync.setManaged(true);
    }

    @FXML
    public void accionDetenerSync() {
        if (servidorP2P != null) {
            servidorP2P.detenerServidor();
            servidorP2P = null; // Reiniciamos el objeto para que genere un PIN nuevo la próxima vez
        }
        
        // Magia visual: Ocultamos el cuadro de IP y restauramos el botón principal
        panelInfoSync.setVisible(false);
        panelInfoSync.setManaged(false);
        
        btnSincronizar.setVisible(true);
        btnSincronizar.setManaged(true);
    }
}