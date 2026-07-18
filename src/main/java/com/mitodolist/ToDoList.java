package com.mitodolist;
import java.util.ArrayList;

public class ToDoList {
    
    private ArrayList<Tarea> tareas;
    //private GestorArchivos gestor;

    public ToDoList() {
        // Le pedimos al nuevo motor que vaya a SQLite y traiga las tareas
        this.tareas = GestorBaseDatos.cargarTareasDesdeBD(); 
        iniciarVigilanteNotificaciones(); 
    }

    public void sincronizarConBaseDatos() {
        this.tareas = GestorBaseDatos.cargarTareasDesdeBD();
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
        
        // 1. Guardamos en la memoria RAM para que la interfaz gráfica (JavaFX) lo vea al instante
        tareas.add(nueva);
        
        // 2. Guardamos en SQLite usando el bisturí INSERT
        // (Nota: Enviamos el ID de categoría 1 por defecto. Esto lo haremos dinámico más adelante)
        GestorBaseDatos.insertarTarea(nueva, GestorBaseDatos.obtenerIdCategoria(categoria)); 
    }

    public boolean alternarEstadoTarea(int indice) {
        if (existeTarea(indice)) {
            Tarea t = tareas.get(indice - 1);
            boolean nuevoEstado = !t.isCompletada();
            t.setCompletada(nuevoEstado);
            
            if (!nuevoEstado) {
                t.setNotificada(false);
            }
            
            // Usamos el bisturí UPDATE para actualizar SÓLO esta tarea en la base de datos
            GestorBaseDatos.actualizarTarea(t);
            return true;
        }
        return false;
    }

    public boolean eliminarTarea(int indice) {
        if (existeTarea(indice)) {
            // 1. Sacamos la tarea de la memoria RAM y la capturamos en la variable 't'
            Tarea t = tareas.remove(indice - 1);
            
            // 2. Le pasamos el número de placa (ID) al bisturí DELETE de SQLite
            GestorBaseDatos.eliminarTareaBD(t.getId());
            return true;
        }
        return false;
    }

    public boolean editarTarea(int indice, String nuevaDescripcion) {
        if (existeTarea(indice)) {
            Tarea t = tareas.get(indice - 1);
            t.setDescripcion(nuevaDescripcion);
            
            GestorBaseDatos.actualizarTarea(t); // Bisturí UPDATE
            return true;
        }
        return false;
    }

    public boolean editarFechaLimite(int indice, java.time.LocalDate nuevaFecha) {
        if (existeTarea(indice)) {
            Tarea t = tareas.get(indice - 1);
            t.setFechaLimite(nuevaFecha);
            t.setNotificada(false);
            
            GestorBaseDatos.actualizarTarea(t); // Bisturí UPDATE
            return true;
        }
        return false;
    }

    public boolean editarCategoria(int indice, String nuevaCategoria) {
        if (existeTarea(indice)) {
            Tarea t = tareas.get(indice - 1);
            t.setCategoria(nuevaCategoria);
            
            GestorBaseDatos.actualizarTarea(t); // Bisturí UPDATE
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

                    // 1. Cada 15 minutos, "olvidamos" que ya notificamos las tareas pendientes de hoy
                    if (minutosTranscurridos >= 15) {
                        for (Tarea t : tareas) {
                            if (!t.isCompletada() && t.getFechaLimite() != null && t.getFechaLimite().isEqual(hoy)) {
                                t.setNotificada(false); // La reseteamos para que vuelva a sonar
                                GestorBaseDatos.actualizarTarea(t);
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
                                GestorBaseDatos.actualizarTarea(t); 
                            }
                        }
                    }

                    // 3. Mandamos al vigilante a dormir por 1 minuto (60,000 milisegundos)
                    Thread.sleep(60000); 
                    
                    // 4. Sumamos 1 minuto a nuestro cronómetro al despertar
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