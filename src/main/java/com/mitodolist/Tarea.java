package com.mitodolist;

import java.time.LocalDate;

public class Tarea {
    
    private String descripcion;
    private boolean completada;
    private LocalDate fechaLimite;
    private String categoria; // Nueva propiedad para la categoría

    public Tarea(String descripcion) {
        this.descripcion = descripcion;
        this.completada = false; 
        this.fechaLimite = null;
        this.categoria = "Sin categoría";
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
}