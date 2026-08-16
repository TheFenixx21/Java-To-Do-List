package com.mitodolist;

import java.time.LocalDate;
import java.util.ArrayList;

public class Tarea {
    
    // --- ATRIBUTOS DE LA CLASE ---
    private int id;
    private String descripcion;
    private boolean completada;
    private LocalDate fechaLimite;
    private String categoria;
    private transient boolean notificada;
    private Integer idTareaPadre;
    private ArrayList<Tarea> subTareas;
    private transient boolean expandida;
    private java.time.LocalTime horaLimite; // Permite guardar la hora exacta
    private String tipoRepeticion; // Puede ser: "NINGUNA", "DIARIA", "SEMANAL", "MENSUAL"
    // --- ATRIBUTOS DE RED Y SINCRONIZACIÓN (V8.0.0e) ---
    private String uuid;
    private long fechaModificacion;
    private String estadoSync; // "PENDIENTE", "SINCRONIZADO"
    private boolean eliminado; // Soft Delete

    // --- CONSTRUCTOR ---
    public Tarea(String descripcion) {
        this.descripcion = descripcion;
        this.completada = false; 
        this.fechaLimite = null;
        this.categoria = "Sin categoría";
        this.notificada = false;
        this.idTareaPadre = null; 
        this.subTareas = new ArrayList<>();
        this.expandida = true;
        this.horaLimite = null; 
        this.tipoRepeticion = "NINGUNA";
    }

    // --- GETTERS Y SETTERS ORIGINALES ---
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescripcion() {
        return this.descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isCompletada() {
        return this.completada;
    }

    public void setCompletada(boolean completada) {
        this.completada = completada;
    }

    public LocalDate getFechaLimite() {
        return this.fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public String getCategoria() {
        return this.categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public boolean isNotificada() {
        return this.notificada;
    }

    public void setNotificada(boolean notificada) {
        this.notificada = notificada;
    }

    // --- NUEVOS GETTERS Y SETTERS V4.0.0e ---
    public Integer getIdTareaPadre() {
        return this.idTareaPadre;
    }

    public void setIdTareaPadre(Integer idTareaPadre) {
        this.idTareaPadre = idTareaPadre;
    }

    public ArrayList<Tarea> getSubTareas() {
        return this.subTareas;
    }

    public void setSubTareas(ArrayList<Tarea> subTareas) {
        this.subTareas = subTareas;
    }
    
    // --- MÉTODO DE UTILIDAD PARA LA UI ---
    public void agregarSubTarea(Tarea subTarea) {
        this.subTareas.add(subTarea);
    }

    public boolean isExpandida() {
        return this.expandida;
    }

    public void setExpandida(boolean expandida) {
        this.expandida = expandida;
    }

    public java.time.LocalTime getHoraLimite() { return horaLimite; }
    public void setHoraLimite(java.time.LocalTime horaLimite) { this.horaLimite = horaLimite; }

    // --- MÉTODOS DE GESTIÓN DE SINCRONIZACIÓN ---
    public String getTipoRepeticion() { return tipoRepeticion; }
    public void setTipoRepeticion(String tipoRepeticion) { this.tipoRepeticion = tipoRepeticion; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public long getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(long fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    public String getEstadoSync() { return estadoSync; }
    public void setEstadoSync(String estadoSync) { this.estadoSync = estadoSync; }

    public boolean isEliminado() { return eliminado; }
    public void setEliminado(boolean eliminado) { this.eliminado = eliminado; }
}