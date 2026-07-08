package com.mitodolist;
import java.util.ArrayList;

public class ToDoList {
    
    private ArrayList<Tarea> tareas;
    private GestorArchivos gestor;

    public ToDoList() {
        this.gestor = new GestorArchivos(); 
        this.tareas = gestor.cargarTareas(); 
    }

    //Radares
    public boolean hayTareas() {
        return !tareas.isEmpty(); 
    }

    public boolean existeTarea(int indice) {
        int indiceReal = indice - 1;
        return indiceReal >= 0 && indiceReal < tareas.size();
    }

    //Métodos CRUD
    public void agregarTarea(String descripcion, java.time.LocalDate fechaLimite) {
        Tarea nuevaTarea = new Tarea(descripcion);
        nuevaTarea.setFechaLimite(fechaLimite);
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
            String textoFecha = "";
            if (tareaActual.getFechaLimite() != null) {
                textoFecha = " 📅 [Vence: " + tareaActual.getFechaLimite().toString() + "]";
            }
            
            sb.append(i + 1).append(". ").append(estado).append(" ").append(tareaActual.getDescripcion()).append(textoFecha).append("\n");
        }
        return sb.toString();
    }

    public boolean marcarCompletada(int indice) {
        if (existeTarea(indice)) {
            tareas.get(indice - 1).setCompletada(true);
            gestor.guardarTareas(tareas);
            return true;
        }
        return false;
    }

    public boolean eliminarTarea(int indice) {
        if (existeTarea(indice)) {
            tareas.remove(indice - 1);
            gestor.guardarTareas(tareas);
            return true;
        }
        return false;
    }

    public boolean editarTarea(int indice, String nuevaDescripcion) {
        if (existeTarea(indice)) {
            tareas.get(indice - 1).setDescripcion(nuevaDescripcion);
            gestor.guardarTareas(tareas);
            return true;
        }
        return false;
    }

    public boolean editarFechaLimite(int indice, java.time.LocalDate nuevaFecha) {
        if (existeTarea(indice)) {
            tareas.get(indice - 1).setFechaLimite(nuevaFecha);
            gestor.guardarTareas(tareas);
            return true;
        }
        return false;
    }

    //Reporte matutino
    public String generarReporteMatutino() {
        if (tareas.isEmpty()) {
            return "🌅 ¡Buenos días! Tu lista de tareas está completamente limpia. ¡Excelente día!";
        }

        java.time.LocalDate hoy = java.time.LocalDate.now(); // ⏱️ Captura la fecha real de hoy
        int tareasParaHoy = 0;
        int tareasAtrasadas = 0;
        int tareasPendientesSanas = 0;

        for (Tarea tarea : tareas) {
            // Solo contamos las tareas que NO estén completadas
            if (!tarea.isCompletada() && tarea.getFechaLimite() != null) {
                if (tarea.getFechaLimite().isEqual(hoy)) {
                    tareasParaHoy++;
                } else if (tarea.getFechaLimite().isBefore(hoy)) {
                    tareasAtrasadas++;
                } else {
                    tareasPendientesSanas++;
                }
            }
        }

        if (tareasParaHoy == 0 && tareasAtrasadas == 0) {
            return "🌅 ¡Buenos días! No tienes tareas urgentes para hoy. Tienes " + tareasPendientesSanas + " tarea(s) en espera.";
        }
        return "🌅 [REPORTE MATUTINO]\n"
             + "⚠️ Tareas atrasadas: " + tareasAtrasadas + "\n"
             + "📅 Tareas para HOY: " + tareasParaHoy + "\n"
             + "⏳ Tareas futuras: " + tareasPendientesSanas;
    }
}