package com.mitodolist;
import java.util.ArrayList;

public class ToDoList {
    
    private ArrayList<Tarea> tareas;
    private GestorArchivos gestor;

    public ToDoList() {
        this.gestor = new GestorArchivos(); 
        this.tareas = gestor.cargarTareas();
        iniciarVigilanteNotificaciones(); 
    }

    //Radares
    public boolean hayTareas() {
        return !tareas.isEmpty(); 
    }

    public boolean existeTarea(int indice) {
        int indiceReal = indice - 1;
        return indiceReal >= 0 && indiceReal < tareas.size();
    }

    // --- V1.6.0: Escáner Anti-Duplicados (Avanzado) ---
    public java.util.ArrayList<Tarea> buscarTareasPorNombre(String descripcion) {
        java.util.ArrayList<Tarea> encontradas = new java.util.ArrayList<>();
        
        for (Tarea t : tareas) {
            if (t.getDescripcion().trim().equalsIgnoreCase(descripcion.trim())) {
                encontradas.add(t); // En lugar de detenerse, la guarda en la lista y sigue buscando
            }
        }
        
        return encontradas; // Devuelve la lista completa de clones (vacía si no hay ninguno)
    }

    //Métodos CRUD
    public void agregarTarea(String descripcion, java.time.LocalDate fechaLimite, String categoria) {
        Tarea nueva = new Tarea(descripcion);
        nueva.setFechaLimite(fechaLimite);
        nueva.setCategoria(categoria);
        tareas.add(nueva);
        gestor.guardarTareas(tareas);
    }

    // --- V2.0.0e: Interruptor de Estado (Toggle) ---
    public boolean alternarEstadoTarea(int indice) {
        if (existeTarea(indice)) {
            Tarea t = tareas.get(indice - 1);
            
            // Invertimos el valor actual: si era 'true' (completada), pasa a 'false' (pendiente) y viceversa
            boolean nuevoEstado = !t.isCompletada();
            t.setCompletada(nuevoEstado);
            
            // Si la desmarcamos (pasa a pendiente), reiniciamos la notificación para que el vigilante vuelva a avisar
            if (!nuevoEstado) {
                t.setNotificada(false);
            }
            
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
            tareas.get(indice - 1).setNotificada(false);
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
    // --- V2.0.0e: TRABAJADOR EN SEGUNDO PLANO (VIGILANTE RECURRENTE) ---
    private void iniciarVigilanteNotificaciones() {
        Thread vigilante = new Thread(() -> {
            
            int minutosTranscurridos = 0; // NUEVO: Nuestro cronómetro interno

            while (true) { 
                try {
                    java.time.LocalDate hoy = java.time.LocalDate.now();
                    boolean huboCambios = false;

                    // 1. Cada 15 minutos, "olvidamos" que ya notificamos las tareas pendientes de hoy
                    if (minutosTranscurridos >= 15) {
                        for (Tarea t : tareas) {
                            if (!t.isCompletada() && t.getFechaLimite() != null && t.getFechaLimite().isEqual(hoy)) {
                                t.setNotificada(false); // La reseteamos para que vuelva a sonar
                                huboCambios = true;
                            }
                        }
                        minutosTranscurridos = 0; // Reiniciamos el cronómetro
                    }

                    // 2. Revisamos silenciosamente todas las tareas
                    for (Tarea t : tareas) {
                        if (!t.isCompletada() && t.getFechaLimite() != null) {
                            if (t.getFechaLimite().isEqual(hoy) && !t.isNotificada()) {
                                // ¡Lanzamos la alerta a Windows!
                                enviarNotificacionWindows("⏰ Tarea pendiente", t.getDescripcion());
                                
                                // Marcamos que ya le avisamos al usuario
                                t.setNotificada(true); 
                                huboCambios = true;
                            }
                        }
                    }

                    // 3. Si modificamos alguna tarea, guardamos en el JSON
                    if (huboCambios) {
                        gestor.guardarTareas(tareas);
                    }

                    // 4. Mandamos al vigilante a dormir por 1 minuto (60,000 milisegundos)
                    Thread.sleep(60000); 
                    
                    // 5. Sumamos 1 minuto a nuestro cronómetro al despertar
                    minutosTranscurridos++; 

                } catch (InterruptedException e) {
                    break; 
                } catch (Exception e) {
                    System.out.println("Error en el vigilante asíncrono: " + e.getMessage());
                }
            }
        });

        vigilante.setDaemon(true);
        vigilante.start();
    }
    // --- V2.0.0e: MOTOR DE NOTIFICACIONES NATIVAS ---
    public void enviarNotificacionWindows(String titulo, String mensaje) {
        // 1. Verificamos si el sistema operativo soporta notificaciones de bandeja
        if (java.awt.SystemTray.isSupported()) {
            try {
                // 2. Obtenemos acceso a la bandeja del sistema (System Tray)
                java.awt.SystemTray bandeja = java.awt.SystemTray.getSystemTray();
                
                // 3. Creamos una imagen invisible de 1x1 pixel para que no de error de icono
                java.awt.Image imagenInvisible = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                
                // 4. Preparamos el icono de la notificación
                java.awt.TrayIcon iconoNotificacion = new java.awt.TrayIcon(imagenInvisible, "Mi ToDo List");
                iconoNotificacion.setImageAutoSize(true);
                
                // 5. Agregamos el icono a Windows y disparamos el mensaje
                bandeja.add(iconoNotificacion);
                iconoNotificacion.displayMessage(titulo, mensaje, java.awt.TrayIcon.MessageType.INFO);
                
                // 6. Retiramos el icono inmediatamente para no dejar basura en la barra de tareas
                bandeja.remove(iconoNotificacion);
                
            } catch (Exception e) {
                System.out.println("Error al intentar mostrar la notificación nativa: " + e.getMessage());
            }
        }
    }
}