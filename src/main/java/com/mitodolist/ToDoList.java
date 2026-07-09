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

    public String verTareas(int filtro) {
        if (tareas.isEmpty()) {
            return "No hay tareas registradas.\n";
        }

        StringBuilder sb = new StringBuilder();
        java.time.LocalDate hoy = java.time.LocalDate.now();
        boolean hayResultados = false;

        for (int i = 0; i < tareas.size(); i++) {
            Tarea tareaActual = tareas.get(i);
            
            boolean mostrar = false;
            switch (filtro) {
                case 1: // Todas
                    mostrar = true;
                    break;
                case 2: // Solo Pendientes
                    mostrar = !tareaActual.isCompletada();
                    break;
                case 3: // Solo Completadas
                    mostrar = tareaActual.isCompletada();
                    break;
                case 4: // Solo Atrasadas (Pendientes + Fecha límite vencida)
                    mostrar = !tareaActual.isCompletada() && tareaActual.getFechaLimite() != null && tareaActual.getFechaLimite().isBefore(hoy);
                    break;
                default: 
                    mostrar = true; 
            }

           
            if (mostrar) {
                hayResultados = true;
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
        }

        if (!hayResultados) {
            return "No se encontraron tareas bajo este filtro.\n";
        }

        return sb.toString();
    }

    //sobrecarga del método verTareas para mostrar todas las tareas por defecto
    public String verTareas() {
        return verTareas(1); 
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
    public int contarPendientes() {
        int contador = 0;
        for (Tarea t : tareas) {
            if (!t.isCompletada()) contador++;
        }
        return contador;
    }

    public int contarCompletadas() {
        int contador = 0;
        for (Tarea t : tareas) {
            if (t.isCompletada()) contador++;
        }
        return contador;
    }

    public int contarAtrasadas() {
        int contador = 0;
        java.time.LocalDate hoy = java.time.LocalDate.now();
        for (Tarea t : tareas) {
            if (!t.isCompletada() && t.getFechaLimite() != null && t.getFechaLimite().isBefore(hoy)) {
                contador++;
            }
        }
        return contador;
    }
}