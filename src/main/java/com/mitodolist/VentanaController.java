package com.mitodolist;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import java.util.ArrayList;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

public class VentanaController {

    @FXML private ComboBox<String> comboFiltros;
    @FXML private ListView<Tarea> listaTareas;
    @FXML private TextField txtNuevaTarea;
    @FXML private DatePicker calendarioPrincipal;
    @FXML private Label lblPendientes;
    @FXML private Label lblCompletadas;
    @FXML private Label lblAtrasadas;
    @FXML private javafx.scene.layout.VBox contenedorCategorias;
    @FXML private TextField txtBuscador;
    @FXML private DatePicker calendarioFiltro;
    @FXML private Button btnLimpiarFiltros;
    @FXML private javafx.scene.layout.VBox panelTareas;
    @FXML private javafx.scene.layout.VBox panelHabitos;
    @FXML private Button btnVistaTareas;
    @FXML private Button btnVistaHabitos;
    @FXML private Label lblMesHabitos;
    @FXML private javafx.scene.layout.VBox contenedorMatrizHabitos;

    private ToDoList logica = new ToDoList();
    private int filtroActual = 1;
    private String categoriaTareas = "Todas";
    private String categoriaHabitos = "Todas";
    private boolean modoPrivacidadActivo = false;
    private String formatoFechaActual = "dd/MM/yyyy";
    private java.time.YearMonth mesNavegacion = java.time.YearMonth.now();

    @FXML
    public void initialize() {
        comboFiltros.getItems().addAll("Todas las tareas", "⏳ Pendientes", "✅ Completadas", "⚠ Atrasadas");
        comboFiltros.getSelectionModel().selectFirst();
        
        comboFiltros.setOnAction(evento -> {
            filtroActual = comboFiltros.getSelectionModel().getSelectedIndex() + 1;
            actualizarInterfaz();
        });

        // --- ESCÁNER EN TIEMPO REAL ---
        if (txtBuscador != null) {
            txtBuscador.textProperty().addListener((observable, oldValue, newValue) -> actualizarInterfaz());
        }
        if (calendarioFiltro != null) {
            calendarioFiltro.valueProperty().addListener((observable, oldValue, newValue) -> actualizarInterfaz());
            // 🚨 FIX ERROR 4: Reconectamos el DatePicker de arriba con su diseño CSS
            calendarioFiltro.getStyleClass().add("date-picker-filtro");
        }
        
        // 🚨 FIX ERROR 4: Reconectamos el DatePicker de abajo con su diseño CSS
        if (calendarioPrincipal != null) {
            calendarioPrincipal.getStyleClass().add("date-picker-creacion");
        }

        // --- MENÚ CONTEXTUAL V4.0.0e ---
        ContextMenu menuClickDerecho = new ContextMenu();
        
        MenuItem menuCompletar = new MenuItem("✅ Marcar/Desmarcar como Completada");
        menuCompletar.setOnAction(e -> accionCompletar());
        
        MenuItem menuAgregarSub = new MenuItem("➕ Agregar Sub-tarea");
        menuAgregarSub.setOnAction(e -> accionAgregarSubTarea());

        MenuItem menuEditarDesc = new MenuItem("🖊 Editar Descripción");
        menuEditarDesc.setOnAction(e -> accionEditarDescripcion());
        MenuItem menuEditarFecha = new MenuItem("⏱ Editar Fecha, Hora y Repetición");
        menuEditarFecha.setOnAction(e -> accionEditarDetallesTiempo());

        MenuItem menuEliminar = new MenuItem("🗑 Eliminar");
        menuEliminar.setOnAction(e -> accionEliminar());

        MenuItem menuEditarCat = new MenuItem("🗂 Editar Categoría (Solo principal)");
        menuEditarCat.setOnAction(e -> accionEditarCategoria());
       

        // ... (y no olvides agregarlo al getItems().addAll al final)
        menuClickDerecho.getItems().addAll(menuCompletar, menuAgregarSub, menuEditarDesc, menuEditarFecha, menuEditarCat, menuEliminar);
        listaTareas.setContextMenu(menuClickDerecho);

        txtNuevaTarea.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER) {
                agregarNuevaTarea();
            }
        });

        // --- FÁBRICA DE CELDAS: EFECTO DE ÁRBOL VISUAL ---
        listaTareas.setCellFactory(parametro -> new javafx.scene.control.ListCell<Tarea>() {
           @Override
            protected void updateItem(Tarea tareaActual, boolean vacio) {
                super.updateItem(tareaActual, vacio);

                if (vacio || tareaActual == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("celda-padre", "celda-hija");
                    setOnMouseEntered(null);
                    setOnMouseExited(null);
                } else {
                    getStyleClass().removeAll("celda-padre", "celda-hija"); 
                    
                    // 1. Construcción de texto y Hora
                    String textoFecha = "";
                    if (tareaActual.getFechaLimite() != null) {
                        java.time.format.DateTimeFormatter formato = java.time.format.DateTimeFormatter.ofPattern(formatoFechaActual);
                        textoFecha = " 📅 [Vence: " + tareaActual.getFechaLimite().format(formato);
                        
                        // 🚨 NUEVO: Formateamos la vista de la hora a 12h (hh:mm a)
                        if (tareaActual.getHoraLimite() != null) {
                            java.time.format.DateTimeFormatter formatoHora = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
                            textoFecha += " a las " + tareaActual.getHoraLimite().format(formatoHora);
                        }
                        textoFecha += "]";
                    }

                    // 🚨 ELIMINAMOS LA VARIABLE "ESTADO" (Adiós a los [X] y [ ])
                    boolean esSubtarea = tareaActual.getIdTareaPadre() != null;

                    String descReal = tareaActual.getDescripcion();
                    String descOculta = descReal.replaceAll("[^\\s]", "*"); 
                    
                    String descMostrar = modoPrivacidadActivo ? descOculta : descReal;
                    String textoBase = ""; 

                    if (esSubtarea) {
                        textoBase = "      ↳ "; // Sangría limpia
                        setText(textoBase + descMostrar + textoFecha);
                        setStyle("-fx-text-fill: -texto-secundario; -fx-font-size: 14px;"); 
                    } else {
                        int numeroVisual = 1;
                        for (int i = 0; i < getIndex(); i++) {
                            Tarea tAnterior = getListView().getItems().get(i);
                            if (tAnterior.getIdTareaPadre() == null) numeroVisual++;
                        }
                        
                        textoBase = numeroVisual + ". "; // Solo el número limpio
                        setText(textoBase + descMostrar + textoFecha);
                        setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 16px; -fx-font-weight: bold;"); 
                    }

                    if (modoPrivacidadActivo) {
                        final String baseFinal = textoBase;
                        final String fechaFinal = textoFecha;
                        setOnMouseEntered(e -> setText(baseFinal + descReal + fechaFinal));
                        setOnMouseExited(e -> setText(baseFinal + descOculta + fechaFinal));
                    } else {
                        setOnMouseEntered(null);
                        setOnMouseExited(null);
                    }

                    // 2. Colores del Indicador incluyendo la HORA
                    javafx.scene.shape.Circle indicador = new javafx.scene.shape.Circle(esSubtarea ? 4 : 6); 
                    java.time.LocalDate hoy = java.time.LocalDate.now();
                    
                    if (tareaActual.isCompletada()) {
                        indicador.setFill(javafx.scene.paint.Color.web("#4CAF50")); 
                    } else if (tareaActual.getFechaLimite() != null) { 
                        if (tareaActual.getFechaLimite().isBefore(hoy)) {
                            indicador.setFill(javafx.scene.paint.Color.web("#F44336")); 
                        } else if (tareaActual.getFechaLimite().isEqual(hoy)) {
                            // 🚨 Evaluamos si la hora exacta ya pasó
                            if (tareaActual.getHoraLimite() != null && java.time.LocalTime.now().isAfter(tareaActual.getHoraLimite())) {
                                indicador.setFill(javafx.scene.paint.Color.web("#F44336")); 
                            } else {
                                indicador.setFill(javafx.scene.paint.Color.web("#FFEB3B")); 
                            }
                        } else {
                            indicador.setFill(javafx.scene.paint.Color.web("#2196F3"));
                        }
                    } else {
                        indicador.setFill(javafx.scene.paint.Color.web("#9E9E9E")); 
                    }

                    // 3. Botones y Caja visual
                    javafx.scene.layout.HBox cajaCelda = new javafx.scene.layout.HBox(8);
                    cajaCelda.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    cajaCelda.getChildren().add(indicador); 

                    if (!esSubtarea && tareaActual.getSubTareas() != null && !tareaActual.getSubTareas().isEmpty()) {
                        javafx.scene.control.Button btnToggle = new javafx.scene.control.Button(tareaActual.isExpandida() ? "[-] " : "[+] ");
                        btnToggle.getStyleClass().add("boton-desplegable-celda");
                        btnToggle.setOnAction(e -> {
                            tareaActual.setExpandida(!tareaActual.isExpandida());                            
                            GestorBaseDatos.actualizarTarea(tareaActual);
                            actualizarInterfaz();
                        });
                        cajaCelda.getChildren().add(btnToggle);
                    }

                    boolean tieneRepeticionValida = tareaActual.getTipoRepeticion() != null && !tareaActual.getTipoRepeticion().equals("NINGUNA");
                    
                    if (tareaActual.getFechaLimite() != null && tieneRepeticionValida) {
                        javafx.scene.control.Button btnHistorial = new javafx.scene.control.Button("🔄 Historial");
                        btnHistorial.setStyle("-fx-background-color: transparent; -fx-text-fill: -color-acento; -fx-cursor: hand; -fx-font-size: 13px; -fx-font-weight: bold;");
                        btnHistorial.setOnAction(e -> mostrarHistorial(tareaActual)); 
                        cajaCelda.getChildren().add(btnHistorial);
                    }
                    setGraphic(cajaCelda);
                } 
            } 
        });

        actualizarInterfaz();
        cargarMenuLateral();

        // 🚨 NUEVO V7: Reloj interno que repinta los colores cada 30 segundos
        javafx.animation.Timeline actualizadorVisual = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(30), e -> {
                listaTareas.refresh(); // Solo repinta la vista, no recarga la base de datos
            })
        );
        actualizadorVisual.setCycleCount(javafx.animation.Animation.INDEFINITE);
        actualizadorVisual.play();

        // 🚨 NUEVO V7: MOTOR DE REDIMENSIONAMIENTO AUTOMÁTICO E INTELIGENTE
        // Usamos Platform.runLater porque necesitamos esperar a que la ventana gráfica (Stage) 
        // esté completamente cargada en la memoria RAM antes de poder modificar sus dimensiones.
        javafx.application.Platform.runLater(() -> {
            javafx.stage.Stage escenario = (javafx.stage.Stage) listaTareas.getScene().getWindow();
            
            // 1. Obtenemos el tamaño real del monitor del usuario (ignorando la barra de tareas de Windows)
            javafx.geometry.Rectangle2D pantalla = javafx.stage.Screen.getPrimary().getVisualBounds();
            
            // 2. Definimos que la app ocupe el 85% de la pantalla para que se vea amplia pero no invasiva
            double anchoDeseado = pantalla.getWidth() * 0.85;
            double altoDeseado = pantalla.getHeight() * 0.85;
            
            escenario.setWidth(anchoDeseado);
            escenario.setHeight(altoDeseado);
            
            // 3. La centramos perfectamente en el medio del monitor
            escenario.setX((pantalla.getWidth() - anchoDeseado) / 2);
            escenario.setY((pantalla.getHeight() - altoDeseado) / 2);
            
            // 4. Escudo de seguridad: Evitamos que el usuario la encoja tanto que se rompan los botones
            escenario.setMinWidth(1100);
            escenario.setMinHeight(700);
        });

        // 🚨 FIX DEFINITIVO: MOTOR GLOBAL DE INYECCIÓN DE TEMAS
        // Vigila cuando JavaFX abre una ventana interna rebelde (como el ColorPicker)
        javafx.stage.Window.getWindows().addListener((javafx.collections.ListChangeListener.Change<? extends javafx.stage.Window> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (javafx.stage.Window ventana : c.getAddedSubList()) {
                        javafx.application.Platform.runLater(() -> {
                            if (ventana.getScene() != null && ventana.getScene().getRoot() != null) {
                                Configuracion config = GestorConfiguracion.cargarConfiguracion();
                                
                                // 1. Inyectamos el CSS de forma segura (sin duplicar)
                                String rutaCss = getClass().getResource("/com/mitodolist/estilos.css").toExternalForm();
                                if (!ventana.getScene().getStylesheets().contains(rutaCss)) {
                                    ventana.getScene().getStylesheets().add(rutaCss);
                                }
                                
                                // 2. Inyectamos el color de acento del usuario
                                ventana.getScene().getRoot().setStyle("-color-acento: " + config.getColorAcento() + ";");
                                
                                // 3. Inyectamos el Modo Claro si aplica (sin duplicar)
                                if (config.isTemaClaro()) {
                                    if (!ventana.getScene().getRoot().getStyleClass().contains("tema-claro")) {
                                        ventana.getScene().getRoot().getStyleClass().add("tema-claro");
                                    }
                                } else {
                                    ventana.getScene().getRoot().getStyleClass().remove("tema-claro");
                                }
                            }
                        });
                    }
                }
            }
        });
    } 

   private void actualizarInterfaz() {
        listaTareas.getItems().clear();
        
        ArrayList<Tarea> listaPrincipales = logica.obtenerTareasFiltradas(filtroActual, categoriaTareas);
        
        String textoBusqueda = (txtBuscador != null && txtBuscador.getText() != null) ? txtBuscador.getText().toLowerCase().trim() : "";
        java.time.LocalDate fechaFiltro = (calendarioFiltro != null) ? calendarioFiltro.getValue() : null;
        
        // 🚨 FIX: Leemos la configuración aquí (solo se ejecuta 1 vez al actualizar la lista)
        Configuracion config = GestorConfiguracion.cargarConfiguracion();
        modoPrivacidadActivo = config.isModoPrivacidad();
        
        // Memorizamos la apariencia
        formatoFechaActual = config.getFormatoFecha();
        aplicarFormatoCalendario(calendarioPrincipal, formatoFechaActual);
        aplicarFormatoCalendario(calendarioFiltro, formatoFechaActual);
        for (Tarea principal : listaPrincipales) {
            
            boolean coincideTexto = textoBusqueda.isEmpty() || principal.getDescripcion().toLowerCase().contains(textoBusqueda);
            boolean coincideFecha = fechaFiltro == null || (principal.getFechaLimite() != null && principal.getFechaLimite().isEqual(fechaFiltro));
            
            if (!coincideTexto || !coincideFecha) {
                continue; 
            }
            
            listaTareas.getItems().add(principal); 
            
            if (principal.isExpandida() && principal.getSubTareas() != null && !principal.getSubTareas().isEmpty()) {
                principal.getSubTareas().sort(ToDoList.ORDENADOR_TAREAS);
                
                // 🚨 FIX: Filtramos las hijas antes de inyectarlas en la lista visual
                for (Tarea hija : principal.getSubTareas()) {
                    if (filtroActual == 1 && config.isOcultarCompletadasAuto() && hija.isCompletada()) {
                        continue; // Saltamos esta subtarea, ya está completada
                    }
                    listaTareas.getItems().add(hija);
                }
            }
        }
        
        lblPendientes.setText("Pendientes: " + logica.contarPendientes());
        lblCompletadas.setText("Completadas: " + logica.contarCompletadas());
        lblAtrasadas.setText("Atrasadas: " + logica.contarAtrasadas());
    }

    private void cargarMenuLateral() {
        contenedorCategorias.getChildren().clear();

        javafx.scene.control.Button btnVerTodas = new javafx.scene.control.Button("🔎 Ver Todas");
        btnVerTodas.getStyleClass().add("boton-transparente");
        btnVerTodas.setStyle("-fx-font-size: 16px; -fx-padding: 8 0 8 10; -fx-alignment: center-left;");
        btnVerTodas.setMaxWidth(Double.MAX_VALUE); 
        btnVerTodas.setOnAction(e -> { 
            if (panelHabitos.isVisible()) {
                categoriaHabitos = "Todas";
                renderizarMatrizHabitos();
            } else {
                categoriaTareas = "Todas";
                actualizarInterfaz();
            }
            resaltarCategoriaActiva(); // 🚨 Disparamos el resaltado visual
        });
        contenedorCategorias.getChildren().add(btnVerTodas);

        String tipoVista = panelHabitos.isVisible() ? "HABITOS" : "TAREAS";
        ArrayList<Categoria> categoriasBD = GestorBaseDatos.obtenerCategorias(tipoVista);

        for (Categoria cat : categoriasBD) {
            javafx.scene.control.Button btn = new javafx.scene.control.Button(cat.getNombre());
            btn.getStyleClass().add("boton-transparente");
            btn.setStyle("-fx-font-size: 16px; -fx-padding: 8 0 8 10; -fx-alignment: center-left;");
            btn.setMaxWidth(Double.MAX_VALUE); 
            btn.setOnAction(e -> { 
                if (panelHabitos.isVisible()) {
                    categoriaHabitos = cat.getNombre();
                    renderizarMatrizHabitos();
                } else {
                    categoriaTareas = cat.getNombre();
                    actualizarInterfaz();
                }
                resaltarCategoriaActiva(); // 🚨 Disparamos el resaltado visual
            });

            if (cat.getId() != GestorBaseDatos.obtenerIdCategoria("📌 Sin categoría")) { 
                javafx.scene.control.ContextMenu menuLista = new javafx.scene.control.ContextMenu();
                
                javafx.scene.control.MenuItem menuRenombrar = new javafx.scene.control.MenuItem("🖊 Renombrar Lista");
                menuRenombrar.setOnAction(eventoClick -> accionRenombrarCategoria(cat));
                
                javafx.scene.control.MenuItem menuEliminar = new javafx.scene.control.MenuItem("🗑 Eliminar Lista");
                menuEliminar.setOnAction(eventoClick -> accionEliminarCategoria(cat));
                
                menuLista.getItems().addAll(menuRenombrar, menuEliminar);
                btn.setContextMenu(menuLista);
            }
            contenedorCategorias.getChildren().add(btn);
        }
        
        // 🚨 Aplicamos el resaltado inicial cuando se carga la aplicación por primera vez
        resaltarCategoriaActiva(); 
    }

    // =======================================================
    // 🎨 MOTOR DE RESALTADO DE LISTA ACTIVA
    // =======================================================
    private void resaltarCategoriaActiva() {
        // Determinamos qué memoria leer dependiendo de la vista actual
        String categoriaActiva = panelHabitos.isVisible() ? categoriaHabitos : categoriaTareas;

        for (javafx.scene.Node nodo : contenedorCategorias.getChildren()) {
            if (nodo instanceof Button) {
                Button btn = (Button) nodo;
                String textoBoton = btn.getText();
                
                // 1. Reseteamos el estilo a su estado base neutro (para que el CSS siga manejando el hover)
                btn.setStyle("-fx-font-size: 16px; -fx-padding: 8 0 8 10; -fx-alignment: center-left;");

                // 2. Comprobamos si este botón es la categoría en la que estamos parados
                boolean esTodas = categoriaActiva.equals("Todas") && textoBoton.equals("🔎 Ver Todas");
                boolean esCategoria = textoBoton.equals(categoriaActiva);

                if (esTodas || esCategoria) {
                    // 3. ¡Lo resaltamos! Fondo oscuro sutil, borde redondeado y color de acento en negrita
                    btn.setStyle("-fx-font-size: 16px; -fx-padding: 8 0 8 10; -fx-alignment: center-left; -fx-background-color: -bg-barra; -fx-text-fill: -color-acento; -fx-font-weight: bold; -fx-background-radius: 8;");
                }
            }
        }
    }

  @FXML
    public void agregarNuevaTarea() {
        String texto = txtNuevaTarea.getText();
        java.time.LocalDate fecha = calendarioPrincipal.getValue();

        if (texto != null && !texto.trim().isEmpty()) {
            
            Dialog<Object[]> dialogo = new Dialog<>();
            dialogo.setTitle("Detalles de la Tarea");
            dialogo.setHeaderText("Configurando: '" + texto + "'");
            aplicarTemaOscuro(dialogo); 
            
            ButtonType btnGuardar = new ButtonType("Guardar Tarea", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            dialogo.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);
            
            // 1. Selector de Categoría
            ComboBox<String> comboCat = new ComboBox<>();
            comboCat.getStyleClass().add("combo-capsula");
            comboCat.setPrefWidth(240); 
            // 🚨 CAMBIO AQUÍ: Añadimos ("TAREAS")
            for (Categoria cat : GestorBaseDatos.obtenerCategorias("TAREAS")) comboCat.getItems().add(cat.getNombre());
            comboCat.setValue(categoriaTareas.equals("Todas") ? "📌 Sin categoría" : categoriaTareas);
            
            // 2. HORA EN FORMATO 12H CON BOTÓN AM/PM
            TextField txtHora = new TextField();
            txtHora.setPromptText("Ej: 10:30");
            txtHora.getStyleClass().add("caja-texto");
            txtHora.setPrefHeight(35);

            Button btnAmPm = new Button("AM");
            btnAmPm.getStyleClass().add("boton-secundario");
            btnAmPm.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            btnAmPm.setPrefWidth(50); 
            btnAmPm.setPrefHeight(35); 

            btnAmPm.setOnAction(e -> btnAmPm.setText(btnAmPm.getText().equals("AM") ? "PM" : "AM"));

            javafx.scene.layout.HBox cajaHora = new javafx.scene.layout.HBox(10, txtHora, btnAmPm);
            cajaHora.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            javafx.scene.layout.HBox.setHgrow(txtHora, javafx.scene.layout.Priority.ALWAYS);
            
            // 3. 🚨 NUEVO: SELECTOR HÍBRIDO (Rápido + Calendario + Personalizado)
            ComboBox<String> comboRep = new ComboBox<>();
            comboRep.getStyleClass().add("combo-capsula");
            comboRep.setPrefWidth(240); 
            // 🚨 Agregamos la opción personalizada
            comboRep.getItems().addAll("NINGUNA", "DIARIA", "SEMANAL", "MENSUAL", "📅 ELEGIR FECHA...", "✏ PERSONALIZADO...");
            comboRep.setValue("NINGUNA");

            // Elemento A: El Calendario
            DatePicker dpHasta = new DatePicker();
            dpHasta.setPromptText("Próxima vez...");
            dpHasta.getStyleClass().add("date-picker-filtro");
            dpHasta.setVisible(false); 
            dpHasta.setManaged(false); 
            aplicarFormatoCalendario(dpHasta, formatoFechaActual);

            // Elemento B: 🚨 La Caja de Texto para días manuales
            TextField txtDias = new TextField();
            txtDias.setPromptText("Ej: 3 (días)");
            txtDias.getStyleClass().add("caja-texto");
            txtDias.setPrefHeight(35);
            txtDias.setPrefWidth(120);
            txtDias.setVisible(false);
            txtDias.setManaged(false);

            // Lógica de visualización
            comboRep.setOnAction(e -> {
                String seleccion = comboRep.getValue();
                boolean mostrarCalendario = seleccion.equals("📅 ELEGIR FECHA...");
                boolean mostrarTexto = seleccion.equals("✏ PERSONALIZADO...");
                
                dpHasta.setVisible(mostrarCalendario);
                dpHasta.setManaged(mostrarCalendario);
                
                txtDias.setVisible(mostrarTexto);
                txtDias.setManaged(mostrarTexto);
                
                dialogo.getDialogPane().getScene().getWindow().sizeToScene(); 
            });

            // Ambos conviven en la caja, pero solo uno se mostrará a la vez
            javafx.scene.layout.HBox cajaRep = new javafx.scene.layout.HBox(10, comboRep, dpHasta, txtDias);
            cajaRep.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            if (fecha == null) {
                comboRep.setDisable(true);
                comboRep.setValue("Requiere fecha inicial");
            }
            
            // 4. DISEÑO DE LA GRILLA
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(20); grid.setVgap(20); grid.setAlignment(javafx.geometry.Pos.CENTER);
            
            Label lblCat = new Label("Categoría:"); lblCat.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 15px;");
            Label lblHora = new Label("Hora (12h):"); lblHora.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 15px;");
            Label lblRep = new Label("Repetición:"); lblRep.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 15px;");
            
            grid.add(lblCat, 0, 0); grid.add(comboCat, 1, 0);
            grid.add(lblHora, 0, 1); grid.add(cajaHora, 1, 1);
            grid.add(lblRep, 0, 2); grid.add(cajaRep, 1, 2);
            
            dialogo.getDialogPane().setContent(grid);
            
            // 🚨 Pasamos 6 parámetros en lugar de 5
            dialogo.setResultConverter(boton -> {
                if (boton == btnGuardar) return new Object[]{comboCat.getValue(), txtHora.getText(), btnAmPm.getText(), comboRep.getValue(), dpHasta.getValue(), txtDias.getText()};
                return null; 
            });
            
            java.util.Optional<Object[]> resultado = dialogo.showAndWait();
            
            resultado.ifPresent(datos -> {
                String catDestino = (String) datos[0];
                String horaStr = datos[1] != null ? datos[1].toString().trim() : "";
                String amPm = (String) datos[2];
                String repOpcion = (String) datos[3];
                java.time.LocalDate fechaHasta = (java.time.LocalDate) datos[4];
                String diasManuales = datos[5] != null ? datos[5].toString().trim() : ""; // 🚨 Capturamos el texto
                
                // --- MATEMÁTICA DE LA HORA ---
                java.time.LocalTime horaLimite = null;
                if (!horaStr.isEmpty()) {
                    try {
                        if (horaStr.length() == 4 && horaStr.contains(":")) horaStr = "0" + horaStr;
                        String[] partes = horaStr.split(":");
                        int h = Integer.parseInt(partes[0]);
                        int m = Integer.parseInt(partes[1]);

                        if (amPm.equals("PM") && h < 12) h += 12;
                        if (amPm.equals("AM") && h == 12) h = 0;

                        horaLimite = java.time.LocalTime.of(h, m);
                    } catch (Exception e) {
                        System.out.println("Hora ignorada por formato incorrecto.");
                    }
                }

                // --- 🚨 MATEMÁTICA MULTI-REPETICIÓN ---
                String repeticion = "NINGUNA";
                if (repOpcion.equals("📅 ELEGIR FECHA...")) {
                    if (fecha != null && fechaHasta != null) {
                        long diasDeDiferencia = java.time.temporal.ChronoUnit.DAYS.between(fecha, fechaHasta);
                        if (diasDeDiferencia > 0) {
                            repeticion = String.valueOf(diasDeDiferencia); 
                        }
                    }
                } else if (repOpcion.equals("✏ PERSONALIZADO...")) {
                    if (!diasManuales.isEmpty()) {
                        try {
                            int numDias = Integer.parseInt(diasManuales);
                            if (numDias > 0) repeticion = String.valueOf(numDias);
                        } catch (NumberFormatException e) {
                            System.out.println("Intervalo no válido. Se omitió la repetición.");
                        }
                    }
                } else {
                    repeticion = repOpcion;
                }
                
                logica.agregarTarea(texto, fecha, horaLimite, repeticion, catDestino);
                actualizarInterfaz();
                
                txtNuevaTarea.clear();
                calendarioPrincipal.setValue(null);
            });
        }
    }

    private void accionAgregarSubTarea() {
        Tarea seleccionada = listaTareas.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            if (seleccionada.getIdTareaPadre() != null) {
                mostrarAlertaRapida("Acción no permitida", "No puedes agregar una subtarea dentro de otra subtarea.");
                return;
            }

            TextInputDialog dialogo = new TextInputDialog();
            dialogo.setTitle("Nueva Sub-tarea");
            dialogo.setHeaderText("Agregando paso a: " + seleccionada.getDescripcion());
            dialogo.setContentText("Descripción de la sub-tarea:");
            aplicarTemaOscuro(dialogo);

            Optional<String> resultado = dialogo.showAndWait();
            resultado.ifPresent(desc -> {
                if (!desc.trim().isEmpty()) {
                    logica.agregarSubTarea(seleccionada, desc.trim()); 
                    actualizarInterfaz();
                }
            });
        }
    }

    private void accionCompletar() {
        Tarea seleccionada = listaTareas.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            if (seleccionada.getIdTareaPadre() != null) {
                logica.alternarEstadoSubTarea(seleccionada); 
            } else {
                logica.alternarEstadoTarea(seleccionada); // Pasamos el objeto directo
            }
            actualizarInterfaz();
        }
    }

    private void accionEliminar() {
        Tarea seleccionada = listaTareas.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            String titulo = "Eliminar Tarea";
            String encabezado = "¿Estás seguro de eliminar esta tarea?";
            String contenido = "Tarea: " + seleccionada.getDescripcion();

            boolean tieneSubtareas = seleccionada.getIdTareaPadre() == null && seleccionada.getSubTareas() != null && !seleccionada.getSubTareas().isEmpty();
            boolean esRutina = seleccionada.getTipoRepeticion() != null && !seleccionada.getTipoRepeticion().equals("NINGUNA");

            if (tieneSubtareas) {
                titulo = "Eliminar Tarea Principal";
                encabezado = "¡Atención! Esta tarea contiene sub-tareas.";
                contenido = "Si eliminas esta tarea, TODAS sus sub-tareas (" + seleccionada.getSubTareas().size() + ") también serán destruidas.\n\n¿Deseas continuar?";
            } else if (esRutina) {
                titulo = "Eliminar Rutina";
                encabezado = "Estás a punto de eliminar una tarea recurrente.";
                contenido = "Si eliminas esta rutina, todo su HISTORIAL de veces completadas desaparecerá de tu base de datos.\n\n¿Deseas continuar?";
            } else if (seleccionada.getIdTareaPadre() != null) {
                titulo = "Eliminar Sub-tarea";
            }

            javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alerta.setTitle(titulo);
            alerta.setHeaderText(encabezado);
            alerta.setContentText(contenido);
            alerta.getButtonTypes().setAll(javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);

            try {
                alerta.initStyle(javafx.stage.StageStyle.UNDECORATED);
                alerta.getDialogPane().getStylesheets().add(getClass().getResource("/com/mitodolist/estilos.css").toExternalForm());
                alerta.getDialogPane().getStyleClass().add("mi-dialogo");
                Configuracion config = GestorConfiguracion.cargarConfiguracion();
                alerta.getDialogPane().setStyle("-color-acento: " + config.getColorAcento() + ";");
                if (config.isTemaClaro()) {
                    alerta.getDialogPane().getStyleClass().add("tema-claro");
                }
            } catch (Exception e) {}

            java.util.Optional<javafx.scene.control.ButtonType> respuesta = alerta.showAndWait();
            
            if (respuesta.isPresent() && respuesta.get() == javafx.scene.control.ButtonType.YES) {
                if (seleccionada.getIdTareaPadre() != null) {
                    Tarea padre = encontrarPadre(seleccionada);
                    if (padre != null) {
                        logica.eliminarSubTarea(padre, seleccionada);
                    }
                } else {
                    logica.eliminarTarea(seleccionada); 
                }
                actualizarInterfaz();
            }
        }
    }

    private void accionEditarDescripcion() {
        Tarea seleccionada = listaTareas.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            TextInputDialog dialogo = new TextInputDialog(seleccionada.getDescripcion());
            dialogo.setTitle("Editar Tarea");
            dialogo.setHeaderText("Modifica la descripción:");
            dialogo.setContentText("Nueva descripción:");
            aplicarTemaOscuro(dialogo);

            Optional<String> resultado = dialogo.showAndWait();
            resultado.ifPresent(nuevaDesc -> {
                if (!nuevaDesc.trim().isEmpty()) {
                    if (seleccionada.getIdTareaPadre() != null) {
                        seleccionada.setDescripcion(nuevaDesc);
                        GestorBaseDatos.actualizarTarea(seleccionada); 
                    } else {
                        logica.editarTarea(seleccionada, nuevaDesc); // Pasamos el objeto directo
                    }
                    actualizarInterfaz();
                }
            });
        }
    }

    private void accionEditarDetallesTiempo() {
        Tarea seleccionada = listaTareas.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            Dialog<Object[]> dialogo = new Dialog<>();
            dialogo.setTitle("Editar Tiempo y Rutina");
            dialogo.setHeaderText("Modificando: " + seleccionada.getDescripcion());
            aplicarTemaOscuro(dialogo);

            ButtonType btnGuardar = new ButtonType("Guardar Cambios", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            ButtonType btnLimpiar = new ButtonType("🗑 Limpiar Todo", javafx.scene.control.ButtonBar.ButtonData.LEFT); 
            dialogo.getDialogPane().getButtonTypes().addAll(btnGuardar, btnLimpiar, ButtonType.CANCEL);

            // --- 1. FECHA (Precargada) ---
            DatePicker dpFecha = new DatePicker(seleccionada.getFechaLimite());
            dpFecha.getStyleClass().add("date-picker-filtro");
            aplicarFormatoCalendario(dpFecha, formatoFechaActual);

            // --- 2. HORA (Matemática inversa para precargar 12h) ---
            TextField txtHora = new TextField();
            txtHora.setPromptText("Ej: 10:30");
            txtHora.getStyleClass().add("caja-texto");
            txtHora.setPrefHeight(35);

            Button btnAmPm = new Button("AM");
            btnAmPm.getStyleClass().add("boton-secundario");
            btnAmPm.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            btnAmPm.setPrefWidth(50);
            btnAmPm.setPrefHeight(35);

            btnAmPm.setOnAction(e -> btnAmPm.setText(btnAmPm.getText().equals("AM") ? "PM" : "AM"));

            if (seleccionada.getHoraLimite() != null) {
                int h = seleccionada.getHoraLimite().getHour();
                int m = seleccionada.getHoraLimite().getMinute();
                String ampm = "AM";
                
                if (h >= 12) { ampm = "PM"; if (h > 12) h -= 12; }
                if (h == 0) h = 12;
                
                txtHora.setText(String.format("%02d:%02d", h, m));
                btnAmPm.setText(ampm);
            }

            javafx.scene.layout.HBox cajaHora = new javafx.scene.layout.HBox(10, txtHora, btnAmPm);
            cajaHora.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            javafx.scene.layout.HBox.setHgrow(txtHora, javafx.scene.layout.Priority.ALWAYS);

            // --- 3. REPETICIÓN (Precargada) ---
            ComboBox<String> comboRep = new ComboBox<>();
            comboRep.getStyleClass().add("combo-capsula");
            comboRep.setPrefWidth(240);
            comboRep.getItems().addAll("NINGUNA", "DIARIA", "SEMANAL", "MENSUAL", "📅 ELEGIR FECHA...", "✏ PERSONALIZADO...");

            DatePicker dpHasta = new DatePicker();
            dpHasta.setPromptText("Próxima vez...");
            dpHasta.getStyleClass().add("date-picker-filtro");
            dpHasta.setVisible(false); dpHasta.setManaged(false);
            aplicarFormatoCalendario(dpHasta, formatoFechaActual);

            TextField txtDias = new TextField();
            txtDias.setPromptText("Ej: 3 (días)");
            txtDias.getStyleClass().add("caja-texto");
            txtDias.setPrefHeight(35); txtDias.setPrefWidth(120);
            txtDias.setVisible(false); txtDias.setManaged(false);

            String repActual = seleccionada.getTipoRepeticion();
            if (repActual == null || repActual.equals("NINGUNA")) {
                comboRep.setValue("NINGUNA");
            } else if (repActual.equals("DIARIA") || repActual.equals("SEMANAL") || repActual.equals("MENSUAL")) {
                comboRep.setValue(repActual);
            } else {
                comboRep.setValue("✏ PERSONALIZADO...");
                txtDias.setText(repActual);
                txtDias.setVisible(true); txtDias.setManaged(true);
            }

            comboRep.setOnAction(e -> {
                String seleccion = comboRep.getValue();
                boolean mostrarCalendario = seleccion.equals("📅 ELEGIR FECHA...");
                boolean mostrarTexto = seleccion.equals("✏ PERSONALIZADO...");

                dpHasta.setVisible(mostrarCalendario); dpHasta.setManaged(mostrarCalendario);
                txtDias.setVisible(mostrarTexto); txtDias.setManaged(mostrarTexto);
                dialogo.getDialogPane().getScene().getWindow().sizeToScene();
            });

            javafx.scene.layout.HBox cajaRep = new javafx.scene.layout.HBox(10, comboRep, dpHasta, txtDias);
            cajaRep.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Bloqueo dinámico si quitan la fecha
            dpFecha.valueProperty().addListener((obs, oldV, newV) -> {
                if (newV == null) {
                    comboRep.setDisable(true);
                    comboRep.setValue("Requiere fecha inicial");
                } else {
                    comboRep.setDisable(false);
                    if (comboRep.getValue().equals("Requiere fecha inicial")) comboRep.setValue("NINGUNA");
                }
            });
            if (seleccionada.getFechaLimite() == null) {
                comboRep.setDisable(true);
                comboRep.setValue("Requiere fecha inicial");
            }

            // --- 4. GRID VISUAL ---
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(20); grid.setVgap(20); grid.setAlignment(javafx.geometry.Pos.CENTER);

            Label lblFecha = new Label("Fecha Límite:"); lblFecha.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 15px;");
            Label lblHora = new Label("Hora (12h):"); lblHora.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 15px;");
            Label lblRep = new Label("Repetición:"); lblRep.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 15px;");

            grid.add(lblFecha, 0, 0); grid.add(dpFecha, 1, 0);
            grid.add(lblHora, 0, 1); grid.add(cajaHora, 1, 1);
            grid.add(lblRep, 0, 2); grid.add(cajaRep, 1, 2);

            dialogo.getDialogPane().setContent(grid);

            // Procesador de botones
            dialogo.setResultConverter(boton -> {
                if (boton == btnGuardar) return new Object[]{dpFecha.getValue(), txtHora.getText(), btnAmPm.getText(), comboRep.getValue(), dpHasta.getValue(), txtDias.getText()};
                if (boton == btnLimpiar) return new Object[]{"LIMPIAR"}; 
                return null;
            });

            Optional<Object[]> resultado = dialogo.showAndWait();

            resultado.ifPresent(datos -> {
                if (datos.length == 1 && datos[0].equals("LIMPIAR")) {
                    logica.editarDetallesTiempo(seleccionada, null, null, "NINGUNA");
                } else {
                    java.time.LocalDate nuevaFecha = (java.time.LocalDate) datos[0];
                    String horaStr = datos[1] != null ? datos[1].toString().trim() : "";
                    String amPm = (String) datos[2];
                    String repOpcion = (String) datos[3];
                    java.time.LocalDate fechaHasta = (java.time.LocalDate) datos[4];
                    String diasManuales = datos[5] != null ? datos[5].toString().trim() : "";

                    java.time.LocalTime nuevaHora = null;
                    if (!horaStr.isEmpty()) {
                        try {
                            if (horaStr.length() == 4 && horaStr.contains(":")) horaStr = "0" + horaStr;
                            String[] partes = horaStr.split(":");
                            int h = Integer.parseInt(partes[0]);
                            int m = Integer.parseInt(partes[1]);
                            if (amPm.equals("PM") && h < 12) h += 12;
                            if (amPm.equals("AM") && h == 12) h = 0;
                            nuevaHora = java.time.LocalTime.of(h, m);
                        } catch (Exception e) {}
                    }

                    String nuevaRep = "NINGUNA";
                    if (repOpcion.equals("📅 ELEGIR FECHA...")) {
                        if (nuevaFecha != null && fechaHasta != null) {
                            long diff = java.time.temporal.ChronoUnit.DAYS.between(nuevaFecha, fechaHasta);
                            if (diff > 0) nuevaRep = String.valueOf(diff);
                        }
                    } else if (repOpcion.equals("✏ PERSONALIZADO...")) {
                        if (!diasManuales.isEmpty()) {
                            try {
                                int numDias = Integer.parseInt(diasManuales);
                                if (numDias > 0) nuevaRep = String.valueOf(numDias);
                            } catch (Exception e) {}
                        }
                    } else if (!repOpcion.equals("Requiere fecha inicial")) {
                        nuevaRep = repOpcion;
                    }

                    logica.editarDetallesTiempo(seleccionada, nuevaFecha, nuevaHora, nuevaRep);
                }
                actualizarInterfaz();
            });
        }
    }

    private void accionEditarCategoria() {
        Tarea seleccionada = listaTareas.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            if (seleccionada.getIdTareaPadre() != null) {
                mostrarAlertaRapida("Acción no permitida", "Las subtareas siempre pertenecen a la misma categoría que su tarea principal.");
                return;
            }

            java.util.List<String> opciones = new java.util.ArrayList<>();
            // 🚨 CAMBIO AQUÍ: Añadimos ("TAREAS")
            for (Categoria cat : GestorBaseDatos.obtenerCategorias("TAREAS")) opciones.add(cat.getNombre());

            javafx.scene.control.ChoiceDialog<String> dialogo = new javafx.scene.control.ChoiceDialog<>(seleccionada.getCategoria(), opciones);
            dialogo.setTitle("Editar Categoría");
            dialogo.setHeaderText("Mover tarea a otra lista:");
            dialogo.setContentText("Nueva lista destino:");
            aplicarTemaOscuro(dialogo);

            Optional<String> resultado = dialogo.showAndWait();
            resultado.ifPresent(nuevaCat -> {
                logica.editarCategoria(seleccionada, nuevaCat); // Pasamos el objeto directo
                actualizarInterfaz();
            });
        }
    }

    // --- MÉTODOS AUXILIARES ---
    private Tarea encontrarPadre(Tarea subTarea) {
        for (Tarea principal : logica.obtenerTareasFiltradas(1, "Todas")) {
            if (principal.getId() == subTarea.getIdTareaPadre()) {
                return principal;
            }
        }
        return null;
    }

    private void mostrarAlertaRapida(String titulo, String mensaje) {
        javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        aplicarTemaOscuro(alerta);
        alerta.showAndWait();
    }

    // --- CRUD DE CATEGORÍAS (Sin cambios) ---
    @FXML
    public void accionCrearNuevaCategoria() {
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Nueva Lista");
        dialogo.setHeaderText("Tip: Presiona la tecla 'Windows + .' para elegir un emoji.");
        dialogo.setContentText("Nombre de la lista:");

        aplicarTemaOscuro(dialogo);

        Optional<String> resultado = dialogo.showAndWait();
        resultado.ifPresent(nombre -> {
            String texto = nombre.trim();
            if (!texto.isEmpty()) {
                if (Character.isLetterOrDigit(texto.codePointAt(0))) texto = "📁 " + texto;                
                // 🚨 FIX: Le decimos a SQLite desde dónde estamos creando la lista
                String tipoVista = panelHabitos.isVisible() ? "HABITOS" : "TAREAS";
                GestorBaseDatos.insertarCategoria(texto, "#FFFFFF", tipoVista);
                
                cargarMenuLateral();
            }
        });
    }

    private void accionRenombrarCategoria(Categoria cat) {
        TextInputDialog dialogo = new TextInputDialog(cat.getNombre());
        dialogo.setTitle("Renombrar Lista");
        dialogo.setHeaderText("Modifica el nombre de tu lista:");
        dialogo.setContentText("Nuevo nombre:");
        aplicarTemaOscuro(dialogo);
        Optional<String> resultado = dialogo.showAndWait();
        resultado.ifPresent(nuevoNombre -> {
            String texto = nuevoNombre.trim();
            if (!texto.isEmpty()) {
                if (Character.isLetterOrDigit(texto.codePointAt(0))) texto = "📁 " + texto;
                GestorBaseDatos.actualizarNombreCategoria(cat.getId(), texto);
                logica.sincronizarConBaseDatos();
                if (categoriaTareas.equals(cat.getNombre())) categoriaTareas = texto;
                if (categoriaHabitos.equals(cat.getNombre())) categoriaHabitos = texto;
                
                cargarMenuLateral();
                actualizarInterfaz(); 
                if (panelHabitos.isVisible()) renderizarMatrizHabitos();
            }
        });
    }

    private void accionEliminarCategoria(Categoria cat) {
        javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alerta.setTitle("Eliminar Lista");
        alerta.setHeaderText("¿Eliminar permanentemente '" + cat.getNombre() + "'?");
        alerta.setContentText("⚠ ADVERTENCIA: Si eliminas esta lista, TODAS las tareas en su interior serán destruidas.\n\n¿Deseas continuar?");
        alerta.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        aplicarTemaOscuro(alerta);
        Optional<ButtonType> respuesta = alerta.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            GestorBaseDatos.eliminarCategoria(cat.getId());
            logica.sincronizarConBaseDatos();
            if (categoriaTareas.equals(cat.getNombre())) categoriaTareas = "Todas";
            if (categoriaHabitos.equals(cat.getNombre())) categoriaHabitos = "Todas";
            
            cargarMenuLateral();
            actualizarInterfaz();
            if (panelHabitos.isVisible()) renderizarMatrizHabitos();
        }
    }

    @FXML
    public void accionCerrarSesion() {
        GestorBaseDatos.revocarRecordarSesion();
        App.cambiarEscena("VentanaLogin.fxml", "Acceso - Mi TodoList");
    }

    // --- MÉTODO AUXILIAR PARA INYECTAR CSS Y FÍSICA A LAS VENTANAS EMERGENTES ---
    private void aplicarTemaOscuro(Dialog<?> dialogo) {
        try {
            // 🚨 FIX 1 (MINIMIZACIÓN): Le decimos a Windows que esta sub-ventana pertenece a nuestra App principal
            if (panelHabitos != null && panelHabitos.getScene() != null && panelHabitos.getScene().getWindow() != null) {
                dialogo.initOwner(panelHabitos.getScene().getWindow());
            }

            dialogo.initStyle(javafx.stage.StageStyle.UNDECORATED);
            dialogo.getDialogPane().getStylesheets().add(getClass().getResource("/com/mitodolist/estilos.css").toExternalForm());
            dialogo.setGraphic(null); 
            dialogo.getDialogPane().getStyleClass().add("mi-dialogo");

            // 🚨 FIX 2 (COLOR PICKER BLANCO): Forzamos a la Escena interna a absorber el CSS
            javafx.application.Platform.runLater(() -> {
                if (dialogo.getDialogPane().getScene() != null) {
                    dialogo.getDialogPane().getScene().getStylesheets().add(getClass().getResource("/com/mitodolist/estilos.css").toExternalForm());
                }
            });

            // Inyectamos el color de acento global
            Configuracion config = GestorConfiguracion.cargarConfiguracion();
            dialogo.getDialogPane().setStyle("-color-acento: " + config.getColorAcento() + ";");
            if (config.isTemaClaro()) {
                dialogo.getDialogPane().getStyleClass().add("tema-claro");
            }

            final double[] xOffset = {0};
            final double[] yOffset = {0};

            dialogo.getDialogPane().setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });

            dialogo.getDialogPane().setOnMouseDragged(event -> {
                javafx.stage.Stage stage = (javafx.stage.Stage) dialogo.getDialogPane().getScene().getWindow();
                stage.setX(event.getScreenX() - xOffset[0]);
                stage.setY(event.getScreenY() - yOffset[0]);
            });

        } catch (Exception e) {
            System.out.println("No se pudo cargar el CSS en el diálogo.");
        }
    }

    @FXML
    public void accionLimpiarFiltros() {
        if (txtBuscador != null) txtBuscador.clear();
        if (calendarioFiltro != null) calendarioFiltro.setValue(null);
    }

    @FXML
    public void abrirConfiguracion() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/mitodolist/VentanaConfiguracion.fxml"));
            javafx.scene.Parent root = loader.load();
            
            // 🚨 INYECTAMOS EL TEMA GLOBAL A LA VENTANA DE CONFIGURACIÓN
            Configuracion config = GestorConfiguracion.cargarConfiguracion();
            root.setStyle("-color-acento: " + config.getColorAcento() + ";");
            if (config.isTemaClaro()) {
                root.getStyleClass().add("tema-claro");
            }
            
            javafx.scene.Scene escenaConfig = new javafx.scene.Scene(root);
            escenaConfig.getStylesheets().add(getClass().getResource("/com/mitodolist/estilos.css").toExternalForm());
            
            javafx.stage.Stage stageConfig = new javafx.stage.Stage();
            stageConfig.initStyle(javafx.stage.StageStyle.UNDECORATED); 
            stageConfig.initModality(javafx.stage.Modality.APPLICATION_MODAL); 
            
            // 🚨 FIX: Cadena de Propiedad. Le decimos a Windows que la ventana de Tareas es la dueña de la Configuración.
            // Esto evita que la aplicación colapse o se minimice al cerrar sub-ventanas.
            if (listaTareas != null && listaTareas.getScene() != null && listaTareas.getScene().getWindow() != null) {
                stageConfig.initOwner(listaTareas.getScene().getWindow());
            }
            
            stageConfig.setTitle("Configuración - Mi ToDo List");
            stageConfig.setScene(escenaConfig);

            stageConfig.showAndWait(); 
            actualizarInterfaz();            
            
            // 🚨 SOLUCIÓN AL ERROR 2: FORZAR ACTUALIZACIÓN DEL COLOR EN VIVO EN LA VENTANA PRINCIPAL
            Configuracion configActualizada = GestorConfiguracion.cargarConfiguracion();
            
            // 1. Refrescamos el color de acento general
            listaTareas.getScene().getRoot().setStyle("-color-acento: " + configActualizada.getColorAcento() + ";");
            
            // 2. Encendemos o apagamos la clase del Modo Claro
            if (configActualizada.isTemaClaro()) {
                if (!listaTareas.getScene().getRoot().getStyleClass().contains("tema-claro")) {
                    listaTareas.getScene().getRoot().getStyleClass().add("tema-claro");
                }
            } else {
                listaTareas.getScene().getRoot().getStyleClass().remove("tema-claro");
            }
            
        } catch (java.io.IOException e) {
            System.out.println("Error al abrir el panel de configuración: " + e.getMessage());
        }
    }

    // --- MÉTODO PARA OBLIGAR A LOS CALENDARIOS A RESPETAR EL FORMATO ---
    private void aplicarFormatoCalendario(DatePicker dp, String formato) {
        if (dp == null) return;
        java.time.format.DateTimeFormatter formateador = java.time.format.DateTimeFormatter.ofPattern(formato);
        
        dp.setConverter(new javafx.util.StringConverter<java.time.LocalDate>() {
            @Override
            public String toString(java.time.LocalDate fecha) {
                return (fecha != null) ? formateador.format(fecha) : "";
            }
            @Override
            public java.time.LocalDate fromString(String cadena) {
                return (cadena != null && !cadena.isEmpty()) ? java.time.LocalDate.parse(cadena, formateador) : null;
            }
        });
    }

    // 🚨 NUEVO V7: Se acciona directamente desde el botón [🔄] de la tarea
    private void mostrarHistorial(Tarea seleccionada) {
        ArrayList<String> historial = GestorBaseDatos.obtenerHistorialTarea(seleccionada.getDescripcion());
        
        javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alerta.setTitle("Detalles de Tarea");
        alerta.setHeaderText("Historial de repeticiones:\n" + seleccionada.getDescripcion());
        
        if (historial.isEmpty()) {
            alerta.setContentText("Aún no tienes un historial completado para esta rutina.");
        } else {
            // 🎨 FIX VISUAL: Usamos un ListView en lugar de TextArea. Es 100% compatible con tu modo oscuro.
            ListView<String> listaHistorial = new ListView<>();
            listaHistorial.getItems().addAll(historial);
            listaHistorial.setPrefHeight(150);
            listaHistorial.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
            
            // Le decimos a la lista cómo pintar el texto internamente
            listaHistorial.setCellFactory(param -> new javafx.scene.control.ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: -texto-principal; -fx-background-color: transparent; -fx-font-size: 14px;");
                    }
                }
            });
            
            alerta.getDialogPane().setContent(listaHistorial);
        }
        
        aplicarTemaOscuro(alerta); 
        alerta.showAndWait();
    }

    // =======================================================
    // ⏳ MÁQUINA DEL TIEMPO (NAVEGACIÓN DE MESES)
    // =======================================================
    @FXML
    public void mesAnteriorHabitos() {
        mesNavegacion = mesNavegacion.minusMonths(1);
        actualizarLabelMes();
        renderizarMatrizHabitos();
    }

    @FXML
    public void mesSiguienteHabitos() {
        mesNavegacion = mesNavegacion.plusMonths(1);
        actualizarLabelMes();
        renderizarMatrizHabitos();
    }

    private void actualizarLabelMes() {
        java.time.format.DateTimeFormatter formatoMes = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.forLanguageTag("es-ES"));
        String textoMes = mesNavegacion.format(formatoMes);
        lblMesHabitos.setText(textoMes.substring(0, 1).toUpperCase() + textoMes.substring(1));
    }

    // =======================================================
    // 🌱 CONTROLADOR DE VISTAS (TAREAS vs HÁBITOS)
    // =======================================================
    @FXML
    public void accionVistaTareas() {
        panelHabitos.setVisible(false);
        panelTareas.setVisible(true);
        
        btnVistaTareas.getStyleClass().setAll("button", "boton-acento");
        btnVistaHabitos.getStyleClass().setAll("button", "boton-secundario");
        renderizarLeyendaAnimos();
        
        // 🚨 FIX: Obligamos al menú a recargar las listas EXCLUSIVAS de Tareas
        cargarMenuLateral(); 
        resaltarCategoriaActiva(); 
    }

    @FXML
    public void accionVistaHabitos() {
        panelTareas.setVisible(false);
        panelHabitos.setVisible(true);
        
        btnVistaHabitos.getStyleClass().setAll("button", "boton-acento");
        btnVistaTareas.getStyleClass().setAll("button", "boton-secundario");
        
        actualizarLabelMes();
        renderizarMatrizHabitos();
        renderizarLeyendaAnimos();
        
        // 🚨 FIX: Obligamos al menú a recargar las listas EXCLUSIVAS de Hábitos
        cargarMenuLateral(); 
        resaltarCategoriaActiva(); 
    }

    // =======================================================
    // 🧠 MOTOR DE DIBUJADO DE LA MATRIZ DE HÁBITOS
    // =======================================================
    private void renderizarMatrizHabitos() {
        contenedorMatrizHabitos.getChildren().clear();

        java.time.YearMonth mesActual = mesNavegacion;
        int diasDelMes = mesActual.lengthOfMonth();
        java.time.LocalDate hoyExacto = java.time.LocalDate.now(); 

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(8); grid.setVgap(15);
        grid.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        // 1. CABECERA DE DÍAS (Fila 0)
        Label lblTitulo = new Label("Tus Hábitos");
        lblTitulo.setStyle("-fx-text-fill: -texto-secundario; -fx-font-size: 14px; -fx-font-weight: bold;");
        lblTitulo.setPrefWidth(200); 
        grid.add(lblTitulo, 0, 0);

        for (int i = 1; i <= diasDelMes; i++) {
            Label lblDia = new Label(String.valueOf(i));
            lblDia.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 13px; -fx-font-weight: bold;");
            lblDia.setMinWidth(22); lblDia.setAlignment(javafx.geometry.Pos.CENTER);
            
            if (i == hoyExacto.getDayOfMonth() && mesActual.getYear() == hoyExacto.getYear() && mesActual.getMonthValue() == hoyExacto.getMonthValue()) {
                lblDia.setStyle("-fx-text-fill: -color-acento; -fx-font-size: 15px; -fx-font-weight: bold;");
            }
            grid.add(lblDia, i, 0);
        }

        // =====================================================================
        // 2. 🌟 FASE 5: LA FILA DEL CIELO (MOOD TRACKER - COLORES) -> Fila 1
        // =====================================================================
        Label lblAnimo = new Label("💡 Estado de Ánimo");
        lblAnimo.setStyle("-fx-text-fill: -texto-secundario; -fx-font-size: 14px; -fx-font-weight: bold;");
        lblAnimo.setPrefWidth(200);
        grid.add(lblAnimo, 0, 1);

        // 🚨 AQUÍ ESTABA EL ERROR QUE VEÍAS EN ROJO. AHORA USA 'EstadoAnimo'
        java.util.HashMap<Integer, EstadoAnimo> animosMes = GestorBaseDatos.obtenerAnimosMes(mesActual.getYear(), mesActual.getMonthValue());

        for (int dia = 1; dia <= diasDelMes; dia++) {
            java.time.LocalDate fechaLoop = java.time.LocalDate.of(mesActual.getYear(), mesActual.getMonthValue(), dia);
            
            EstadoAnimo estadoGuardado = animosMes.get(dia);
            javafx.scene.shape.Circle circuloAnimo = new javafx.scene.shape.Circle(9);
            circuloAnimo.setStrokeWidth(1);

            if (estadoGuardado != null) {
                // Si hay ánimo registrado, pintamos el círculo con su color real
                circuloAnimo.setFill(javafx.scene.paint.Color.web(estadoGuardado.getColorHex()));
                circuloAnimo.setStroke(javafx.scene.paint.Color.web("#555555"));
                circuloAnimo.setCursor(javafx.scene.Cursor.HAND);
            } else {
                // Si no hay ánimo, dejamos un "Puntito" diminuto y sutil como placeholder
                circuloAnimo.setRadius(3);
                circuloAnimo.setFill(javafx.scene.paint.Color.web("#555555"));
                circuloAnimo.setStroke(javafx.scene.paint.Color.TRANSPARENT);
            }

            if (fechaLoop.isAfter(hoyExacto)) {
                circuloAnimo.setCursor(javafx.scene.Cursor.DEFAULT);
                circuloAnimo.setFill(javafx.scene.paint.Color.TRANSPARENT); 
            } else {
                // Efecto expansivo elegante al pasar el ratón
                circuloAnimo.setOnMouseEntered(e -> { if (estadoGuardado == null) circuloAnimo.setRadius(5); });
                circuloAnimo.setOnMouseExited(e -> { if (estadoGuardado == null) circuloAnimo.setRadius(3); });
                circuloAnimo.setOnMouseClicked(e -> mostrarSelectorAnimo(circuloAnimo, fechaLoop));
            }

            javafx.scene.layout.HBox cajaAnimo = new javafx.scene.layout.HBox(circuloAnimo);
            cajaAnimo.setAlignment(javafx.geometry.Pos.CENTER);
            grid.add(cajaAnimo, dia, 1);
        }

        // =====================================================================
        // 3. LA MATRIZ DE HÁBITOS -> Inicia en la Fila 2
        // =====================================================================
        ArrayList<Habito> listaHabitosBD = GestorBaseDatos.obtenerHabitos();
        ArrayList<Habito> listaHabitos = new ArrayList<>();
        
        int idCatFiltro = categoriaHabitos.equals("Todas") ? -1 : GestorBaseDatos.obtenerIdCategoria(categoriaHabitos, "HABITOS");
        for (Habito h : listaHabitosBD) {
            if (idCatFiltro == -1 || h.getIdCategoria() == idCatFiltro) {
                listaHabitos.add(h);
            }
        }

        if (listaHabitos.isEmpty()) {
            Label lblVacio = new Label("Aún no tienes hábitos. ¡Crea uno nuevo!");
            lblVacio.setStyle("-fx-text-fill: -texto-secundario; -fx-font-size: 14px; -fx-font-style: italic;");
            grid.add(lblVacio, 0, 2, diasDelMes + 1, 1);
        } else {
            // 🚨 IMPORTANTE: La variable fila ahora empieza en 2 para respetar el cielo
            int fila = 2; 
            for (Habito h : listaHabitos) {
                
                Button btnHabito = new Button("▶ " + h.getNombre());
                btnHabito.setStyle("-fx-background-color: transparent; -fx-text-fill: -texto-principal; -fx-font-size: 15px; -fx-alignment: center-left; -fx-cursor: hand; -fx-font-weight: bold;");
                btnHabito.setPrefWidth(200);
                btnHabito.setMaxWidth(200);

                javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(h.getNombre());
                tooltip.setStyle("-fx-font-size: 13px; -fx-background-color: -bg-barra; -fx-text-fill: -texto-principal; -fx-border-color: -color-acento; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
                tooltip.setShowDelay(javafx.util.Duration.millis(150)); 
                btnHabito.setTooltip(tooltip);
                
                ContextMenu menuHabito = new ContextMenu();
                MenuItem menuEditar = new MenuItem("🖊 Editar Hábito");
                menuEditar.setOnAction(e -> accionEditarHabito(h));
                MenuItem menuEliminar = new MenuItem("🗑 Eliminar Hábito (Perderás todo su historial)");
                menuEliminar.setOnAction(e -> accionEliminarHabito(h));
                menuHabito.getItems().addAll(menuEditar, menuEliminar);
                btnHabito.setContextMenu(menuHabito);
                
                grid.add(btnHabito, 0, fila);

                ArrayList<java.time.LocalDate> historialCompleto = new ArrayList<>();
                for (int m = 0; m < 6; m++) {
                    java.time.YearMonth ym = mesActual.minusMonths(m);
                    for (int d : GestorBaseDatos.obtenerDiasCompletadosMes(h.getId(), ym.getYear(), ym.getMonthValue())) {
                        historialCompleto.add(java.time.LocalDate.of(ym.getYear(), ym.getMonthValue(), d));
                    }
                }

                for (int dia = 1; dia <= diasDelMes; dia++) {
                    java.time.LocalDate fechaLoop = java.time.LocalDate.of(mesActual.getYear(), mesActual.getMonthValue(), dia);

                    if (fechaLoop.isBefore(h.getFechaCreacion())) {
                        javafx.scene.shape.Circle circuloBloqueado = new javafx.scene.shape.Circle(9);
                        circuloBloqueado.setFill(javafx.scene.paint.Color.TRANSPARENT);
                        circuloBloqueado.setStroke(javafx.scene.paint.Color.web("#333333"));
                        Label lblX = new Label("✖");
                        lblX.setStyle("-fx-text-fill: #555555; -fx-font-size: 10px;");
                        javafx.scene.layout.StackPane cajaBloqueada = new javafx.scene.layout.StackPane(circuloBloqueado, lblX);
                        grid.add(cajaBloqueada, dia, fila);
                    } else {
                        javafx.scene.shape.Circle circulo = new javafx.scene.shape.Circle(9);
                        boolean completado = historialCompleto.contains(fechaLoop);
                        
                        circulo.setFill(completado ? javafx.scene.paint.Color.web(h.getColorHex()) : javafx.scene.paint.Color.web("#333333"));
                        circulo.setStroke(javafx.scene.paint.Color.web("#555555"));
                        circulo.setStrokeWidth(1);

                        if (fechaLoop.isAfter(hoyExacto)) {
                            circulo.setCursor(javafx.scene.Cursor.DEFAULT);
                        } else {
                            circulo.setCursor(javafx.scene.Cursor.HAND);
                            circulo.setOnMouseClicked(e -> {
                                if (circulo.getFill().equals(javafx.scene.paint.Color.web(h.getColorHex()))) {
                                    GestorBaseDatos.eliminarRegistroHabito(h.getId(), fechaLoop);
                                } else {
                                    GestorBaseDatos.registrarHabito(h.getId(), fechaLoop);
                                }
                                renderizarMatrizHabitos(); 
                            });
                        }
                        javafx.scene.layout.HBox cajaCirculo = new javafx.scene.layout.HBox(circulo);
                        cajaCirculo.setAlignment(javafx.geometry.Pos.CENTER);
                        grid.add(cajaCirculo, dia, fila);
                    }
                }

                javafx.scene.layout.VBox panelGrafico = new javafx.scene.layout.VBox(10);
                panelGrafico.setVisible(false); 
                panelGrafico.setManaged(false); 
                panelGrafico.setStyle("-fx-background-color: -bg-caja; -fx-background-radius: 10; -fx-padding: 15; -fx-border-color: -borde-color; -fx-border-radius: 10;");
                
                javafx.scene.layout.HBox cabeceraGrafico = new javafx.scene.layout.HBox(10);
                cabeceraGrafico.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                
                Label lblTituloGrafico = new Label("Análisis de Rendimiento");
                lblTituloGrafico.setStyle("-fx-text-fill: -texto-secundario; -fx-font-size: 14px; -fx-font-weight: bold;");
                
                javafx.scene.layout.Region spacerGrafico = new javafx.scene.layout.Region();
                javafx.scene.layout.HBox.setHgrow(spacerGrafico, javafx.scene.layout.Priority.ALWAYS);
                
                ComboBox<String> comboVistas = new ComboBox<>();
                comboVistas.getItems().addAll("📅 Últimos 7 Días", "📈 Últimas 8 Semanas", "📊 Últimos 6 Meses");
                comboVistas.setValue("📈 Últimas 8 Semanas");
                comboVistas.getStyleClass().add("combo-capsula"); 
                
                cabeceraGrafico.getChildren().addAll(lblTituloGrafico, spacerGrafico, comboVistas);
                
                javafx.scene.layout.StackPane contenedorChart = new javafx.scene.layout.StackPane();
                contenedorChart.setPrefHeight(220);
                
                Runnable dibujarGrafico = () -> {
                    contenedorChart.getChildren().clear();
                    String vistaElegida = comboVistas.getValue();
                    
                    java.time.LocalDate ancla = (mesActual.equals(java.time.YearMonth.now())) ? hoyExacto : mesActual.atEndOfMonth();
                    
                    CategoryAxis ejeX = new CategoryAxis();
                    NumberAxis ejeY = new NumberAxis();
                    ejeY.setMinorTickVisible(false);
                    ejeY.setAutoRanging(false); 
                    ejeY.setLowerBound(0);

                    XYChart.Series<String, Number> serie = new XYChart.Series<>();

                    // 🧠 Memoria RAM: Caché para evitar saturar la base de datos con peticiones repetidas
                    java.util.HashMap<String, java.util.HashMap<Integer, EstadoAnimo>> cacheAnimos = new java.util.HashMap<>();

                    if (vistaElegida.contains("7 Días")) {
                        ejeY.setUpperBound(1); 
                        ejeY.setTickUnit(1);
                        ejeY.setLabel("Estado (1 = Logrado)"); // Etiqueta del eje
                        javafx.scene.chart.LineChart<String, Number> chart = new javafx.scene.chart.LineChart<>(ejeX, ejeY);
                        chart.setLegendVisible(false); chart.setAnimated(false); chart.setCreateSymbols(true);
                        
                        for (int d = 6; d >= 0; d--) {
                            java.time.LocalDate dia = ancla.minusDays(d);
                            int valor = historialCompleto.contains(dia) ? 1 : 0;
                            String nombreDia = dia.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.forLanguageTag("es-ES"));
                            nombreDia = nombreDia.substring(0, 1).toUpperCase() + nombreDia.substring(1);
                            
                            // Obtenemos el mes/año para buscar en el caché
                            String llaveMes = dia.getYear() + "-" + dia.getMonthValue();
                            cacheAnimos.putIfAbsent(llaveMes, GestorBaseDatos.obtenerAnimosMes(dia.getYear(), dia.getMonthValue()));
                            EstadoAnimo estadoDelDia = cacheAnimos.get(llaveMes).get(dia.getDayOfMonth());
                            
                            XYChart.Data<String, Number> dato = new XYChart.Data<>(nombreDia + " " + dia.getDayOfMonth(), valor);
                            dato.nodeProperty().addListener((obs, o, n) -> {
                                if (n != null) {
                                    n.setStyle("-fx-background-color: " + h.getColorHex() + ", -bg-caja; -fx-background-insets: 0, 2; -fx-padding: 5px;");
                                    
                                    // 🚨 TOOLTIP INTELIGENTE (Día exacto)
                                    String txtEstado = (estadoDelDia != null) ? estadoDelDia.getNombre() : "No registrado";
                                    String colorBorde = (estadoDelDia != null) ? estadoDelDia.getColorHex() : h.getColorHex();
                                    String txtCompletado = (valor == 1) ? "✅ Hábito logrado" : "❌ No completado";
                                    
                                    javafx.scene.control.Tooltip tt = new javafx.scene.control.Tooltip(
                                        dia.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")) + "\n" +
                                        txtCompletado + "\n" +
                                        "💡 Ánimo: " + txtEstado
                                    );
                                    tt.setShowDelay(javafx.util.Duration.millis(150)); // Ultra rápido
                                    tt.setShowDuration(javafx.util.Duration.INDEFINITE);
                                    tt.setStyle("-fx-font-size: 13px; -fx-background-color: -bg-barra; -fx-text-fill: -texto-principal; -fx-border-color: " + colorBorde + "; -fx-border-width: 2px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
                                    javafx.scene.control.Tooltip.install(n, tt);
                                }
                            });
                            serie.getData().add(dato);
                        }
                        serie.nodeProperty().addListener((obs, o, n) -> { if (n != null) n.setStyle("-fx-stroke: " + h.getColorHex() + "; -fx-stroke-width: 3px;"); });
                        chart.getData().add(serie);
                        contenedorChart.getChildren().add(chart);

                    } else if (vistaElegida.contains("8 Semanas")) {
                        ejeY.setUpperBound(7); 
                        ejeY.setTickUnit(1);
                        ejeY.setLabel("Días completados"); // Etiqueta
                        javafx.scene.chart.LineChart<String, Number> chart = new javafx.scene.chart.LineChart<>(ejeX, ejeY);
                        chart.setLegendVisible(false); chart.setAnimated(false); chart.setCreateSymbols(true);
                        
                        for (int w = 7; w >= 0; w--) {
                            java.time.LocalDate finSemana = ancla.minusDays(w * 7);
                            java.time.LocalDate inicioSemana = finSemana.minusDays(6);
                            int exitos = 0;
                            
                            // 🧠 MOTOR: Contar la emoción predominante y memorizar su color
                            java.util.HashMap<String, Integer> conteoEmociones = new java.util.HashMap<>();
                            java.util.HashMap<String, String> colorEmociones = new java.util.HashMap<>(); // 🚨 NUEVO: Mapa de colores
                            
                            for (java.time.LocalDate f = inicioSemana; !f.isAfter(finSemana); f = f.plusDays(1)) {
                                if (historialCompleto.contains(f)) exitos++;
                                
                                String llaveMes = f.getYear() + "-" + f.getMonthValue();
                                cacheAnimos.putIfAbsent(llaveMes, GestorBaseDatos.obtenerAnimosMes(f.getYear(), f.getMonthValue()));
                                EstadoAnimo ea = cacheAnimos.get(llaveMes).get(f.getDayOfMonth());
                                if (ea != null) {
                                    conteoEmociones.put(ea.getNombre(), conteoEmociones.getOrDefault(ea.getNombre(), 0) + 1);
                                    colorEmociones.put(ea.getNombre(), ea.getColorHex()); // 🚨 Guardamos su color real
                                }
                            }
                            
                            String emocionPredominante = "Ninguna";
                            String colorPredominante = h.getColorHex(); // Fallback: Color del hábito por defecto si no hay ánimo
                            int max = 0;
                            for (java.util.Map.Entry<String, Integer> entry : conteoEmociones.entrySet()) {
                                if (entry.getValue() > max) {
                                    max = entry.getValue();
                                    emocionPredominante = entry.getKey();
                                    colorPredominante = colorEmociones.get(entry.getKey()); // 🚨 Extraemos el color ganador
                                }
                            }
                            
                            String mesCorto = finSemana.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.forLanguageTag("es-ES"));
                            mesCorto = mesCorto.substring(0, 1).toUpperCase() + mesCorto.substring(1);
                            
                            XYChart.Data<String, Number> dato = new XYChart.Data<>(finSemana.getDayOfMonth() + "/" + mesCorto, exitos);
                            
                            final String emoFinal = emocionPredominante;
                            final String colorFinal = colorPredominante; // 🚨 Congelamos el color para la Lambda
                            final int exitosFinal = exitos;
                            dato.nodeProperty().addListener((obs, o, n) -> {
                                if (n != null) {
                                    n.setStyle("-fx-background-color: " + h.getColorHex() + ", -bg-caja; -fx-background-insets: 0, 2; -fx-padding: 5px;");
                                    
                                    javafx.scene.control.Tooltip tt = new javafx.scene.control.Tooltip(
                                        "Semana: " + inicioSemana.getDayOfMonth() + "/" + inicioSemana.getMonthValue() + " al " + finSemana.getDayOfMonth() + "/" + finSemana.getMonthValue() + "\n" +
                                        "🎯 Logrado: " + exitosFinal + " de 7 días\n" +
                                        "💡 Ánimo frecuente: " + emoFinal
                                    );
                                    tt.setShowDelay(javafx.util.Duration.millis(150));
                                    tt.setShowDuration(javafx.util.Duration.INDEFINITE);
                                    // 🚨 FIX: Inyectamos colorFinal al borde
                                    tt.setStyle("-fx-font-size: 13px; -fx-background-color: -bg-barra; -fx-text-fill: -texto-principal; -fx-border-color: " + colorFinal + "; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
                                    javafx.scene.control.Tooltip.install(n, tt);
                                }
                            });
                            serie.getData().add(dato);
                        }
                        serie.nodeProperty().addListener((obs, o, n) -> { if (n != null) n.setStyle("-fx-stroke: " + h.getColorHex() + "; -fx-stroke-width: 3px;"); });
                        chart.getData().add(serie);
                        contenedorChart.getChildren().add(chart);

                    } else {
                        ejeY.setAutoRanging(true); 
                        ejeY.setLabel("Días completados"); // Etiqueta
                        BarChart<String, Number> chart = new BarChart<>(ejeX, ejeY);
                        chart.setLegendVisible(false); chart.setAnimated(false);
                        
                        for (int m = 5; m >= 0; m--) {
                            java.time.YearMonth ym = mesActual.minusMonths(m);
                            int exitos = 0;
                            
                            // 🧠 MOTOR: Contar la emoción predominante del mes y memorizar color
                            String llaveMes = ym.getYear() + "-" + ym.getMonthValue();
                            cacheAnimos.putIfAbsent(llaveMes, GestorBaseDatos.obtenerAnimosMes(ym.getYear(), ym.getMonthValue()));
                            java.util.HashMap<String, Integer> conteoEmociones = new java.util.HashMap<>();
                            java.util.HashMap<String, String> colorEmociones = new java.util.HashMap<>(); // 🚨 NUEVO
                            
                            for (java.time.LocalDate f : historialCompleto) {
                                if (f.getYear() == ym.getYear() && f.getMonthValue() == ym.getMonthValue()) exitos++;
                            }
                            
                            for (EstadoAnimo ea : cacheAnimos.get(llaveMes).values()) {
                                conteoEmociones.put(ea.getNombre(), conteoEmociones.getOrDefault(ea.getNombre(), 0) + 1);
                                colorEmociones.put(ea.getNombre(), ea.getColorHex()); // 🚨 Guardamos su color
                            }
                            
                            String emocionPredominante = "Ninguna";
                            String colorPredominante = h.getColorHex(); // Fallback
                            int max = 0;
                            for (java.util.Map.Entry<String, Integer> entry : conteoEmociones.entrySet()) {
                                if (entry.getValue() > max) {
                                    max = entry.getValue();
                                    emocionPredominante = entry.getKey();
                                    colorPredominante = colorEmociones.get(entry.getKey()); // 🚨 Extraemos el color ganador
                                }
                            }
                            
                            String nombreMes = ym.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.forLanguageTag("es-ES"));
                            nombreMes = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1);
                            
                            XYChart.Data<String, Number> dato = new XYChart.Data<>(nombreMes, exitos);
                            
                            final String emoFinal = emocionPredominante;
                            final String colorFinal = colorPredominante; // 🚨 Congelamos el color para la Lambda
                            final int exitosFinal = exitos;
                            final String mesFinal = nombreMes; 
                            
                            dato.nodeProperty().addListener((obs, o, n) -> {
                                if (n != null) {
                                    n.setStyle("-fx-bar-fill: " + h.getColorHex() + ";");
                                    
                                    javafx.scene.control.Tooltip tt = new javafx.scene.control.Tooltip(
                                        "Mes: " + mesFinal + " " + ym.getYear() + "\n" +
                                        "🎯 Logrado: " + exitosFinal + " días\n" +
                                        "💡 Ánimo frecuente: " + emoFinal
                                    );
                                    tt.setShowDelay(javafx.util.Duration.millis(150));
                                    tt.setShowDuration(javafx.util.Duration.INDEFINITE);
                                    // 🚨 FIX: Inyectamos colorFinal al borde
                                    tt.setStyle("-fx-font-size: 13px; -fx-background-color: -bg-barra; -fx-text-fill: -texto-principal; -fx-border-color: " + colorFinal + "; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
                                    javafx.scene.control.Tooltip.install(n, tt);
                                }
                            });
                            serie.getData().add(dato);
                        }
                        chart.getData().add(serie);
                        contenedorChart.getChildren().add(chart);
                    }
                };

                comboVistas.setOnAction(e -> dibujarGrafico.run());
                dibujarGrafico.run(); 

                panelGrafico.getChildren().addAll(cabeceraGrafico, contenedorChart);
                javafx.scene.layout.GridPane.setColumnSpan(panelGrafico, diasDelMes + 1);
                grid.add(panelGrafico, 0, fila + 1);

                btnHabito.setOnAction(e -> {
                    boolean estaAbierto = panelGrafico.isVisible();
                    panelGrafico.setVisible(!estaAbierto);
                    panelGrafico.setManaged(!estaAbierto);
                    btnHabito.setText((estaAbierto ? "▶ " : "▼ ") + h.getNombre());
                });

                fila += 2; 
            }
        }
        contenedorMatrizHabitos.getChildren().add(grid);
    }

    // =======================================================
    // 🧠 FASE 5: SELECTOR DE EMOJIS (MOOD TRACKER)
    // =======================================================

    private void renderizarLeyendaAnimos() {
        javafx.scene.layout.Pane panelDerecho = (javafx.scene.layout.Pane) lblPendientes.getParent();
        panelDerecho.getChildren().removeIf(node -> "leyenda-animos".equals(node.getId())); 

        boolean esTareas = panelTareas.isVisible();
        
        // 🚨 FIX 1: Ocultamos TODOS los Labels del panel derecho (Incluyendo "RESUMEN")
        for (javafx.scene.Node nodo : panelDerecho.getChildren()) {
            if (nodo instanceof Label) {
                nodo.setVisible(esTareas);
                nodo.setManaged(esTareas);
            }
        }

        if (!esTareas) {
            javafx.scene.layout.VBox leyenda = new javafx.scene.layout.VBox(10);
            leyenda.setId("leyenda-animos");
            leyenda.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            leyenda.setStyle("-fx-padding: 10 0 10 0;");

            Label lblTitulo = new Label("Estado de Ánimo");
            lblTitulo.setStyle("-fx-text-fill: -texto-secundario; -fx-font-weight: bold; -fx-font-size: 15px;");
            leyenda.getChildren().add(lblTitulo);

            for (EstadoAnimo estado : GestorBaseDatos.obtenerEstadosAnimo()) {
                javafx.scene.shape.Circle circulo = new javafx.scene.shape.Circle(7, javafx.scene.paint.Color.web(estado.getColorHex()));
                Label lblNombre = new Label(estado.getNombre());
                lblNombre.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 14px;");
                
                javafx.scene.layout.HBox fila = new javafx.scene.layout.HBox(8, circulo, lblNombre);
                fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                leyenda.getChildren().add(fila);
            }

            Button btnEditar = new Button("⚙ Editar Ánimos");
            btnEditar.getStyleClass().add("boton-transparente");
            btnEditar.setStyle("-fx-text-fill: -color-acento; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 15 0 0 0;");
            btnEditar.setOnAction(e -> abrirGestorAnimos());
            leyenda.getChildren().add(btnEditar);

            int size = panelDerecho.getChildren().size();
            panelDerecho.getChildren().add(size > 0 ? size - 1 : 0, leyenda);
        }
    }

    private void abrirGestorAnimos() {
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.setTitle("Editor de Ánimos");
        dialogo.setHeaderText("Personaliza tus emociones (Máximo 10)");
        aplicarTemaOscuro(dialogo);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        javafx.scene.layout.VBox contenedorPrincipal = new javafx.scene.layout.VBox(15);
        // 🚨 Lo hacemos un poco más ancho (400px) para que quepa cómodamente el nuevo botón
        contenedorPrincipal.setPrefWidth(400); 
        
        javafx.scene.layout.VBox contenedorLista = new javafx.scene.layout.VBox(10);
        
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(contenedorLista);
        scroll.setPrefViewportHeight(280); 
        scroll.setFitToWidth(true);
        scroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent; -fx-border-color: transparent;");

        Button btnNuevo = new Button("➕ Agregar nuevo");
        btnNuevo.getStyleClass().add("boton-acento");
        btnNuevo.setPrefHeight(35);
        btnNuevo.setMaxWidth(Double.MAX_VALUE);

        Runnable refrescarLista = new Runnable() {
            @Override
            public void run() {
                contenedorLista.getChildren().clear();
                ArrayList<EstadoAnimo> estados = GestorBaseDatos.obtenerEstadosAnimo();

                for (EstadoAnimo est : estados) {
                    TextField txtNom = new TextField(est.getNombre());
                    txtNom.getStyleClass().add("caja-texto");
                    txtNom.setPrefHeight(35); 
                    txtNom.setStyle("-fx-font-size: 14px;"); 
                    javafx.scene.layout.HBox.setHgrow(txtNom, javafx.scene.layout.Priority.ALWAYS);

                    javafx.scene.control.ColorPicker cp = new javafx.scene.control.ColorPicker(javafx.scene.paint.Color.web(est.getColorHex()));
                    cp.setStyle("-fx-background-color: -bg-caja; -fx-color-label-visible: false;");
                    cp.setPrefHeight(35);

                    // 🚨 FIX: Adiós emoji gris. Hola botón de Guardar real.
                    Button btnGuardar = new Button("Guardar");
                    btnGuardar.getStyleClass().add("boton-secundario");
                    btnGuardar.setStyle("-fx-font-size: 13px; -fx-cursor: hand;");
                    
                    btnGuardar.setOnAction(e -> {
                        String hex = String.format("#%02X%02X%02X", (int)(cp.getValue().getRed()*255), (int)(cp.getValue().getGreen()*255), (int)(cp.getValue().getBlue()*255));
                        GestorBaseDatos.actualizarEstadoAnimo(est.getId(), txtNom.getText(), hex);
                        
                        // 🚨 Magia UX: Feedback visual de que se guardó correctamente
                        btnGuardar.setText("¡Listo!");
                        btnGuardar.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand; -fx-font-weight: bold;");
                        
                        // Volvemos a la normalidad después de 1.5 segundos
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
                        pause.setOnFinished(event -> {
                            btnGuardar.setText("Guardar");
                            btnGuardar.setStyle("-fx-font-size: 13px; -fx-cursor: hand;");
                        });
                        pause.play();
                        
                        renderizarLeyendaAnimos();
                        renderizarMatrizHabitos();
                    });

                    // 🚨 UX Pro: Auto-Guardar al cambiar el color o presionar Enter
                    cp.setOnAction(e -> btnGuardar.fire());
                    txtNom.setOnKeyPressed(event -> {
                        if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                            btnGuardar.fire();
                        }
                    });

                    Button btnEliminar = new Button("🗑");
                    btnEliminar.setStyle("-fx-background-color: transparent; -fx-text-fill: #F44336; -fx-cursor: hand; -fx-font-size: 16px;");
                    btnEliminar.setOnAction(e -> {
                        GestorBaseDatos.eliminarEstadoAnimo(est.getId());
                        this.run(); 
                        renderizarLeyendaAnimos();
                        renderizarMatrizHabitos();
                    });

                    javafx.scene.layout.HBox fila = new javafx.scene.layout.HBox(8, cp, txtNom, btnGuardar, btnEliminar);
                    fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    contenedorLista.getChildren().add(fila);
                }

                btnNuevo.setVisible(estados.size() < 10);
                btnNuevo.setManaged(estados.size() < 10);

                javafx.application.Platform.runLater(() -> dialogo.getDialogPane().getScene().getWindow().sizeToScene());
            }
        };
        
        btnNuevo.setOnAction(e -> {
            GestorBaseDatos.agregarEstadoAnimo("Nuevo", "#FFFFFF");
            refrescarLista.run();
            renderizarLeyendaAnimos();
        });
        
        refrescarLista.run();
        
        contenedorPrincipal.getChildren().addAll(scroll, btnNuevo);
        dialogo.getDialogPane().setContent(contenedorPrincipal);
        
        javafx.application.Platform.runLater(() -> dialogo.getDialogPane().getScene().getWindow().sizeToScene());
        dialogo.showAndWait();
    }

    private void mostrarSelectorAnimo(javafx.scene.Node ancla, java.time.LocalDate fecha) {
        // 🚨 FIX 2: Abandonamos el 'ContextMenu' que se ponía Fucsia y creamos un 'Popup' puro y limpio
        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.setAutoHide(true); // Se cierra solo si haces clic en otra parte

        javafx.scene.layout.VBox lista = new javafx.scene.layout.VBox(5);
        lista.setStyle("-fx-background-color: -bg-caja; -fx-background-radius: 8; -fx-border-color: -borde-color; -fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5);");

        for (EstadoAnimo estado : GestorBaseDatos.obtenerEstadosAnimo()) {
            javafx.scene.shape.Circle circ = new javafx.scene.shape.Circle(7, javafx.scene.paint.Color.web(estado.getColorHex()));
            Label lbl = new Label(estado.getNombre());
            lbl.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 14px; -fx-font-weight: bold;");

            javafx.scene.layout.HBox fila = new javafx.scene.layout.HBox(12, circ, lbl);
            fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            fila.setStyle("-fx-padding: 8 15 8 15; -fx-background-radius: 5; -fx-cursor: hand; -fx-background-color: transparent;");

            // Efecto hover preciso y elegante
            fila.setOnMouseEntered(e -> fila.setStyle("-fx-padding: 8 15 8 15; -fx-background-radius: 5; -fx-cursor: hand; -fx-background-color: -bg-barra;"));
            fila.setOnMouseExited(e -> fila.setStyle("-fx-padding: 8 15 8 15; -fx-background-radius: 5; -fx-cursor: hand; -fx-background-color: transparent;"));

            fila.setOnMouseClicked(e -> {
                GestorBaseDatos.registrarAnimo(fecha, estado.getId());
                popup.hide();
                renderizarMatrizHabitos();
            });

            lista.getChildren().add(fila);
        }

        // Línea divisoria elegante
        javafx.scene.shape.Line linea = new javafx.scene.shape.Line(0, 0, 150, 0);
        linea.setStroke(javafx.scene.paint.Color.web("#333333"));
        javafx.scene.layout.VBox.setMargin(linea, new javafx.geometry.Insets(5, 0, 5, 0));
        lista.getChildren().add(linea);

        Button btnBorrar = new Button("✖ Borrar día");
        btnBorrar.setStyle("-fx-background-color: transparent; -fx-text-fill: #F44336; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;");
        btnBorrar.setMaxWidth(Double.MAX_VALUE);
        
        btnBorrar.setOnMouseEntered(e -> btnBorrar.setStyle("-fx-background-color: rgba(244, 67, 54, 0.1); -fx-text-fill: #F44336; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5;"));
        btnBorrar.setOnMouseExited(e -> btnBorrar.setStyle("-fx-background-color: transparent; -fx-text-fill: #F44336; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;"));

        btnBorrar.setOnAction(e -> {
            GestorBaseDatos.eliminarAnimo(fecha);
            popup.hide();
            renderizarMatrizHabitos();
        });
        
        lista.getChildren().add(btnBorrar);
        popup.getContent().add(lista);

        // Alineación matemática bajo el círculo
        javafx.geometry.Point2D punto = ancla.localToScreen(0, 0);
        if (punto != null) {
            popup.show(ancla.getScene().getWindow(), punto.getX() - 10, punto.getY() + 25);
        }
    }

    // --- ACCIONES CRUD DE HÁBITOS ---
    private void accionEliminarHabito(Habito h) {
        javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alerta.setTitle("Eliminar Hábito");
        alerta.setHeaderText("¿Estás seguro de eliminar '" + h.getNombre() + "'?");
        alerta.setContentText("⚠ ADVERTENCIA: Se borrará TODO el historial de meses pasados y no podrás recuperarlo.");
        alerta.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        aplicarTemaOscuro(alerta);

        Optional<ButtonType> respuesta = alerta.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.YES) {
            GestorBaseDatos.eliminarHabito(h.getId());
            renderizarMatrizHabitos();
        }
    }

    private void accionEditarHabito(Habito h) {
        Dialog<Object[]> dialogo = new Dialog<>();
        dialogo.setTitle("Editar Hábito");
        dialogo.setHeaderText("Modificando: " + h.getNombre());
        aplicarTemaOscuro(dialogo);

        ButtonType btnGuardar = new ButtonType("Guardar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        TextField txtNombre = new TextField(h.getNombre());
        txtNombre.getStyleClass().add("caja-texto");
        txtNombre.setPrefHeight(35);

        ComboBox<String> comboCat = new ComboBox<>();
        comboCat.getStyleClass().add("combo-capsula");
        comboCat.setPrefWidth(200);
        // 🚨 CAMBIO 1: Añadimos ("HABITOS")
        for (Categoria cat : GestorBaseDatos.obtenerCategorias("HABITOS")) comboCat.getItems().add(cat.getNombre());
        
        // Precargar categoría y color
        String catOriginal = "📌 Sin categoría";
        // 🚨 CAMBIO 2: Añadimos ("HABITOS")
        for (Categoria cat : GestorBaseDatos.obtenerCategorias("HABITOS")) {
            if (cat.getId() == h.getIdCategoria()) catOriginal = cat.getNombre();
        }
        comboCat.setValue(catOriginal);

        javafx.scene.control.ColorPicker colorPicker = new javafx.scene.control.ColorPicker(javafx.scene.paint.Color.web(h.getColorHex()));
        colorPicker.setStyle("-fx-background-color: -bg-caja; -fx-color-label-visible: false;");
        colorPicker.setPrefHeight(35);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(15);
        grid.add(new Label("Hábito:"), 0, 0); grid.add(txtNombre, 1, 0);
        grid.add(new Label("Lista:"), 0, 1); grid.add(comboCat, 1, 1);
        grid.add(new Label("Color:"), 0, 2); grid.add(colorPicker, 1, 2);
        
        // Estilizar los labels del grid
        for (javafx.scene.Node n : grid.getChildren()) {
            if (n instanceof Label) n.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 14px;");
        }

        dialogo.getDialogPane().setContent(grid);
        dialogo.setResultConverter(boton -> {
            if (boton == btnGuardar) {
                String hex = String.format("#%02X%02X%02X", 
                    (int)(colorPicker.getValue().getRed() * 255),
                    (int)(colorPicker.getValue().getGreen() * 255),
                    (int)(colorPicker.getValue().getBlue() * 255));
                return new Object[]{txtNombre.getText(), comboCat.getValue(), hex};
            }
            return null;
        });

        dialogo.showAndWait().ifPresent(datos -> {
            String nombre = datos[0].toString().trim();
            if (!nombre.isEmpty()) {
                // 🚨 CAMBIO 3: Añadimos ("HABITOS") dentro del obtenerIdCategoria
                GestorBaseDatos.actualizarHabito(h.getId(), nombre, GestorBaseDatos.obtenerIdCategoria(datos[1].toString(), "HABITOS"), datos[2].toString());
                renderizarMatrizHabitos();
            }
        });
    }

    @FXML
    public void crearNuevoHabito() {
        Dialog<Object[]> dialogo = new Dialog<>();
        dialogo.setTitle("Nuevo Hábito");
        dialogo.setHeaderText("🌱 Crea un nuevo hábito para trackear:");
        aplicarTemaOscuro(dialogo);

        ButtonType btnGuardar = new ButtonType("Guardar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);

        // 1. Nombre del Hábito
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Ej: Leer 10 páginas, Beber agua...");
        txtNombre.getStyleClass().add("caja-texto");
        txtNombre.setPrefHeight(35);

        // 2. Elegir la Lista/Categoría
        ComboBox<String> comboCat = new ComboBox<>();
        comboCat.getStyleClass().add("combo-capsula");
        comboCat.setPrefWidth(200);
        // 🚨 CAMBIO AQUÍ: Le añadimos ("HABITOS")
        for (Categoria cat : GestorBaseDatos.obtenerCategorias("HABITOS")) comboCat.getItems().add(cat.getNombre());
        comboCat.setValue(categoriaHabitos.equals("Todas") ? "📌 Sin categoría" : categoriaHabitos);

        // 3. Selector de Color (ColorPicker de JavaFX)
        javafx.scene.control.ColorPicker colorPicker = new javafx.scene.control.ColorPicker(javafx.scene.paint.Color.web("#C2185B"));
        colorPicker.setStyle("-fx-background-color: -bg-caja; -fx-color-label-visible: false;");
        colorPicker.setPrefHeight(35);

        // Armamos el diseño de la ventanita
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(15);
        
        Label lblNom = new Label("Hábito:"); lblNom.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 14px;");
        Label lblCat = new Label("Lista:"); lblCat.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 14px;");
        Label lblCol = new Label("Color:"); lblCol.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 14px;");

        grid.add(lblNom, 0, 0); grid.add(txtNombre, 1, 0);
        grid.add(lblCat, 0, 1); grid.add(comboCat, 1, 1);
        grid.add(lblCol, 0, 2); grid.add(colorPicker, 1, 2);

        dialogo.getDialogPane().setContent(grid);

        // Capturamos los datos al presionar Guardar
        dialogo.setResultConverter(boton -> {
            if (boton == btnGuardar) {
                // Transformamos el Color de JavaFX a formato Hexadecimal (#RRGGBB) para guardarlo en SQLite
                String hex = String.format("#%02X%02X%02X", 
                    (int)(colorPicker.getValue().getRed() * 255),
                    (int)(colorPicker.getValue().getGreen() * 255),
                    (int)(colorPicker.getValue().getBlue() * 255));
                return new Object[]{txtNombre.getText(), comboCat.getValue(), hex};
            }
            return null;
        });

        Optional<Object[]> resultado = dialogo.showAndWait();
        resultado.ifPresent(datos -> {
            String nombre = datos[0].toString().trim();
            String catNombre = datos[1].toString();
            String colorHex = datos[2].toString();

            if (!nombre.isEmpty()) {
                // 🚨 CAMBIO AQUÍ: Le añadimos ("HABITOS") al final
                int idCat = GestorBaseDatos.obtenerIdCategoria(catNombre, "HABITOS");
                GestorBaseDatos.insertarHabito(nombre, idCat, colorHex);
                
                renderizarMatrizHabitos(); 
            }
        });
    }

   // =======================================================
    // 📊 PANEL DE ESTADÍSTICAS INTELIGENTE (TAREAS / HÁBITOS)
    // =======================================================
    @FXML
    public void abrirEstadisticas() {
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.setTitle("Análisis de Productividad");
        
        dialogo.setHeaderText(panelTareas.isVisible() ? "📊 Tu rendimiento general en Tareas" : "🌱 Tu constancia global en Hábitos");
        aplicarTemaOscuro(dialogo);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        javafx.scene.layout.HBox contenedorGraficos = new javafx.scene.layout.HBox(20);
        contenedorGraficos.setAlignment(javafx.geometry.Pos.CENTER);
        contenedorGraficos.setStyle("-fx-background-color: transparent;");

        // ==========================================
        // MODO 1: GRÁFICOS DE TAREAS
        // ==========================================
        if (panelTareas.isVisible()) {
            int completadas = logica.contarCompletadas();
            int pendientes = logica.contarPendientes();
            int atrasadas = logica.contarAtrasadas();

            javafx.collections.ObservableList<PieChart.Data> datosTorta = javafx.collections.FXCollections.observableArrayList();
            if (completadas > 0) datosTorta.add(new PieChart.Data("Completadas (" + completadas + ")", completadas));
            if (pendientes > 0) datosTorta.add(new PieChart.Data("Pendientes (" + pendientes + ")", pendientes));
            if (atrasadas > 0) datosTorta.add(new PieChart.Data("Atrasadas (" + atrasadas + ")", atrasadas));

            PieChart graficoTorta = new PieChart(datosTorta);
            graficoTorta.setTitle("Distribución de Tareas");
            graficoTorta.setLabelsVisible(true);
            graficoTorta.setLegendVisible(false);
            graficoTorta.setPrefSize(400, 350); 
            graficoTorta.setLabelLineLength(15); 
            graficoTorta.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

            CategoryAxis ejeX = new CategoryAxis();
            ejeX.setTickLabelRotation(45); 
            ejeX.setTickLabelFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 13));

            NumberAxis ejeY = new NumberAxis();
            ejeY.setTickUnit(1);
            ejeY.setMinorTickVisible(false); 
            ejeY.setTickLabelFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 13));
            
            BarChart<String, Number> graficoBarras = new BarChart<>(ejeX, ejeY);
            graficoBarras.setTitle("Carga de Trabajo por Lista");
            graficoBarras.setLegendVisible(false);
            graficoBarras.setStyle("-fx-font-size: 15px;");

            ArrayList<Categoria> categoriasBD = GestorBaseDatos.obtenerCategorias("TAREAS");
            XYChart.Series<String, Number> serieBarras = new XYChart.Series<>();
            for (Categoria cat : categoriasBD) {
                int cantidad = GestorBaseDatos.contarTareasEnCategoria(cat.getId());
                if (cantidad > 0) {
                    String nombreLimpio = cat.getNombre();
                    if (nombreLimpio.contains(" ")) {
                        nombreLimpio = nombreLimpio.substring(nombreLimpio.indexOf(" ") + 1);
                    }
                    // 🚨 FIX 1: Truncar textos demasiado largos (Máximo 10 letras + puntos)
                    if (nombreLimpio.length() > 13) {
                        nombreLimpio = nombreLimpio.substring(0, 10) + "...";
                    }
                    serieBarras.getData().add(new XYChart.Data<>(nombreLimpio, cantidad));
                }
            }
            graficoBarras.getData().add(serieBarras);

            // 🚨 FIX 2: Aumentamos el espacio por columna a 80px para que el texto respire
            int anchoDinamico = Math.max(550, categoriasBD.size() * 80);
            graficoBarras.setMinWidth(anchoDinamico);
            graficoBarras.setPrefWidth(anchoDinamico);
            
            javafx.scene.control.ScrollPane scrollBarras = new javafx.scene.control.ScrollPane(graficoBarras);
            scrollBarras.setPrefSize(550, 370);
            scrollBarras.setFitToHeight(true);
            scrollBarras.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER); 
            // 🚨 FIX 3: Quitamos el setStyle que borraba las líneas guía
            
            contenedorGraficos.getChildren().addAll(graficoTorta, scrollBarras);

       // ==========================================
        // MODO 2: GRÁFICOS DE HÁBITOS
        // ==========================================
        } else {
            ArrayList<Habito> listaHabitos = GestorBaseDatos.obtenerHabitos();
            
            if (listaHabitos.isEmpty()) {
                Label lblVacio = new Label("Aún no tienes hábitos registrados para analizar.");
                lblVacio.setStyle("-fx-text-fill: -texto-secundario; -fx-font-size: 16px;");
                contenedorGraficos.getChildren().add(lblVacio);
            } else {
                
                // --- CONTENEDOR MAESTRO (Para agrupar todo verticalmente) ---
                javafx.scene.layout.VBox layoutHabitos = new javafx.scene.layout.VBox(25);
                javafx.scene.layout.HBox graficosSuperiores = new javafx.scene.layout.HBox(20);
                graficosSuperiores.setAlignment(javafx.geometry.Pos.CENTER);

                // --- A) Gráfico Circular ---
                // --- A) Gráfico Circular ---
                javafx.collections.ObservableList<PieChart.Data> datosTorta = javafx.collections.FXCollections.observableArrayList();
                // 🚨 CAMBIO AQUÍ: Añadimos ("HABITOS")
                for (Categoria cat : GestorBaseDatos.obtenerCategorias("HABITOS")) {
                    long cantidad = listaHabitos.stream().filter(h -> h.getIdCategoria() == cat.getId()).count();
                    if (cantidad > 0) {
                        String nombreLimpio = cat.getNombre();
                        if (nombreLimpio.contains(" ")) nombreLimpio = nombreLimpio.substring(nombreLimpio.indexOf(" ") + 1);
                        datosTorta.add(new PieChart.Data(nombreLimpio + " (" + cantidad + ")", cantidad));
                    }
                }
                
                PieChart graficoTorta = new PieChart(datosTorta);
                graficoTorta.setTitle("Hábitos por Lista");
                graficoTorta.setLabelsVisible(true);
                graficoTorta.setLegendVisible(false);
                graficoTorta.setPrefSize(400, 350); 
                graficoTorta.setLabelLineLength(15); 
                graficoTorta.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

                // --- B) Gráfico de Barras ---
                CategoryAxis ejeX = new CategoryAxis();
                ejeX.setTickLabelRotation(45);
                ejeX.setTickLabelFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 13));

                NumberAxis ejeY = new NumberAxis();
                ejeY.setTickUnit(5);
                ejeY.setMinorTickVisible(false);
                ejeY.setAutoRanging(false); 
                ejeY.setLowerBound(0);
                ejeY.setUpperBound(mesNavegacion.lengthOfMonth()); 
                ejeY.setTickLabelFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 13));
                ejeY.setLabel("Días Completados");

                BarChart<String, Number> graficoBarras = new BarChart<>(ejeX, ejeY);
                
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MMMM", java.util.Locale.forLanguageTag("es-ES"));
                String mesStr = mesNavegacion.format(fmt);
                mesStr = mesStr.substring(0, 1).toUpperCase() + mesStr.substring(1);
                graficoBarras.setTitle("Ranking de Constancia (" + mesStr + ")");
                
                graficoBarras.setLegendVisible(false);
                graficoBarras.setStyle("-fx-font-size: 15px;");

                XYChart.Series<String, Number> serieBarras = new XYChart.Series<>();
                for (Habito h : listaHabitos) {
                    int diasLogrados = GestorBaseDatos.obtenerDiasCompletadosMes(h.getId(), mesNavegacion.getYear(), mesNavegacion.getMonthValue()).size();
                    
                    String nombreLimpio = h.getNombre();
                    if (nombreLimpio.length() > 13) {
                        nombreLimpio = nombreLimpio.substring(0, 10) + "...";
                    }

                    XYChart.Data<String, Number> dato = new XYChart.Data<>(nombreLimpio, diasLogrados);
                    
                    dato.nodeProperty().addListener((obs, oldNode, newNode) -> {
                        if (newNode != null) newNode.setStyle("-fx-bar-fill: " + h.getColorHex() + ";");
                    });
                    serieBarras.getData().add(dato);
                }
                graficoBarras.getData().add(serieBarras);

                int anchoDinamico = Math.max(550, listaHabitos.size() * 80);
                graficoBarras.setMinWidth(anchoDinamico);
                graficoBarras.setPrefWidth(anchoDinamico);
                
                javafx.scene.control.ScrollPane scrollBarras = new javafx.scene.control.ScrollPane(graficoBarras);
                scrollBarras.setPrefSize(550, 370); 
                scrollBarras.setFitToHeight(true);
                scrollBarras.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER); 

                graficosSuperiores.getChildren().addAll(graficoTorta, scrollBarras);

                // --- C) 🧠 NUEVO: GRÁFICO DE IMPACTO EMOCIONAL ---
                javafx.scene.layout.VBox panelImpacto = new javafx.scene.layout.VBox(10);
                // 🚨 FIX 1: Quitamos el fondo y los bordes para igualar la estética oscura de arriba
                panelImpacto.setStyle("-fx-background-color: transparent; -fx-padding: 20 0 0 0;");
                
                javafx.scene.layout.HBox cabeceraImpacto = new javafx.scene.layout.HBox(15);
                cabeceraImpacto.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                
                // 🚨 FIX 3: Título dinámico con el mes exacto en visualización
                Label lblTituloImpacto = new Label("Impacto Emocional en tu Productividad (" + mesStr + ")");
                lblTituloImpacto.setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 16px; -fx-font-weight: bold;");
                
                javafx.scene.layout.Region spacerImpacto = new javafx.scene.layout.Region();
                javafx.scene.layout.HBox.setHgrow(spacerImpacto, javafx.scene.layout.Priority.ALWAYS);
                
                ComboBox<String> comboFiltroAnimo = new ComboBox<>();
                comboFiltroAnimo.getItems().add("Todas las emociones");
                comboFiltroAnimo.setValue("Todas las emociones");
                comboFiltroAnimo.getStyleClass().add("combo-capsula");
                
                cabeceraImpacto.getChildren().addAll(lblTituloImpacto, spacerImpacto, comboFiltroAnimo);

                CategoryAxis ejeXImp = new CategoryAxis();
                ejeXImp.setTickLabelFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 14));

                NumberAxis ejeYImp = new NumberAxis();
                ejeYImp.setAutoRanging(false);
                ejeYImp.setLowerBound(0);
                ejeYImp.setUpperBound(100); 
                ejeYImp.setTickUnit(20);
                ejeYImp.setTickLabelFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 13));
                // 🚨 FIX 2: Etiqueta explícita para guiar al usuario en el Eje Y
                ejeYImp.setLabel("Porcentaje de Constancia y Éxito (%)");

                BarChart<String, Number> chartImpacto = new BarChart<>(ejeXImp, ejeYImp);
                chartImpacto.setLegendVisible(false);
                chartImpacto.setPrefHeight(280); 
                chartImpacto.setStyle("-fx-background-color: transparent;");
                
                // Traemos la analítica cruzada desde el backend
                java.util.HashMap<EstadoAnimo, Double> efectividadAnimos = GestorBaseDatos.calcularEfectividadPorAnimo(mesNavegacion.getYear(), mesNavegacion.getMonthValue());
                
                // Llenamos el filtro SÓLO con las emociones que el usuario sí experimentó este mes
                for (EstadoAnimo ea : efectividadAnimos.keySet()) {
                    comboFiltroAnimo.getItems().add(ea.getNombre());
                }

                // Motor de dibujado dinámico que responde al Filtro
                Runnable dibujarImpacto = () -> {
                    chartImpacto.getData().clear();
                    XYChart.Series<String, Number> serieImp = new XYChart.Series<>();
                    String filtro = comboFiltroAnimo.getValue();

                    for (java.util.Map.Entry<EstadoAnimo, Double> entry : efectividadAnimos.entrySet()) {
                        EstadoAnimo ea = entry.getKey();
                        
                        // Validamos el filtro seleccionado
                        if (filtro.equals("Todas las emociones") || filtro.equals(ea.getNombre())) {
                            XYChart.Data<String, Number> dato = new XYChart.Data<>(ea.getNombre(), entry.getValue());
                            
                            // Pintamos la barra del color exacto de esa emoción y le añadimos un Tooltip
                            dato.nodeProperty().addListener((obs, oldNode, newNode) -> {
                                if (newNode != null) {
                                    newNode.setStyle("-fx-bar-fill: " + ea.getColorHex() + ";");
                                    javafx.scene.control.Tooltip tt = new javafx.scene.control.Tooltip(String.format("%.1f%% de disciplina", entry.getValue()));
                                    tt.setStyle("-fx-font-size: 14px; -fx-background-color: -bg-barra; -fx-text-fill: -texto-principal;");
                                    
                                    // 🚨 FIX: Acelerador inyectado. El Tooltip aparecerá en 150 milisegundos
                                    tt.setShowDelay(javafx.util.Duration.millis(150));
                                    tt.setShowDuration(javafx.util.Duration.INDEFINITE);
                                    
                                    javafx.scene.control.Tooltip.install(newNode, tt);
                                }
                            });
                            serieImp.getData().add(dato);
                        }
                    }
                    chartImpacto.getData().add(serieImp);
                };

                comboFiltroAnimo.setOnAction(e -> dibujarImpacto.run());
                dibujarImpacto.run();

                panelImpacto.getChildren().addAll(cabeceraImpacto, chartImpacto);
                layoutHabitos.getChildren().addAll(graficosSuperiores, panelImpacto);
                
                // Envolvemos todo en un ScrollPane vertical para evitar que la ventana estalle del monitor
                javafx.scene.control.ScrollPane scrollGlobal = new javafx.scene.control.ScrollPane(layoutHabitos);
                scrollGlobal.setFitToWidth(true);
                scrollGlobal.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent; -fx-control-inner-background: transparent;");
                scrollGlobal.setPrefViewportHeight(650); // Límite de alto
                scrollGlobal.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);

                contenedorGraficos.getChildren().add(scrollGlobal);
            }
        }

        dialogo.getDialogPane().setContent(contenedorGraficos);
        dialogo.showAndWait();
    }
}