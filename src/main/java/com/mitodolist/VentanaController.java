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

        // --- MENÚ CONTEXTUAL MEJORADO ---
        ContextMenu menuClickDerecho = new ContextMenu();
        
        MenuItem menuCompletar = new MenuItem("✅ Marcar/Desmarcar como Completada");
        menuCompletar.setOnAction(e -> accionCompletar());
        
        MenuItem menuEditarDesc = new MenuItem("✏️ Editar Descripción");
        menuEditarDesc.setOnAction(e -> accionEditarDescripcion());
        
        MenuItem menuEditarFecha = new MenuItem("📅 Editar Fecha Límite");
        menuEditarFecha.setOnAction(e -> accionEditarFecha());

        MenuItem menuEliminar = new MenuItem("🗑️ Eliminar Tarea");
        menuEliminar.setOnAction(e -> accionEliminar());

        MenuItem menuEditarCat = new MenuItem("🗂️ Editar Categoría");
        menuEditarCat.setOnAction(e -> accionEditarCategoria());
        
        // ¡No olvides agregarlo a la lista final del menú!
        menuClickDerecho.getItems().addAll(menuCompletar, menuEditarDesc, menuEditarFecha, menuEditarCat, menuEliminar);
        listaTareas.setContextMenu(menuClickDerecho);

        txtNuevaTarea.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER) {
                agregarNuevaTarea();
            }
        });

        // --- FÁBRICA DE CELDAS: CÓMO PINTAR CADA TAREA ---
        listaTareas.setCellFactory(parametro -> new javafx.scene.control.ListCell<Tarea>() {
            @Override
            protected void updateItem(Tarea tareaActual, boolean vacio) {
                super.updateItem(tareaActual, vacio);

                if (vacio || tareaActual == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // 1. Rescatamos tu formato de fecha (DD/MM/AAAA)
                    String textoFecha = "";
                    if (tareaActual.getFechaLimite() != null) {
                        java.time.format.DateTimeFormatter formato = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        textoFecha = " 📅 [Vence: " + tareaActual.getFechaLimite().format(formato) + "]";
                    }

                    // 2. Calculamos el ID real de la tarea
                    int idReal = logica.obtenerTareasFiltradas(1, "Todas").indexOf(tareaActual) + 1;
                    
                    // 3. Armamos el texto base
                    String estado = tareaActual.isCompletada() ? "[X]" : "[ ]";
                    setText(idReal + ". " + estado + " " + tareaActual.getDescripcion() + textoFecha);

                    // 4. EL INDICADOR VISUAL (Círculo de estado)
                    javafx.scene.shape.Circle indicador = new javafx.scene.shape.Circle(6); // Radio de 6 píxeles
                    java.time.LocalDate hoy = java.time.LocalDate.now();
                    
                    if (tareaActual.isCompletada()) {
                        // 🟢 Verde para completadas
                        indicador.setFill(javafx.scene.paint.Color.web("#4CAF50")); 
                    } else if (tareaActual.getFechaLimite() != null) {
                        if (tareaActual.getFechaLimite().isBefore(hoy)) {
                            // 🔴 Rojo para atrasadas
                            indicador.setFill(javafx.scene.paint.Color.web("#F44336")); 
                        } else if (tareaActual.getFechaLimite().isEqual(hoy)) {
                            // 🟡 Amarillo para tareas de HOY (Alerta / Urgente)
                            indicador.setFill(javafx.scene.paint.Color.web("#FFEB3B"));
                        } else {
                            // 🔵 Azul para tareas a futuro (Tranquilidad)
                            indicador.setFill(javafx.scene.paint.Color.web("#2196F3"));
                        }
                    } else {
                        // ⚪ Gris neutral para tareas sin fecha (Ideas / Backlog a futuro)
                        indicador.setFill(javafx.scene.paint.Color.web("#9E9E9E")); 
                    }

                    setGraphic(indicador); 
                }
            }
        });

        actualizarInterfaz();
        cargarMenuLateral();
    }

    private void actualizarInterfaz() {
    listaTareas.getItems().clear();
    
    java.util.ArrayList<Tarea> listaNueva = logica.obtenerTareasFiltradas(filtroActual, categoriaActual);
    
    if (!listaNueva.isEmpty()) {
        listaTareas.getItems().addAll(listaNueva);
    }
    
    lblPendientes.setText("Pendientes: " + logica.contarPendientes());
    lblCompletadas.setText("Completadas: " + logica.contarCompletadas());
    lblAtrasadas.setText("Atrasadas: " + logica.contarAtrasadas());
    }

    private void cargarMenuLateral() {
        // 1. Vaciamos la caja fuerte
        contenedorCategorias.getChildren().clear();

        // 2. Creamos el botón fijo de "Ver Todas"
        javafx.scene.control.Button btnVerTodas = new javafx.scene.control.Button("🔎 Ver Todas");
        btnVerTodas.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: normal; -fx-cursor: hand; -fx-padding: 8 0 8 10;");
        btnVerTodas.setOnAction(e -> {
            categoriaActual = "Todas";
            actualizarInterfaz();
        });
        contenedorCategorias.getChildren().add(btnVerTodas);

        // 3. Traemos las categorías de la BD (ahora ya vienen con su emoji gracias al parche)
        java.util.ArrayList<Categoria> categoriasBD = GestorBaseDatos.obtenerCategorias();

        // 4. Fabricamos los botones dinámicos
        for (Categoria cat : categoriasBD) {
            
            // Usamos directamente el nombre de la BD (ej. "💼 Trabajo" o "📁 Mi Lista")
            javafx.scene.control.Button btn = new javafx.scene.control.Button(cat.getNombre());
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: normal; -fx-cursor: hand; -fx-padding: 8 0 8 10;");
            
            btn.setOnAction(e -> {
                categoriaActual = cat.getNombre();
                actualizarInterfaz();
            });

            // 5. Le pegamos el menú del clic derecho (Protegiendo el ID 1)
            if (cat.getId() != 1) {
                javafx.scene.control.ContextMenu menuLista = new javafx.scene.control.ContextMenu();
                
                javafx.scene.control.MenuItem menuRenombrar = new javafx.scene.control.MenuItem("✏️ Renombrar Lista");
                menuRenombrar.setOnAction(eventoClick -> accionRenombrarCategoria(cat));
                
                javafx.scene.control.MenuItem menuEliminar = new javafx.scene.control.MenuItem("🗑️ Eliminar Lista");
                menuEliminar.setOnAction(eventoClick -> accionEliminarCategoria(cat));
                
                menuLista.getItems().addAll(menuRenombrar, menuEliminar);
                btn.setContextMenu(menuLista); // Le conectamos el menú al botón
            }

            // Inyectamos el botón en la pantalla
            contenedorCategorias.getChildren().add(btn);
        }
    }

   @FXML
    public void agregarNuevaTarea() {
        String texto = txtNuevaTarea.getText();
        java.time.LocalDate fecha = calendarioPrincipal.getValue();

        if (texto != null && !texto.trim().isEmpty()) {
            
            // --- NUEVA LÓGICA V1.6.0: ESCÁNER ANTI-DUPLICADOS (MULTIPLE) ---
            java.util.ArrayList<Tarea> tareasExistentes = logica.buscarTareasPorNombre(texto);
            
            if (!tareasExistentes.isEmpty()) {
                int cantidadDuplicados = tareasExistentes.size();
                
                // Extraemos la última tarea de la lista (la más reciente que el usuario creó)
                Tarea ultimaTarea = tareasExistentes.get(cantidadDuplicados - 1);

                // Preparamos la información de esa última tarea
                String infoFecha = (ultimaTarea.getFechaLimite() != null) 
                    ? ultimaTarea.getFechaLimite().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
                    : "Sin fecha límite";
                String estado = ultimaTarea.isCompletada() ? "Completada" : "Pendiente";
                String categoriaOriginal = ultimaTarea.getCategoria();

                // Creamos el pop-up dinámico
                javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                alerta.setTitle("Tarea Recurrente Detectada");
                alerta.setHeaderText("¡Atención! Tienes " + cantidadDuplicados + " tarea(s) registrada(s) como: '" + texto + "'");
                alerta.setContentText("Detalles de la coincidencia más reciente:\n"
                        + "• Estado: " + estado + "\n"
                        + "• Categoría: " + categoriaOriginal + "\n"
                        + "• Fecha límite: " + infoFecha + "\n\n"
                        + "¿Estás seguro de que deseas agregar un nuevo registro para esta tarea?");

                java.util.Optional<javafx.scene.control.ButtonType> respuesta = alerta.showAndWait();
                
                // Si el usuario presiona "Cancelar" o cierra la ventana, abortamos
                if (respuesta.isPresent() && respuesta.get() != javafx.scene.control.ButtonType.OK) {
                    return; 
                }
            }

            // (Aquí continúa tu código normal con la lista de opciones y el ChoiceDialog...)
            // --- FIN LÓGICA V1.6.0 ---

            // Si llegamos hasta aquí, es porque no era duplicada, o el usuario confirmó querer duplicarla.
            // Continuamos con el flujo normal de asignar categoría.
            
            // 1. Vamos a SQLite y extraemos las categorías reales
            java.util.ArrayList<Categoria> listaCategoriasBD = GestorBaseDatos.obtenerCategorias();
            
            // 2. Extraemos solo los nombres para mostrarlos en el menú desplegable
            java.util.List<String> opciones = new java.util.ArrayList<>();
            for (Categoria cat : listaCategoriasBD) {
                opciones.add(cat.getNombre());
            }

            String sugerencia = categoriaActual.equals("Todas") ? "Sin categoría" : categoriaActual;

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

    //CRUD DE CATEGORÍAS

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
                
                // --- INTELIGENCIA DE EMOJIS ---
                // Si el primer carácter es una letra o número normal, inyectamos la carpeta
                if (Character.isLetterOrDigit(texto.codePointAt(0))) {
                    texto = "📁 " + texto;
                }
                
                GestorBaseDatos.insertarCategoria(texto, "#FFFFFF");
                cargarMenuLateral();
            }
        });
    }

    private void accionRenombrarCategoria(Categoria cat) {
        // Le mostramos el nombre actual (para que pueda mantener su emoji si quiere)
        TextInputDialog dialogo = new TextInputDialog(cat.getNombre());
        dialogo.setTitle("Renombrar Lista");
        dialogo.setHeaderText("Modifica el nombre de tu lista:");
        dialogo.setContentText("Nuevo nombre:");

        Optional<String> resultado = dialogo.showAndWait();
        resultado.ifPresent(nuevoNombre -> {
            String texto = nuevoNombre.trim();
            if (!texto.isEmpty()) {
                
                // Misma inteligencia de emojis
                if (Character.isLetterOrDigit(texto.codePointAt(0))) {
                    texto = "📁 " + texto;
                }
                
                GestorBaseDatos.actualizarNombreCategoria(cat.getId(), texto);

                logica.sincronizarConBaseDatos();
                
                if (categoriaActual.equals(cat.getNombre())) {
                    categoriaActual = texto;
                }
                
                cargarMenuLateral();
                actualizarInterfaz(); 
            }
        });
    }

    private void accionEliminarCategoria(Categoria cat) {
        int cantidadTareas = GestorBaseDatos.contarTareasEnCategoria(cat.getId());

        // Alerta de Advertencia (Warning)
        javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alerta.setTitle("Eliminar Lista");
        alerta.setHeaderText("¿Eliminar permanentemente '" + cat.getNombre() + "'?");
        alerta.setContentText("Esta lista contiene " + cantidadTareas + " tarea(s).\n\n" + "⚠ ADVERTENCIA: Si eliminas esta lista, TODAS las tareas en su interior serán destruidas para siempre.\n\n" + "¿Deseas continuar?");

        alerta.getButtonTypes().setAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        Optional<ButtonType> respuesta = alerta.showAndWait();
        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            
            GestorBaseDatos.eliminarCategoria(cat.getId());

            logica.sincronizarConBaseDatos();
            
            if (categoriaActual.equals(cat.getNombre())) {
                categoriaActual = "Todas";
            }
            
            cargarMenuLateral();
            actualizarInterfaz();
        }
    }

    // --- ACCIONES DEL CLICK DERECHO ---

    private void accionCompletar() {
        int idReal = obtenerIdTareaSeleccionada();
        if (idReal != -1) {
            logica.alternarEstadoTarea(idReal);
            actualizarInterfaz();
        }
    }

    private void accionEliminar() {
        int idReal = obtenerIdTareaSeleccionada();
        if (idReal != -1) {
            logica.eliminarTarea(idReal);
            actualizarInterfaz();
        }
    }

    // --- NUEVO: ACCIÓN EDITAR DESCRIPCIÓN ---
    private void accionEditarDescripcion() {
        int idReal = obtenerIdTareaSeleccionada();
        if (idReal != -1) {
            // Creamos una ventana emergente de texto
            TextInputDialog dialogo = new TextInputDialog();
            dialogo.setTitle("Editar Tarea");
            dialogo.setHeaderText("Modifica la descripción de la tarea:");
            dialogo.setContentText("Nueva descripción:");

            // Capturamos lo que el usuario escriba
            Optional<String> resultado = dialogo.showAndWait();
            resultado.ifPresent(nuevaDesc -> {
                if (!nuevaDesc.trim().isEmpty()) {
                    logica.editarTarea(idReal, nuevaDesc);
                    actualizarInterfaz(); // Refrescamos la lista
                }
            });
        }
    }

    private void accionEditarCategoria() {
        // 1. Conseguimos la tarea exacta que el usuario seleccionó
        Tarea tareaSeleccionada = listaTareas.getSelectionModel().getSelectedItem();
        int idReal = obtenerIdTareaSeleccionada();
        
        if (idReal != -1 && tareaSeleccionada != null) {
            // 1. Vamos a SQLite y extraemos las categorías reales
            java.util.ArrayList<Categoria> listaCategoriasBD = GestorBaseDatos.obtenerCategorias();
            
            // 2. Extraemos solo los nombres para mostrarlos en el menú desplegable
            java.util.List<String> opciones = new java.util.ArrayList<>();
            for (Categoria cat : listaCategoriasBD) {
                opciones.add(cat.getNombre());
            }

            // 3. Creamos el pop-up, sugiriendo la categoría que YA tiene la tarea
            javafx.scene.control.ChoiceDialog<String> dialogo = new javafx.scene.control.ChoiceDialog<>(tareaSeleccionada.getCategoria(), opciones);
            dialogo.setTitle("Editar Categoría");
            dialogo.setHeaderText("Mover tarea a otra lista:");
            dialogo.setContentText("Nueva lista destino:");

            // 4. Capturamos la respuesta
            java.util.Optional<String> resultado = dialogo.showAndWait();
            resultado.ifPresent(nuevaCat -> {
                logica.editarCategoria(idReal, nuevaCat);
                actualizarInterfaz();
            });
        }
    }

    // --- NUEVO: ACCIÓN EDITAR FECHA ---
    private void accionEditarFecha() {
        int idReal = obtenerIdTareaSeleccionada();
        if (idReal != -1) {
            // Creamos un cuadro de diálogo personalizado
            Dialog<java.time.LocalDate> dialogo = new Dialog<>();
            dialogo.setTitle("Editar Fecha Límite");
            dialogo.setHeaderText("Selecciona la nueva fecha para esta tarea:");

            // Le metemos un DatePicker al cuadro de diálogo
            DatePicker dpNuevo = new DatePicker();
            dialogo.getDialogPane().setContent(dpNuevo);
            dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // Convertimos el click del botón OK en la fecha seleccionada
            dialogo.setResultConverter(boton -> {
                if (boton == ButtonType.OK) {
                    return dpNuevo.getValue();
                }
                return null;
            });

            // Capturamos la fecha y la guardamos
            Optional<java.time.LocalDate> resultado = dialogo.showAndWait();
            resultado.ifPresent(nuevaFecha -> {
                logica.editarFechaLimite(idReal, nuevaFecha);
                actualizarInterfaz(); // Refrescamos la lista
            });
        }
    }

    private int obtenerIdTareaSeleccionada() {
    Tarea tareaSeleccionada = listaTareas.getSelectionModel().getSelectedItem();
    
    if (tareaSeleccionada == null) {
        return -1; 
    }
    
    int indiceReal = logica.obtenerTareasFiltradas(1, "Todas").indexOf(tareaSeleccionada) + 1;
    
    return indiceReal;
    }
}