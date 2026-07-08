package com.mitodolist;
import javax.swing.JOptionPane;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class App {
    public static void main(String[] args) {

        ToDoList toDoList = new ToDoList();        
        JOptionPane.showMessageDialog(null, toDoList.generarReporteMatutino());

        String textoMenu = "Ingrese el número de la opción que desea:\n"
                         + "1. Agregar tarea\n"
                         + "2. Ver tareas\n"
                         + "3. Marcar como completada\n"
                         + "4. Eliminar tarea\n"
                         + "5. Editar tarea\n"
                         + "6. Salir";

        int opcion = solicitarNumero(textoMenu);        
        while (opcion != 6 && opcion != -1) {
            switch (opcion) {
                case 1:
                    String tarea = JOptionPane.showInputDialog("Ingrese la tarea que desea agregar:");
                    if (tarea != null && !tarea.trim().isEmpty()) {
                        LocalDate fecha = solicitarFecha("¿Desea agregar una fecha límite a esta tarea?");
                        
                        toDoList.agregarTarea(tarea.trim(), fecha);
                        
                        JOptionPane.showMessageDialog(null, "✅ La tarea se ha agregado correctamente");
                    } else {
                        JOptionPane.showMessageDialog(null, "⚠️ Error: La tarea no puede estar vacía.");
                    }
                    break;
                case 2:
                    JOptionPane.showMessageDialog(null, "Tareas: \n" + toDoList.verTareas());
                    break;
                case 3:
                    if (!toDoList.hayTareas()) {
                        JOptionPane.showMessageDialog(null, "⚠️ No hay tareas pendientes para completar.");
                        break; 
                    }
                    
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
                    if (!toDoList.hayTareas()) {
                        JOptionPane.showMessageDialog(null, "⚠️ No hay tareas para eliminar.");
                        break;
                    }
                    
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
                    
                case 5:
                    if (!toDoList.hayTareas()) {
                        JOptionPane.showMessageDialog(null, "⚠️ No hay tareas para editar.");
                        break;
                    }
                    
                    int indiceEditar = solicitarNumero("Ingrese el número de la tarea que desea editar: \n" + toDoList.verTareas());
                    
                    if (indiceEditar != -1) {
                        if (!toDoList.existeTarea(indiceEditar)) {
                            JOptionPane.showMessageDialog(null, "❌ Error: El número ingresado no corresponde a ninguna tarea.");
                            break;
                        }

                        boolean cambiosRealizados = false;

                        // --- BLOQUE 1: EDICIÓN DE TEXTO ---
                        int modificarTexto = JOptionPane.showConfirmDialog(null,"¿Desea modificar el texto de la descripción?", "Editar Texto", JOptionPane.YES_NO_OPTION);
                                
                        if (modificarTexto == JOptionPane.YES_OPTION) {
                            String nuevoTexto = JOptionPane.showInputDialog("Ingrese el nuevo texto para la tarea:");
                            if (nuevoTexto != null && !nuevoTexto.trim().isEmpty()) {
                                toDoList.editarTarea(indiceEditar, nuevoTexto.trim());
                                cambiosRealizados = true;
                            } else {
                                JOptionPane.showMessageDialog(null, "⚠️ Error: El texto no puede estar vacío. Se conservó el texto original.");
                            }
                        }

                        int modificarFecha = JOptionPane.showConfirmDialog(null, 
                                "¿Desea modificar o eliminar la fecha límite actual?", 
                                "Editar Fecha", JOptionPane.YES_NO_OPTION);
                                
                        if (modificarFecha == JOptionPane.YES_OPTION) {
                            LocalDate nuevaFecha = solicitarFecha("¿Desea establecer una nueva fecha límite para esta tarea?\n(Si selecciona 'No', la tarea se guardará SIN fecha)");
                            toDoList.editarFechaLimite(indiceEditar, nuevaFecha);
                            cambiosRealizados = true;
                        }

                        if (cambiosRealizados) {
                            JOptionPane.showMessageDialog(null, "✅ La tarea se ha actualizado correctamente.");
                        } else {
                            JOptionPane.showMessageDialog(null, "ℹ️ No se realizaron cambios en la tarea.");
                        }
                    }
                    break;
                default:
                    JOptionPane.showMessageDialog(null, "Opción no válida");
            }
            opcion = solicitarNumero(textoMenu);
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

    // Esta función pregunta si se desea una fecha, la pide y valida que sea correcta
    public static LocalDate solicitarFecha(String mensaje) {
        while (true) {
            int respuesta = JOptionPane.showConfirmDialog(null, mensaje, "Fecha Límite", JOptionPane.YES_NO_OPTION);
            
            if (respuesta != JOptionPane.YES_OPTION) {
                return null;
            }
            String fechaStr = JOptionPane.showInputDialog("Ingrese la fecha en formato AAAA-MM-DD \n(Ejemplo: 2026-12-31):");
            
            if (fechaStr == null || fechaStr.trim().isEmpty()) {
                return null; 
            }

            try {
                return LocalDate.parse(fechaStr.trim());
            } catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(null, "❌ Error: Formato de fecha incorrecto o fecha inexistente. Intente de nuevo.");
            }
        }
    }
}