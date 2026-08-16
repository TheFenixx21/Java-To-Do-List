package com.mitodolist;

public class CategoriaDTO {
    public String uuid;
    public String nombre;
    public String color;
    public String tipo; // "AMBOS", "TAREAS", etc.
    
    public long fecha_modificacion;
    public boolean eliminado;

    public CategoriaDTO() {}
}