package com.mitodolist;
import java.util.ArrayList;

public class ToDoList {
    
    private ArrayList<Tarea> tareas;

    // --- V5.0.0e: MOTOR DE ORDENAMIENTO INTELIGENTE (ACTUALIZADO) ---
    public static final java.util.Comparator<Tarea> ORDENADOR_TAREAS = (t1, t2) -> {
        // Regla 1: Las completadas SIEMPRE van al fondo de la lista general
        if (t1.isCompletada() != t2.isCompletada()) {
            return t1.isCompletada() ? 1 : -1; // Si t1 está completada, la empuja hacia abajo (+1)
        }

        // Si llegamos aquí, ambas tienen el mismo estado (ambas pendientes o ambas completadas)
        java.time.LocalDate fecha1 = t1.getFechaLimite();
        java.time.LocalDate fecha2 = t2.getFechaLimite();

        // Si ambas son tareas sin fecha
        if (fecha1 == null && fecha2 == null) {
            // Si están completadas, ordenamos por ID de mayor a menor (las creadas más recientemente van arriba)
            if (t1.isCompletada()) {
                return Integer.compare(t2.getId(), t1.getId());
            }
            return 0; // Si están pendientes, conservan su orden de creación por defecto
        }
        
        // Las tareas SIN fecha se van al fondo de su respectivo grupo
        if (fecha1 == null) return 1; 
        if (fecha2 == null) return -1;

        // Regla 2: Comparamos las fechas dependiendo de su estado
        int comparacionFechas;
        if (t1.isCompletada()) {
            // Si están COMPLETADAS: Orden Inverso (Fechas más recientes arriba)
            comparacionFechas = fecha2.compareTo(fecha1); 
        } else {
            // Si están PENDIENTES: Orden Cronológico (Fechas más urgentes/antiguas arriba)
            comparacionFechas = fecha1.compareTo(fecha2);
        }

        // Regla 3: Si tienen la misma fecha y están completadas, desempatamos por el ID más reciente
        if (comparacionFechas == 0 && t1.isCompletada()) {
            return Integer.compare(t2.getId(), t1.getId());
        }

        return comparacionFechas;
    };

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

    // ==========================================
    // MÉTODOS CRUD BASADOS EN OBJETOS (V7.0.0e)
    // ==========================================
    
    // Método actualizado para soportar horas y rutinas
    public void agregarTarea(String descripcion, java.time.LocalDate fechaLimite, java.time.LocalTime horaLimite, String repeticion, String categoria) {
        Tarea nueva = new Tarea(descripcion);
        nueva.setFechaLimite(fechaLimite);
        
        // 🚨 Inyectamos las nuevas características de tiempo y repetición
        nueva.setHoraLimite(horaLimite);
        nueva.setTipoRepeticion(repeticion);
        
        nueva.setCategoria(categoria);
        
        // Guardamos en RAM
        tareas.add(nueva);
        
        // Guardamos en SQLite
        GestorBaseDatos.insertarTarea(nueva, GestorBaseDatos.obtenerIdCategoria(categoria)); 
    }
    
    public void alternarEstadoTarea(Tarea t) {
        boolean nuevoEstado = !t.isCompletada();
        
        // --- 🚨 V7.0.0e: MOTOR DE RESURRECCIÓN PARA RUTINAS ---
        if (nuevoEstado == true && t.getTipoRepeticion() != null && !t.getTipoRepeticion().equals("NINGUNA")) {
            
            java.time.LocalDate proximaFecha = calcularProximaFecha(t.getFechaLimite(), t.getTipoRepeticion());
            
            if (proximaFecha != null) {
                // 1. Creamos un CLON fantasma para guardar el historial
                Tarea clonHistorial = new Tarea(t.getDescripcion());
                clonHistorial.setCompletada(true);
                clonHistorial.setFechaLimite(t.getFechaLimite()); // Guardamos qué día se cumplió realmente
                clonHistorial.setHoraLimite(t.getHoraLimite());
                clonHistorial.setTipoRepeticion("HISTORIAL"); // 👻 Etiqueta secreta
                
                GestorBaseDatos.insertarTarea(clonHistorial, GestorBaseDatos.obtenerIdCategoria(t.getCategoria()));
                
                // 2. Empujamos la tarea principal hacia el futuro
                t.setFechaLimite(proximaFecha);
                t.setNotificada(false);
                GestorBaseDatos.actualizarTarea(t);
                
                // 3. Recargamos la RAM para que la interfaz gráfica lo note al instante
                this.sincronizarConBaseDatos(); 
                
                System.out.println("🔄 Rutina cumplida. Historial guardado y tarea empujada al: " + proximaFecha);
                return; // Cortamos el flujo aquí para no desmarcar la tarea actual
            }
        }

        // --- COMPORTAMIENTO NORMAL PARA TAREAS ÚNICAS ---
        t.setCompletada(nuevoEstado);
        
        if (!nuevoEstado) {
            t.setNotificada(false);
        }
        GestorBaseDatos.actualizarTarea(t); 
    }

    public void eliminarTarea(Tarea t) {
        tareas.remove(t); 
        // 🚨 V7.0.1e: Le pasamos la descripción para que cace a los fantasmas
        GestorBaseDatos.eliminarTareaBD(t.getId(), t.getDescripcion());
    }

    public void editarTarea(Tarea t, String nuevaDescripcion) {
        t.setDescripcion(nuevaDescripcion);
        GestorBaseDatos.actualizarTarea(t);
    }

    // 🚨 NUEVO V7: Reemplaza al antiguo editarFechaLimite
    public void editarDetallesTiempo(Tarea t, java.time.LocalDate nuevaFecha, java.time.LocalTime nuevaHora, String nuevaRepeticion) {
        t.setFechaLimite(nuevaFecha);
        t.setHoraLimite(nuevaHora);
        t.setTipoRepeticion(nuevaRepeticion);
        
        // Reseteamos la notificación por si el usuario movió la tarea para hoy más tarde
        t.setNotificada(false); 
        
        // Guardamos todo de un solo golpe en la base de datos
        GestorBaseDatos.actualizarTarea(t);
    }

    public void editarCategoria(Tarea t, String nuevaCategoria) {
        t.setCategoria(nuevaCategoria);
        GestorBaseDatos.actualizarTarea(t);
    }

    public void agregarSubTarea(Tarea tareaPadre, String descripcion) {
        // Creamos la subtarea
        Tarea nuevaSub = new Tarea(descripcion);
        nuevaSub.setIdTareaPadre(tareaPadre.getId()); 
        nuevaSub.setCategoria(tareaPadre.getCategoria()); 
        
        // La guardamos en RAM
        tareaPadre.agregarSubTarea(nuevaSub);
        
        // La guardamos en SQLite
        GestorBaseDatos.insertarTarea(nuevaSub, GestorBaseDatos.obtenerIdCategoria(tareaPadre.getCategoria()));
    }

    /**
     * Elimina directamente una subtarea basándose en su ID real de base de datos
     */
    public boolean eliminarSubTarea(Tarea tareaPadre, Tarea subTarea) {
        if (tareaPadre.getSubTareas().remove(subTarea)) {
            GestorBaseDatos.eliminarTareaBD(subTarea.getId(), subTarea.getDescripcion());
            return true;
        }
        return false;
    }

    /**
     * Cambia el estado (completado/pendiente) de una subtarea directamente
     */
    public void alternarEstadoSubTarea(Tarea subTarea) {
        subTarea.setCompletada(!subTarea.isCompletada());
        GestorBaseDatos.actualizarTarea(subTarea);
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
        
        // 🚨 NUEVO: Leemos las configuraciones del usuario en tiempo real
        Configuracion config = GestorConfiguracion.cargarConfiguracion();

        for (Tarea tareaActual : tareas) {
            
            // 👻 NUEVO V7: Los clones de historial son invisibles en la lista principal
            if ("HISTORIAL".equals(tareaActual.getTipoRepeticion())) {
                continue; 
            }

            boolean incluirPorFiltro = false;
            switch (filtro) {
                case 1: 
                    // Si eligió "Ocultar completadas", las sacamos de la vista principal "Todas"
                    if (config.isOcultarCompletadasAuto() && tareaActual.isCompletada()) {
                        incluirPorFiltro = false;
                    } else {
                        incluirPorFiltro = true; 
                    }
                    break;
                case 2: incluirPorFiltro = !tareaActual.isCompletada(); break;
                case 3: incluirPorFiltro = tareaActual.isCompletada(); break;
                case 4: incluirPorFiltro = !tareaActual.isCompletada() && tareaActual.getFechaLimite() != null && tareaActual.getFechaLimite().isBefore(hoy); break;
                default: incluirPorFiltro = true;
            }

            boolean incluirPorCategoria = categoriaDeseada.equals("Todas") || tareaActual.getCategoria().equals(categoriaDeseada);

            if (incluirPorFiltro && incluirPorCategoria) {
                resultado.add(tareaActual);
            }
        }
        
        resultado.sort(ORDENADOR_TAREAS);
        return resultado;
    }
    
  // --- V2.0.0e: TRABAJADOR EN SEGUNDO PLANO (MODIFICADO PARA V6) ---
    private void iniciarVigilanteNotificaciones() {
        Thread vigilante = new Thread(() -> {
            int minutosTranscurridos = 0; 

            while (true) { 
                try {
                    java.time.LocalDate hoy = java.time.LocalDate.now();
                    
                    Configuracion config = GestorConfiguracion.cargarConfiguracion();
                    int intervalo = config.getIntervaloNotificaciones();

                    // 🚨 NUEVO: Solo trabaja si el usuario NO ha desactivado la función (0)
                    if (intervalo > 0) {
                        
                        // 1. Reseteo dinámico
                        if (minutosTranscurridos >= intervalo) {
                            for (Tarea principal : tareas) {
                                resetearNotificacionSiEsHoy(principal, hoy);
                                if (principal.getSubTareas() != null) {
                                    for (Tarea hija : principal.getSubTareas()) {
                                        resetearNotificacionSiEsHoy(hija, hoy);
                                    }
                                }
                            }
                            minutosTranscurridos = 0; 
                        }

                        // 2. Evaluación de Notificaciones
                        for (Tarea principal : tareas) {
                            evaluarYLanzarNotificacion(principal, null, hoy);
                            if (principal.getSubTareas() != null) {
                                for (Tarea hija : principal.getSubTareas()) {
                                    evaluarYLanzarNotificacion(hija, principal, hoy);
                                }
                            }
                        }
                    }

                    Thread.sleep(60000); // Pausa de 1 minuto real
                    
                    // Solo sumamos tiempo si el vigilante está activo
                    if (intervalo > 0) {
                        minutosTranscurridos++; 
                    }

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

    // --- MÉTODOS DE AYUDA (HELPER METHODS) PARA MANTENER EL CÓDIGO LIMPIO ---
    
    private void resetearNotificacionSiEsHoy(Tarea t, java.time.LocalDate hoy) {
        if (!t.isCompletada() && t.getFechaLimite() != null && t.getFechaLimite().isEqual(hoy)) {
            t.setNotificada(false); 
            GestorBaseDatos.actualizarTarea(t);
        }
    }

    private void evaluarYLanzarNotificacion(Tarea t, Tarea tareaPadre, java.time.LocalDate hoy) {
        if (!t.isCompletada() && t.getFechaLimite() != null) {
            if (t.getFechaLimite().isEqual(hoy) && !t.isNotificada()) {
                
                // 🚨 NUEVO: Si tiene hora, verificamos que ya haya llegado el momento
                if (t.getHoraLimite() != null) {
                    java.time.LocalTime ahora = java.time.LocalTime.now();
                    if (ahora.isBefore(t.getHoraLimite())) {
                        return; // Aún no es la hora, abortamos la notificación por ahora
                    }
                }
                
                String titulo = "⏰ Tarea pendiente";
                String mensaje = t.getDescripcion();
                if (tareaPadre != null) mensaje = t.getDescripcion() + " (Pertenece a: " + tareaPadre.getDescripcion() + ")";
                
                enviarNotificacionWindows(titulo, mensaje);
                t.setNotificada(true);
                GestorBaseDatos.actualizarTarea(t); 
            }
        }
    }

    // --- V2.0.0e: MOTOR DE NOTIFICACIONES NATIVAS ---
    public void enviarNotificacionWindows(String titulo, String mensaje) {
        if (java.awt.SystemTray.isSupported()) {
            try {
                Configuracion config = GestorConfiguracion.cargarConfiguracion();
                java.awt.TrayIcon.MessageType tipoMensaje = config.isSonidoNotificaciones() 
                        ? java.awt.TrayIcon.MessageType.INFO 
                        : java.awt.TrayIcon.MessageType.NONE;

                java.awt.SystemTray bandeja = java.awt.SystemTray.getSystemTray();
                java.awt.Image imagenInvisible = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                java.awt.TrayIcon iconoNotificacion = new java.awt.TrayIcon(imagenInvisible, "Mi ToDo List");
                iconoNotificacion.setImageAutoSize(true);
                
                bandeja.add(iconoNotificacion);
                iconoNotificacion.displayMessage(titulo, mensaje, tipoMensaje); 
                
                // 🚨 PARCHE ANTI-CRASHEO WINDOWS: 
                // Esperamos 5 segundos en un hilo secundario antes de borrar el ícono
                // para darle tiempo a la API de Windows a procesar el globo de texto.
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        bandeja.remove(iconoNotificacion);
                    } catch (Exception e) {}
                }).start();
                
            } catch (Exception e) {
                System.out.println("Error al mostrar notificación: " + e.getMessage());
            }
        }
    }

   /**
     * Calcula la próxima fecha basándose en rutinas fijas O en intervalos personalizados de días.
     */
    private java.time.LocalDate calcularProximaFecha(java.time.LocalDate fechaActual, String tipoRepeticion) {
        if (fechaActual == null || tipoRepeticion == null) return null;
        
        String rep = tipoRepeticion.toUpperCase().trim();

        switch (rep) {
            case "DIARIA": return fechaActual.plusDays(1);
            case "SEMANAL": return fechaActual.plusWeeks(1);
            case "MENSUAL": return fechaActual.plusMonths(1);
            case "NINGUNA": 
            case "HISTORIAL": 
                return null;
            default:
                // 🧠 MAGIA LIBRE: Si el usuario escribió un número (Ej: "3"), sumamos esos días
                try {
                    int diasLibres = Integer.parseInt(rep);
                    return fechaActual.plusDays(diasLibres);
                } catch (NumberFormatException e) {
                    return null; // Si escribió letras sin sentido, no se repite
                }
        }
    }
}