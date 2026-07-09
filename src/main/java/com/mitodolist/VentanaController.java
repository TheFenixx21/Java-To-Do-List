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
// --- NUEVAS HERRAMIENTAS PARA LOS POP-UPS ---
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public class VentanaController {

    @FXML private ComboBox<String> comboFiltros;
    @FXML private ListView<String> listaTareas;
    @FXML private TextField txtNuevaTarea;
    @FXML private Button btnAgregar;
    @FXML private DatePicker calendarioPrincipal;
    @FXML private Label lblPendientes;
    @FXML private Label lblCompletadas;
    @FXML private Label lblAtrasadas;
    @FXML private TextArea txtNotasRapidas;

    private ToDoList logica = new ToDoList();
    private int filtroActual = 1;

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
        
        MenuItem menuCompletar = new MenuItem("✅ Marcar como Completada");
        menuCompletar.setOnAction(e -> accionCompletar());
        
        MenuItem menuEditarDesc = new MenuItem("✏️ Editar Descripción");
        menuEditarDesc.setOnAction(e -> accionEditarDescripcion());
        
        MenuItem menuEditarFecha = new MenuItem("📅 Editar Fecha Límite");
        menuEditarFecha.setOnAction(e -> accionEditarFecha());

        MenuItem menuEliminar = new MenuItem("🗑️ Eliminar Tarea");
        menuEliminar.setOnAction(e -> accionEliminar());
        
        // Agregamos todas las opciones al menú
        menuClickDerecho.getItems().addAll(menuCompletar, menuEditarDesc, menuEditarFecha, menuEliminar);
        listaTareas.setContextMenu(menuClickDerecho);

        txtNuevaTarea.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER) {
                agregarNuevaTarea();
            }
        });

        actualizarInterfaz();
    }

    private void actualizarInterfaz() {
        listaTareas.getItems().clear();
        
        String tareasTexto = logica.verTareas(filtroActual);
        
        if (!tareasTexto.startsWith("No hay tareas") && !tareasTexto.startsWith("No se encontraron")) {
            String[] lineas = tareasTexto.split("\n");
            listaTareas.getItems().addAll(lineas);
        } else {
            listaTareas.getItems().add(tareasTexto);
        }
        
        lblPendientes.setText("Pendientes: " + logica.contarPendientes());
        lblCompletadas.setText("Completadas: " + logica.contarCompletadas());
        lblAtrasadas.setText("Atrasadas: " + logica.contarAtrasadas());
    }

    @FXML
    public void agregarNuevaTarea() {
        String texto = txtNuevaTarea.getText();
        java.time.LocalDate fecha = calendarioPrincipal.getValue();

        if (texto != null && !texto.trim().isEmpty()) {
            logica.agregarTarea(texto, fecha);
            actualizarInterfaz();
            txtNuevaTarea.clear();
            calendarioPrincipal.setValue(null);
        }
    }

    // --- ACCIONES DEL CLICK DERECHO ---

    private void accionCompletar() {
        int idReal = obtenerIdTareaSeleccionada();
        if (idReal != -1) {
            logica.marcarCompletada(idReal);
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
        String seleccion = listaTareas.getSelectionModel().getSelectedItem();
        
        if (seleccion == null || seleccion.startsWith("No ")) {
            return -1; 
        }
        
        try {
            String[] partes = seleccion.split("\\.");
            return Integer.parseInt(partes[0].trim());
        } catch (Exception e) {
            return -1;
        }
    }
}