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

    private ToDoList logica = new ToDoList();
    private int filtroActual = 1;
    private String categoriaActual = "Todas";
    private boolean modoPrivacidadActivo = false;
    private String formatoFechaActual = "dd/MM/yyyy";

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
        
        MenuItem menuEditarFecha = new MenuItem("📅 Editar Fecha Límite");
        menuEditarFecha.setOnAction(e -> accionEditarFecha());

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
                    // 🚨 Limpiamos las clases visuales y el "Ghosting" del modo enmascarado
                    getStyleClass().removeAll("celda-padre", "celda-hija");
                    setOnMouseEntered(null);
                    setOnMouseExited(null);
                } else {
                    getStyleClass().removeAll("celda-padre", "celda-hija"); // Reseteo de seguridad
                    
                    String textoFecha = "";
                    if (tareaActual.getFechaLimite() != null) {
                        java.time.format.DateTimeFormatter formato = java.time.format.DateTimeFormatter.ofPattern(formatoFechaActual);
                        textoFecha = " 📅 [Vence: " + tareaActual.getFechaLimite().format(formato) + "]";
                    }

                    String estado = tareaActual.isCompletada() ? "[X]" : "[ ]";
                    boolean esSubtarea = tareaActual.getIdTareaPadre() != null;

                    String descReal = tareaActual.getDescripcion();
                    String descOculta = descReal.replaceAll("[^\\s]", "*"); 
                    
                    String descMostrar = modoPrivacidadActivo ? descOculta : descReal;
                    String textoBase = ""; 

                    if (esSubtarea) {
                        textoBase = "      ↳ " + estado + " ";
                        setText(textoBase + descMostrar + textoFecha);
                        // Usamos variable dinámica pero forzamos el tamaño
                        setStyle("-fx-text-fill: -texto-secundario; -fx-font-size: 14px;"); 
                    } else {
                        int numeroVisual = 1;
                        for (int i = 0; i < getIndex(); i++) {
                            Tarea tAnterior = getListView().getItems().get(i);
                            if (tAnterior.getIdTareaPadre() == null) numeroVisual++;
                        }
                        
                        textoBase = numeroVisual + ". " + estado + " ";
                        setText(textoBase + descMostrar + textoFecha);
                        // Usamos variable dinámica pero forzamos el tamaño 16px
                        setStyle("-fx-text-fill: -texto-principal; -fx-font-size: 16px; -fx-font-weight: bold;"); 
                    }

                    // --- EFECTO REVELADO AL PASAR EL RATÓN (HOVER) ---
                    if (modoPrivacidadActivo) {
                        final String baseFinal = textoBase;
                        final String fechaFinal = textoFecha;
                        
                        setOnMouseEntered(e -> setText(baseFinal + descReal + fechaFinal));
                        setOnMouseExited(e -> setText(baseFinal + descOculta + fechaFinal));
                    } else {
                        setOnMouseEntered(null);
                        setOnMouseExited(null);
                    }

                    javafx.scene.shape.Circle indicador = new javafx.scene.shape.Circle(esSubtarea ? 4 : 6); 
                    java.time.LocalDate hoy = java.time.LocalDate.now();
                    
                    if (tareaActual.isCompletada()) {
                        indicador.setFill(javafx.scene.paint.Color.web("#4CAF50")); 
                    } else if (tareaActual.getFechaLimite() != null) { 
                        if (tareaActual.getFechaLimite().isBefore(hoy)) indicador.setFill(javafx.scene.paint.Color.web("#F44336")); 
                        else if (tareaActual.getFechaLimite().isEqual(hoy)) indicador.setFill(javafx.scene.paint.Color.web("#FFEB3B"));
                        else indicador.setFill(javafx.scene.paint.Color.web("#2196F3"));
                    } else {
                        indicador.setFill(javafx.scene.paint.Color.web("#9E9E9E")); 
                    }

                    if (!esSubtarea && tareaActual.getSubTareas() != null && !tareaActual.getSubTareas().isEmpty()) {
                        javafx.scene.control.Button btnToggle = new javafx.scene.control.Button(tareaActual.isExpandida() ? "[-] " : "[+] ");
                        btnToggle.getStyleClass().add("boton-desplegable-celda");
                        
                        btnToggle.setOnAction(e -> {
                            tareaActual.setExpandida(!tareaActual.isExpandida());                            
                            GestorBaseDatos.actualizarTarea(tareaActual);
                            actualizarInterfaz();
                        });

                        javafx.scene.layout.HBox cajaCelda = new javafx.scene.layout.HBox(8, indicador, btnToggle);
                        cajaCelda.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        setGraphic(cajaCelda);
                    } else {
                        setGraphic(indicador); 
                    }
                }
            }
        });

        actualizarInterfaz();
        cargarMenuLateral();
    }

   private void actualizarInterfaz() {
        listaTareas.getItems().clear();
        
        ArrayList<Tarea> listaPrincipales = logica.obtenerTareasFiltradas(filtroActual, categoriaActual);
        
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

        // Arreglo para el botón "Ver Todas"
        javafx.scene.control.Button btnVerTodas = new javafx.scene.control.Button("🔎 Ver Todas");
        btnVerTodas.getStyleClass().add("boton-transparente");
        btnVerTodas.setStyle("-fx-font-size: 16px; -fx-padding: 8 0 8 10; -fx-alignment: center-left;");
        btnVerTodas.setMaxWidth(Double.MAX_VALUE); // 🚨 FIX ERROR 1: Hace que todo el ancho sea clicable
        btnVerTodas.setOnAction(e -> { categoriaActual = "Todas"; actualizarInterfaz(); });
        contenedorCategorias.getChildren().add(btnVerTodas);

        ArrayList<Categoria> categoriasBD = GestorBaseDatos.obtenerCategorias();

        for (Categoria cat : categoriasBD) {
            javafx.scene.control.Button btn = new javafx.scene.control.Button(cat.getNombre());
            btn.getStyleClass().add("boton-transparente");
            btn.setStyle("-fx-font-size: 16px; -fx-padding: 8 0 8 10; -fx-alignment: center-left;");
            btn.setMaxWidth(Double.MAX_VALUE); 
            btn.setOnAction(e -> { categoriaActual = cat.getNombre(); actualizarInterfaz(); });

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
    }

    @FXML
    public void agregarNuevaTarea() {
        String texto = txtNuevaTarea.getText();
        java.time.LocalDate fecha = calendarioPrincipal.getValue();

        if (texto != null && !texto.trim().isEmpty()) {
            java.util.List<String> opciones = new java.util.ArrayList<>();
            for (Categoria cat : GestorBaseDatos.obtenerCategorias()) {
                opciones.add(cat.getNombre());
            }

            String sugerencia = categoriaActual.equals("Todas") ? "📌 Sin categoría" : categoriaActual;

            javafx.scene.control.ChoiceDialog<String> dialogo = new javafx.scene.control.ChoiceDialog<>(sugerencia, opciones);
            dialogo.setTitle("Asignar Categoría");
            dialogo.setHeaderText("Has creado: '" + texto + "'");
            dialogo.setContentText("¿En qué lista deseas guardarla?");

            aplicarTemaOscuro(dialogo);

            java.util.Optional<String> resultado = dialogo.showAndWait();

            if (resultado.isPresent()) {
                String catDestino = resultado.get();
                logica.agregarTarea(texto, fecha, catDestino);
                actualizarInterfaz();
                
                txtNuevaTarea.clear();
                calendarioPrincipal.setValue(null);
            }
        }
    }

    // --- ACCIONES DEL CLICK DERECHO REESCRITAS PARA JERARQUÍA (V5.0.0e) ---

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
                    logica.agregarSubTarea(seleccionada, desc.trim()); // Pasamos el objeto directo
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
            
            // 1. Evaluamos el contexto de la tarea para personalizar el mensaje
            String titulo = "Eliminar Tarea";
            String encabezado = "¿Estás seguro de eliminar esta tarea?";
            String contenido = "Tarea: " + seleccionada.getDescripcion();

            boolean tieneSubtareas = seleccionada.getIdTareaPadre() == null && seleccionada.getSubTareas() != null && !seleccionada.getSubTareas().isEmpty();

            if (tieneSubtareas) {
                titulo = "Eliminar Tarea Principal";
                encabezado = "¡Atención! Esta tarea contiene sub-tareas.";
                contenido = "Si eliminas esta tarea, TODAS sus sub-tareas (" + seleccionada.getSubTareas().size() + ") también serán destruidas irremediablemente.\n\n¿Deseas continuar?";
            } else if (seleccionada.getIdTareaPadre() != null) {
                titulo = "Eliminar Sub-tarea";
            }

            // 2. Construimos la barrera visual (Alerta)
            javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alerta.setTitle(titulo);
            alerta.setHeaderText(encabezado);
            alerta.setContentText(contenido);
            alerta.getButtonTypes().setAll(javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);

            // 3. Inyectamos nuestro diseño y el tema dinámico
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

            // 4. Pausamos la app y esperamos la decisión del usuario
            java.util.Optional<javafx.scene.control.ButtonType> respuesta = alerta.showAndWait();
            
            // 5. Si el usuario está seguro, ejecutamos la guillotina
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

    private void accionEditarFecha() {
        Tarea seleccionada = listaTareas.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            Dialog<java.time.LocalDate> dialogo = new Dialog<>();
            dialogo.setTitle("Editar Fecha Límite");
            dialogo.setHeaderText("Modificar fecha para:\n" + seleccionada.getDescripcion());

            aplicarTemaOscuro(dialogo);
            // Solo dejamos el botón Cancelar nativo por si se arrepiente
            dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL); 

            // =======================================================
            // CONSTRUYENDO LA INTERFAZ CUSTOM DEL DIÁLOGO
            // =======================================================
            javafx.scene.layout.VBox contenedor = new javafx.scene.layout.VBox(15);
            contenedor.setAlignment(javafx.geometry.Pos.CENTER);
            
            // 1. El calendario clásico como protagonista
            DatePicker dpManual = new DatePicker(seleccionada.getFechaLimite());
            dpManual.getStyleClass().add("date-picker-filtro"); // Reciclamos tu diseño oscuro
            dpManual.setPromptText("Seleccionar nueva fecha...");

            // 2. Botón dedicado para limpiar la fecha
            Button btnLimpiar = new Button("🗑 Eliminar Fecha");
            btnLimpiar.setStyle("-fx-background-color: transparent; -fx-text-fill: #F44336; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px;");

            // Empaquetamos todo
            contenedor.getChildren().addAll(dpManual, btnLimpiar);
            dialogo.getDialogPane().setContent(contenedor);

            // =======================================================
            // LÓGICA DE RESPUESTA AUTOMÁTICA
            // =======================================================
            
            // Si elige manualmente en el calendario, se cierra solo y envía la fecha
            dpManual.setOnAction(e -> {
                if(dpManual.getValue() != null) {
                    dialogo.setResult(dpManual.getValue());
                    dialogo.close();
                }
            });

            // Usamos la fecha MÍNIMA de Java como una "Bandera" secreta para borrar
            btnLimpiar.setOnAction(e -> { 
                dialogo.setResult(java.time.LocalDate.MIN); 
                dialogo.close(); 
            });

            // =======================================================
            // PROCESAMIENTO FINAL AL CERRAR LA VENTANA
            // =======================================================
            Optional<java.time.LocalDate> resultado = dialogo.showAndWait();
            resultado.ifPresent(nuevaFecha -> {
                
                // Si la bandera es "MIN", significa que el usuario presionó el botón rojo
                java.time.LocalDate fechaFinal = nuevaFecha.equals(java.time.LocalDate.MIN) ? null : nuevaFecha;
                
                if (seleccionada.getIdTareaPadre() != null) {
                    seleccionada.setFechaLimite(fechaFinal);
                    GestorBaseDatos.actualizarTarea(seleccionada); // Directo a SQLite
                } else {
                    logica.editarFechaLimite(seleccionada, fechaFinal);
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
            for (Categoria cat : GestorBaseDatos.obtenerCategorias()) opciones.add(cat.getNombre());

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
                GestorBaseDatos.insertarCategoria(texto, "#FFFFFF");
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
                if (categoriaActual.equals(cat.getNombre())) categoriaActual = texto;
                cargarMenuLateral();
                actualizarInterfaz(); 
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
            if (categoriaActual.equals(cat.getNombre())) categoriaActual = "Todas";
            cargarMenuLateral();
            actualizarInterfaz();
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
            dialogo.initStyle(javafx.stage.StageStyle.UNDECORATED);
            dialogo.getDialogPane().getStylesheets().add(getClass().getResource("/com/mitodolist/estilos.css").toExternalForm());
            dialogo.setGraphic(null); 
            dialogo.getDialogPane().getStyleClass().add("mi-dialogo");

            // 🚨 INYECTAMOS EL TEMA GLOBAL A LAS ALERTAS
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
}