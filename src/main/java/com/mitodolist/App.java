package com.mitodolist;
import javax.swing.JOptionPane;

public class App {
    public static void main(String[] args) {
        JOptionPane.showMessageDialog(null, "Bienvenido a tu To-Do List");
        int opcion = solicitarNumero("Ingrese el numero de la opcion que desea: " + "\n1. Agregar tarea" + "\n2. Ver tareas" + "\n3. Marcar como completada" + "\n4. Eliminar tarea" + "\n5. Salir");
        ToDoList toDoList = new ToDoList();
        while (opcion != 5 && opcion != -1) {
            switch (opcion) {
                case 1:
                    String tarea = JOptionPane.showInputDialog("Ingrese la tarea que desea agregar:");
                    if (tarea != null && !tarea.trim().isEmpty()) {
                        toDoList.agregarTarea(tarea.trim()); 
                        JOptionPane.showMessageDialog(null, "✔ La tarea se ha agregado correctamente");
                    } else {
                        JOptionPane.showMessageDialog(null, "⚠️ Error: La tarea no puede estar vacía.");
                    }
                    break;
                case 2:
                    JOptionPane.showMessageDialog(null, "Tareas: \n" + toDoList.verTareas());
                    break;
                case 3:
                    int indice = solicitarNumero("Ingrese el numero de la tarea que desea marcar como completada: \n" + toDoList.verTareas());
                    if (indice != -1) {
                        boolean exito = toDoList.marcarCompletada(indice);                        
                        if (exito) {
                            JOptionPane.showMessageDialog(null, "✔️ La tarea se ha marcado como completada.");
                        } else {
                            JOptionPane.showMessageDialog(null, "❌ Error: El número ingresado no corresponde a ninguna tarea.");
                        }
                    }
                    break;
                    
                case 4:
                    int indice2 = solicitarNumero("Ingrese el numero de la tarea que desea eliminar: \n" + toDoList.verTareas());
                    if (indice2 != -1) {
                        boolean exito = toDoList.eliminarTarea(indice2);                        
                        if (exito) {
                            JOptionPane.showMessageDialog(null, "🗑️ La tarea se ha eliminado correctamente.");
                        } else {
                            JOptionPane.showMessageDialog(null, "❌ Error: El número ingresado no corresponde a ninguna tarea.");
                        }
                    }
                    break;
                default:
                    JOptionPane.showMessageDialog(null, "Opcion no valida");
            }
            opcion = solicitarNumero("Ingrese el numero de la opcion que desea: " + "\n1. Agregar tarea" + "\n2. Ver tareas" + "\n3. Marcar como completada" + "\n4. Eliminar tarea" + "\n5. Salir");
        }
    }

    // Esta función se encarga EXCLUSIVAMENTE de pedir números y manejar errores
    public static int solicitarNumero(String mensaje) {
        while (true) {
            String entrada = JOptionPane.showInputDialog(mensaje);
            
            if (entrada == null) {
                return -1; 
            }            
            try {
                return Integer.parseInt(entrada); 
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "⚠️ Error: Por favor, ingrese únicamente números enteros.");
            }
        }
    }
}