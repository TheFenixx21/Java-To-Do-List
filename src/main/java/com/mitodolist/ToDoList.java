package com.mitodolist;
import java.util.ArrayList;

public class ToDoList {
    
    private ArrayList<Tarea> tareas;
    private GestorArchivos gestor;

    public ToDoList() {
        this.gestor = new GestorArchivos(); 
        this.tareas = gestor.cargarTareas(); 
    }

    public void agregarTarea(String descripcion) {
        Tarea nuevaTarea = new Tarea(descripcion);
        tareas.add(nuevaTarea);
        gestor.guardarTareas(tareas);
    }

    public String verTareas() {
        if (tareas.isEmpty()) {
            return "No hay tareas pendientes.\n";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tareas.size(); i++) {
            Tarea tareaActual = tareas.get(i);
            String estado = "[ ]";
            if (tareaActual.isCompletada()) {
                estado = "[X]";
            }
            sb.append(i + 1).append(". ").append(estado).append(" ").append(tareaActual.getDescripcion()).append("\n");
        }
        return sb.toString();
    }

    public void eliminarTarea(int indice) {
        int indiceReal = indice - 1;
        if (indiceReal >= 0 && indiceReal < tareas.size()) {
            tareas.remove(indiceReal);
            gestor.guardarTareas(tareas);
        }
    }

    public void marcarCompletada(int indice) {
        int indiceReal = indice - 1;
        if (indiceReal >= 0 && indiceReal < tareas.size()) {
            Tarea tareaSeleccionada = tareas.get(indiceReal);
            tareaSeleccionada.setCompletada(true);
            gestor.guardarTareas(tareas);
        }
    }
}