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

    private ToDoList logica = new ToDoList();
    private int filtroActual = 1;
    private String categoriaActual = "Todas";

    @FXML
    public void initialize() {
        comboFiltros.getItems().addAll("Todas las tareas", "⏳ Pendientes", "✅ Completadas", "⚠️ Atrasadas");
        comboFiltros.getSelectionModel().selectFirst();
        
        comboFiltros.setOnAction(evento -> {
            filtroActual = comboFiltros.getSelectionModel().getSelectedIndex() + 1;
            actualizarInterfaz();
        });

        // --- MENÚ CONTEXTUAL V4.0.0e ---
        ContextMenu menuClickDerecho = new ContextMenu();
        
        MenuItem menuCompletar = new MenuItem("✅ Marcar/Desmarcar como Completada");
        menuCompletar.setOnAction(e -> accionCompletar());
        
        MenuItem menuAgregarSub = new MenuItem("➕ Agregar Sub-tarea");
        menuAgregarSub.setOnAction(e -> accionAgregarSubTarea());

        MenuItem menuEditarDesc = new MenuItem("✏️ Editar Descripción");
        menuEditarDesc.setOnAction(e -> accionEditarDescripcion());
        
        MenuItem menuEditarFecha = new MenuItem("📅 Editar Fecha Límite");
        menuEditarFecha.setOnAction(e -> accionEditarFecha());

        MenuItem menuEliminar = new MenuItem("🗑️ Eliminar");
        menuEliminar.setOnAction(e -> accionEliminar());

        MenuItem menuEditarCat = new MenuItem("🗂️ Editar Categoría (Solo principal)");
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
                        int idReal = logica.obtenerTareasFiltradas(1, "Todas").indexOf(tareaActual) + 1;
                        setText(idReal + ". " + estado + " " + tareaActual.getDescripcion() + textoFecha);
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
        
        ArrayList<Tarea> listaPrincipales = logica.obtenerTareasFiltradas(filtroActual, categoriaActual);
        
        for (Tarea principal : listaPrincipales) {
            listaTareas.getItems().add(principal); 
            
            // LÓGICA V4.0.0e: Solo mostramos las hijas si la madre está "expandida"
            if (principal.isExpandida() && principal.getSubTareas() != null && !principal.getSubTareas().isEmpty()) {
                listaTareas.getItems().addAll(principal.getSubTareas());
            }
        }
        
        lblPendientes.setText("Pendientes: " + logica.contarPendientes());
        lblCompletadas.setText("Completadas: " + logica.contarCompletadas());
        lblAtrasadas.setText("Atrasadas: " + logica.contarAtrasadas());
    }

    // --- NUEVA ACCIÓN PARA EL MENÚ CONTEXTUAL ---
    private void accionAlternarDespliegue() {
        Tarea seleccionada = listaTareas.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            if (seleccionada.getIdTareaPadre() == null) {
                // Solo las tareas principales se pueden plegar/desplegar
                seleccionada.setExpandida(!seleccionada.isExpandida());
                actualizarInterfaz(); // Recargamos la pantalla al instante
            } else {
                mostrarAlertaRapida("Información", "Esta acción es solo para tareas principales.");
            }
        }
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
                
                javafx.scene.control.MenuItem menuRenombrar = new javafx.scene.control.MenuItem("✏️ Renombrar Lista");
                menuRenombrar.setOnAction(eventoClick -> accionRenombrarCategoria(cat));
                
                javafx.scene.control.MenuItem menuEliminar = new javafx.scene.control.MenuItem("🗑️ Eliminar Lista");
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

    // --- ACCIONES DEL CLICK DERECHO REESCRITAS PARA JERARQUÍA ---

    private void accionAgregarSubTarea() {
        Tarea seleccionada = listaTareas.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            // Verificamos que no sea ya una subtarea (evitar Inception)
            if (seleccionada.getIdTareaPadre() != null) {
                mostrarAlertaRapida("Acción no permitida", "No puedes agregar una subtarea dentro de otra subtarea.");
                return;
            }

            TextInputDialog dialogo = new TextInputDialog();
            dialogo.setTitle("Nueva Sub-tarea");
            dialogo.setHeaderText("Agregando paso a: " + seleccionada.getDescripcion());
            dialogo.setContentText("Descripción de la sub-tarea:");

            Optional<String> resultado = dialogo.showAndWait();
            resultado.ifPresent(desc -> {
                if (!desc.trim().isEmpty()) {
                    int indiceReal = logica.obtenerTareasFiltradas(1, "Todas").indexOf(seleccionada) + 1;
                    logica.agregarSubTarea(indiceReal, desc.trim());
                    actualizarInterfaz();
                }
            });
        }
    }

    private void accionCompletar() {
        Tarea seleccionada = listaTareas.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            if (seleccionada.getIdTareaPadre() != null) {
                logica.alternarEstadoSubTarea(seleccionada); // Acción exclusiva para hijas
            } else {
                int idReal = logica.obtenerTareasFiltradas(1, "Todas").indexOf(seleccionada) + 1;
                logica.alternarEstadoTarea(idReal); // Acción para madres
            }
            actualizarInterfaz();
        }
    }

    private void accionEliminar() {
        Tarea seleccionada = listaTareas.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            if (seleccionada.getIdTareaPadre() != null) {
                // Buscamos quién es su madre para desconectarla
                Tarea padre = encontrarPadre(seleccionada);
                if (padre != null) {
                    logica.eliminarSubTarea(padre, seleccionada);
                }
            } else {
                int idReal = logica.obtenerTareasFiltradas(1, "Todas").indexOf(seleccionada) + 1;
                logica.eliminarTarea(idReal);
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

            Optional<String> resultado = dialogo.showAndWait();
            resultado.ifPresent(nuevaDesc -> {
                if (!nuevaDesc.trim().isEmpty()) {
                    if (seleccionada.getIdTareaPadre() != null) {
                        seleccionada.setDescripcion(nuevaDesc);
                        GestorBaseDatos.actualizarTarea(seleccionada); // Directo al bisturí
                    } else {
                        int idReal = logica.obtenerTareasFiltradas(1, "Todas").indexOf(seleccionada) + 1;
                        logica.editarTarea(idReal, nuevaDesc);
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
            dialogo.setHeaderText("Selecciona la nueva fecha para esta tarea:");

            DatePicker dpNuevo = new DatePicker(seleccionada.getFechaLimite());
            dialogo.getDialogPane().setContent(dpNuevo);
            dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialogo.setResultConverter(boton -> (boton == ButtonType.OK) ? dpNuevo.getValue() : null);

            Optional<java.time.LocalDate> resultado = dialogo.showAndWait();
            resultado.ifPresent(nuevaFecha -> {
                if (seleccionada.getIdTareaPadre() != null) {
                    seleccionada.setFechaLimite(nuevaFecha);
                    GestorBaseDatos.actualizarTarea(seleccionada);
                } else {
                    int idReal = logica.obtenerTareasFiltradas(1, "Todas").indexOf(seleccionada) + 1;
                    logica.editarFechaLimite(idReal, nuevaFecha);
                }
                actualizarInterfaz();
            });
        }
    }

    private void accionEditarCategoria() {
        Tarea seleccionada = listaTareas.getSelectionModel().getSelectedItem();
        if (seleccionada != null) {
            if (seleccionada.getIdTareaPadre() != null) {
                mostrarAlertaRapida("Acción no permitida", "Las subtareas siempre pertenecen a la misma categoría que su tarea principal. Cambia la lista de la tarea principal.");
                return;
            }

            java.util.List<String> opciones = new java.util.ArrayList<>();
            for (Categoria cat : GestorBaseDatos.obtenerCategorias()) opciones.add(cat.getNombre());

            javafx.scene.control.ChoiceDialog<String> dialogo = new javafx.scene.control.ChoiceDialog<>(seleccionada.getCategoria(), opciones);
            dialogo.setTitle("Editar Categoría");
            dialogo.setHeaderText("Mover tarea a otra lista:");
            dialogo.setContentText("Nueva lista destino:");

            Optional<String> resultado = dialogo.showAndWait();
            resultado.ifPresent(nuevaCat -> {
                int idReal = logica.obtenerTareasFiltradas(1, "Todas").indexOf(seleccionada) + 1;
                logica.editarCategoria(idReal, nuevaCat);
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
}