package com.mitodolist;

public class Configuracion {
    
    // --- ⚙️ COMPORTAMIENTO DEL SISTEMA ---
    private int intervaloNotificaciones;
    private boolean arranqueAutomatico;  
    private boolean sonidoNotificaciones;
    private boolean ocultarCompletadasAuto;

    // --- 🛡️ PRIVACIDAD Y SEGURIDAD ---
    private int bloqueoInactividad; 
    private boolean modoPrivacidad;

    // --- 🎨 PERSONALIZACIÓN (UI/UX) ---
    private String colorAcento;
    private boolean temaClaro;  
    private String formatoFecha; 

    // Constructor por defecto (Los valores de fábrica de la aplicación)
    public Configuracion() {
        this.intervaloNotificaciones = 15;
        this.arranqueAutomatico = true;
        this.sonidoNotificaciones = true;
        this.ocultarCompletadasAuto = false;
        
        this.bloqueoInactividad = 0;
        this.modoPrivacidad = false;
        
        this.colorAcento = "#C2185B";
        this.temaClaro = false;
        this.formatoFecha = "dd/MM/yyyy";
    }

    // Constructor para cuando leemos desde SQLite
    public Configuracion(int intervaloNotificaciones, boolean arranqueAutomatico, boolean sonidoNotificaciones, 
                         boolean ocultarCompletadasAuto, int bloqueoInactividad, boolean modoPrivacidad, 
                         String colorAcento, boolean temaClaro, String formatoFecha) {
        this.intervaloNotificaciones = intervaloNotificaciones;
        this.arranqueAutomatico = arranqueAutomatico;
        this.sonidoNotificaciones = sonidoNotificaciones;
        this.ocultarCompletadasAuto = ocultarCompletadasAuto;
        this.bloqueoInactividad = bloqueoInactividad;
        this.modoPrivacidad = modoPrivacidad;
        this.colorAcento = colorAcento;
        this.temaClaro = temaClaro;
        this.formatoFecha = formatoFecha;
    }

    // --- GETTERS Y SETTERS ---
    public int getIntervaloNotificaciones() { return intervaloNotificaciones; }
    public void setIntervaloNotificaciones(int intervaloNotificaciones) { this.intervaloNotificaciones = intervaloNotificaciones; }

    public boolean isArranqueAutomatico() { return arranqueAutomatico; }
    public void setArranqueAutomatico(boolean arranqueAutomatico) { this.arranqueAutomatico = arranqueAutomatico; }

    public boolean isSonidoNotificaciones() { return sonidoNotificaciones; }
    public void setSonidoNotificaciones(boolean sonidoNotificaciones) { this.sonidoNotificaciones = sonidoNotificaciones; }

    public boolean isOcultarCompletadasAuto() { return ocultarCompletadasAuto; }
    public void setOcultarCompletadasAuto(boolean ocultarCompletadasAuto) { this.ocultarCompletadasAuto = ocultarCompletadasAuto; }

    public int getBloqueoInactividad() { return bloqueoInactividad; }
    public void setBloqueoInactividad(int bloqueoInactividad) { this.bloqueoInactividad = bloqueoInactividad; }

    public boolean isModoPrivacidad() { return modoPrivacidad; }
    public void setModoPrivacidad(boolean modoPrivacidad) { this.modoPrivacidad = modoPrivacidad; }

    public String getColorAcento() { return colorAcento; }
    public void setColorAcento(String colorAcento) { this.colorAcento = colorAcento; }

    public boolean isTemaClaro() { return temaClaro; }
    public void setTemaClaro(boolean temaClaro) { this.temaClaro = temaClaro; }

    public String getFormatoFecha() { return formatoFecha; }
    public void setFormatoFecha(String formatoFecha) { this.formatoFecha = formatoFecha; }
}