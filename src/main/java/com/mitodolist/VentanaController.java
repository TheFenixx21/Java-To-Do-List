package com.mitodolist;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
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
    @FXML private Button btnAgregar;
    @FXML private DatePicker calendarioPrincipal;
    @FXML private Label lblPendientes;
    @FXML private Label lblCompletadas;
    @FXML private Label lblAtrasadas;
    @FXML private TextArea txtNotasRapidas;
    @FXML private javafx.scene.layout.VBox contenedorCategorias;
    @FXML private TextField txtBuscador;
    @FXML private DatePicker calendarioFiltro;
    @FXML private Button btnLimpiarFiltros;

    private ToDoList logica = new ToDoList();
    private int filtroActual = 1;
    private String categoriaActual = "Todas";

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
                    setStyle(""); // Limpiamos la celda si está vacía
                } else {
                    // 1. Procesamos la fecha (ahora aplica para padres e hijas)
                    String textoFecha = "";
                    if (tareaActual.getFechaLimite() != null) {
                        java.time.format.DateTimeFormatter formato = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        textoFecha = " 📅 [Vence: " + tareaActual.getFechaLimite().format(formato) + "]";
                    }

                    String estado = tareaActual.isCompletada() ? "[X]" : "[ ]";
                    boolean esSubtarea = tareaActual.getIdTareaPadre() != null;

                    // 2. Aplicamos textos y estilos base
                if (esSubtarea) {
                    setText("      ↳ " + estado + " " + tareaActual.getDescripcion() + textoFecha);
                    setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 14px;"); // Hija: Gris y más pequeña
                } else {
                    // --- CORRECCIÓN V5.0.0e: ENUMERACIÓN VISUAL DINÁMICA ---
                    int numeroVisual = 1;
                    // Contamos cuántas tareas principales hay ANTES de esta en la pantalla actual
                    for (int i = 0; i < getIndex(); i++) {
                        Tarea tAnterior = getListView().getItems().get(i);
                        if (tAnterior.getIdTareaPadre() == null) {
                            numeroVisual++;
                        }
                    }
                    
                    setText(numeroVisual + ". " + estado + " " + tareaActual.getDescripcion() + textoFecha);
                    setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;"); // Padre: Blanca y fuerte
                }

                    // 3. El Indicador Visual (Círculo de estado unificado)
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

                    // 4. CREACIÓN DEL NODO INTERACTIVO (BOTÓN DE PLEGADO)
                    if (!esSubtarea && tareaActual.getSubTareas() != null && !tareaActual.getSubTareas().isEmpty()) {
                        
                        // Fabricamos un pequeño botón transparente
                        javafx.scene.control.Button btnToggle = new javafx.scene.control.Button(tareaActual.isExpandida() ? "[-] " : "[+] ");
                        btnToggle.setStyle("-fx-background-color: transparent; -fx-text-fill: #C2185B; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 5 0 0;");
                        
                        // Le enseñamos qué hacer al darle clic
                        btnToggle.setOnAction(e -> {
                            tareaActual.setExpandida(!tareaActual.isExpandida());                            
                            // NUEVO: Usamos el bisturí para guardar la preferencia en el disco de inmediato
                            GestorBaseDatos.actualizarTarea(tareaActual);
                            
                            actualizarInterfaz(); // Recarga la lista para mostrar/ocultar las hijas
                        });

                        // Empaquetamos el botón interactivo y el círculo en una caja horizontal (HBox)
                        javafx.scene.layout.HBox cajaCelda = new javafx.scene.layout.HBox(btnToggle, indicador);
                        cajaCelda.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        
                        // Mostramos la caja en la interfaz
                        setGraphic(cajaCelda);
                    } else {
                        // Si es subtarea o no tiene hijas, solo mostramos el círculo normal
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
        
        // 1. Obtenemos las tareas pre-filtradas (Pendientes/Completadas y Categoría)
        ArrayList<Tarea> listaPrincipales = logica.obtenerTareasFiltradas(filtroActual, categoriaActual);
        
        // 2. Capturamos lo que el usuario escribió (Si los campos ya cargaron)
        String textoBusqueda = (txtBuscador != null && txtBuscador.getText() != null) ? txtBuscador.getText().toLowerCase().trim() : "";
        java.time.LocalDate fechaFiltro = (calendarioFiltro != null) ? calendarioFiltro.getValue() : null;
        
        for (Tarea principal : listaPrincipales) {
            
            // --- FILTRADO DINÁMICO ---
            boolean coincideTexto = textoBusqueda.isEmpty() || principal.getDescripcion().toLowerCase().contains(textoBusqueda);
            boolean coincideFecha = fechaFiltro == null || (principal.getFechaLimite() != null && principal.getFechaLimite().isEqual(fechaFiltro));
            
            // Si la tarea NO cumple, la saltamos
            if (!coincideTexto || !coincideFecha) {
                continue; 
            }
            
            listaTareas.getItems().add(principal); 
            
            // LÓGICA V5.0.0e: Si la madre está expandida y tiene hijas
            if (principal.isExpandida() && principal.getSubTareas() != null && !principal.getSubTareas().isEmpty()) {
                // Ordenamos a las hijas y las mostramos
                principal.getSubTareas().sort(ToDoList.ORDENADOR_TAREAS);
                listaTareas.getItems().addAll(principal.getSubTareas());
            }
        }
        
        lblPendientes.setText("Pendientes: " + logica.contarPendientes());
        lblCompletadas.setText("Completadas: " + logica.contarCompletadas());
        lblAtrasadas.setText("Atrasadas: " + logica.contarAtrasadas());
    }

    private void cargarMenuLateral() {
        contenedorCategorias.getChildren().clear();

        javafx.scene.control.Button btnVerTodas = new javafx.scene.control.Button("🔎 Ver Todas");
        btnVerTodas.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: normal; -fx-cursor: hand; -fx-padding: 8 0 8 10;");
        btnVerTodas.setOnAction(e -> { categoriaActual = "Todas"; actualizarInterfaz(); });
        contenedorCategorias.getChildren().add(btnVerTodas);

        ArrayList<Categoria> categoriasBD = GestorBaseDatos.obtenerCategorias();

        for (Categoria cat : categoriasBD) {
            javafx.scene.control.Button btn = new javafx.scene.control.Button(cat.getNombre());
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: normal; -fx-cursor: hand; -fx-padding: 8 0 8 10;");
            
            btn.setOnAction(e -> { categoriaActual = cat.getNombre(); actualizarInterfaz(); });

            if (cat.getId() != GestorBaseDatos.obtenerIdCategoria("📌 Sin categoría")) { // Protegemos su lista default personal
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
            if (seleccionada.getIdTareaPadre() != null) {
                Tarea padre = encontrarPadre(seleccionada);
                if (padre != null) {
                    logica.eliminarSubTarea(padre, seleccionada);
                }
            } else {
                logica.eliminarTarea(seleccionada); // Pasamos el objeto directo, adiós a las decapitaciones aleatorias
            }
            actualizarInterfaz();
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
            // 1. ¡Destruimos la barra blanca de Windows!
            dialogo.initStyle(javafx.stage.StageStyle.UNDECORATED);

            // 2. Inyectamos nuestro archivo de estilos CSS
            dialogo.getDialogPane().getStylesheets().add(getClass().getResource("/com/mitodolist/estilos.css").toExternalForm());
            dialogo.setGraphic(null); // Quitamos el icono azul por defecto
            dialogo.getDialogPane().getStyleClass().add("mi-dialogo");

            // 3. Motor de Física: Permitimos que la ventanita se pueda arrastrar con el mouse
            final double[] xOffset = {0};
            final double[] yOffset = {0};

            // Cuando el usuario hace clic en cualquier parte de la alerta...
            dialogo.getDialogPane().setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });

            // Cuando el usuario arrastra el mouse...
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
}