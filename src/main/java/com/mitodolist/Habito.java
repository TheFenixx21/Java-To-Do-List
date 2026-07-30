package com.mitodolist;

import java.time.LocalDate;

public class Habito {
    private int id;
    private String nombre;
    private int idCategoria;
    private String colorHex;
    private LocalDate fechaCreacion;

    public Habito(int id, String nombre, int idCategoria, String colorHex, LocalDate fechaCreacion) {
        this.id = id;
        this.nombre = nombre;
        this.idCategoria = idCategoria;
        this.colorHex = colorHex;
        this.fechaCreacion = fechaCreacion;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public int getIdCategoria() { return idCategoria; }
    public String getColorHex() { return colorHex; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }
}