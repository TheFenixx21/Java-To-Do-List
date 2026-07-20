package com.mitodolist;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private Label lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private PasswordField txtPin;
    @FXML private CheckBox chkRecordar;
    @FXML private Button btnAcceder;
    @FXML private Button btnCambiarModo;

    private boolean modoRegistro = false; // Empezamos siempre en modo "Ingreso"

    @FXML
    public void initialize() {
        // Si no hay NINGÚN usuario, forzamos el modo registro
        if (!GestorBaseDatos.existeUsuarioRegistrado()) {
            activarModoRegistro();
            btnCambiarModo.setVisible(false); // No puede iniciar sesión si no hay cuentas
        } else {
            activarModoIngreso();
        }
    }

    @FXML
    public void alternarModo() {
        if (modoRegistro) {
            activarModoIngreso();
        } else {
            activarModoRegistro();
        }
    }

    private void activarModoRegistro() {
        modoRegistro = true;
        lblTitulo.setText("Crear Nueva Cuenta");
        btnAcceder.setText("Registrarse");
        btnCambiarModo.setText("¿Ya tienes cuenta? Ingresa aquí");
    }

    private void activarModoIngreso() {
        modoRegistro = false;
        lblTitulo.setText("Iniciar Sesión");
        btnAcceder.setText("Entrar");
        btnCambiarModo.setText("¿No tienes cuenta? Regístrate");
    }

    @FXML
    public void accionAcceder() {
        String nombre = txtNombre.getText().trim();
        String pin = txtPin.getText();
        boolean recordar = chkRecordar.isSelected();

        if (nombre.isEmpty() || pin.trim().isEmpty()) {
            mostrarError("Por favor, llena todos los campos.");
            return;
        }

        if (modoRegistro) {
            // VERIFICACIÓN: Evitar nombres duplicados
            if (GestorBaseDatos.existeNombreUsuario(nombre)) {
                mostrarError("Ese nombre de usuario ya está en uso. Elige otro.");
                return;
            }
            
            GestorBaseDatos.registrarUsuario(nombre, pin, recordar);
            App.cambiarEscena("VentanaPrincipal.fxml", "Mi TodoList V4.0.0e");
            
        } else {
            // MODO INGRESO: Verificamos credenciales exactas
            if (GestorBaseDatos.autenticarUsuario(nombre, pin)) {
                
                // --- AQUÍ ESTÁ LA CORRECCIÓN ---
                GestorBaseDatos.actualizarTokenSesion(recordar); 
                
                App.cambiarEscena("VentanaPrincipal.fxml", "Mi TodoList V4.0.0e");
            } else {
                mostrarError("Credenciales incorrectas. Inténtalo de nuevo.");
            }
        }
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error de Acceso");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}