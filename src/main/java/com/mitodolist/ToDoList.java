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
    public void agregarTarea(String descripcion, java.time.LocalDate fechaLimite, String categoria) {
        Tarea nueva = new Tarea(descripcion);
        nueva.setFechaLimite(fechaLimite);
        nueva.setCategoria(categoria);
        tareas.add(nueva);
        gestor.guardarTareas(tareas);
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

    public boolean editarCategoria(int indice, String nuevaCategoria) {
        if (existeTarea(indice)) {
            tareas.get(indice - 1).setCategoria(nuevaCategoria);
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

    public ArrayList<Tarea> obtenerTareasFiltradas(int filtro, String categoriaDeseada) {
        ArrayList<Tarea> resultado = new ArrayList<>();
        java.time.LocalDate hoy = java.time.LocalDate.now();

        for (Tarea tareaActual : tareas) {
            // 1. ¿Cumple con el estado (Pendiente/Completada)?
            boolean incluirPorFiltro = false;
            switch (filtro) {
                case 1: incluirPorFiltro = true; break;
                case 2: incluirPorFiltro = !tareaActual.isCompletada(); break;
                case 3: incluirPorFiltro = tareaActual.isCompletada(); break;
                case 4: incluirPorFiltro = !tareaActual.isCompletada() && tareaActual.getFechaLimite() != null && tareaActual.getFechaLimite().isBefore(hoy); break;
                default: incluirPorFiltro = true;
            }

            // 2. ¿Cumple con la categoría elegida en el menú lateral?
            boolean incluirPorCategoria = categoriaDeseada.equals("Todas") || tareaActual.getCategoria().equals(categoriaDeseada);

            // Si pasa ambas pruebas, se muestra en pantalla
            if (incluirPorFiltro && incluirPorCategoria) {
                resultado.add(tareaActual);
            }
        }
        return resultado;
    }
}