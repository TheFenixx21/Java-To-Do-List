package com.mitodolist;

public class Categoria {
    
    // --- ATRIBUTOS ---
    private int id; // El número de placa en la base de datos
    private String nombre; // Ej: "Trabajo", "Estudios"
    private String color; // Ej: "#FFFFFF" (Para la futura interfaz dinámica)

    // --- CONSTRUCTOR ---
    public Categoria(int id, String nombre, String color) {
        this.id = id;
        this.nombre = nombre;
        this.color = color;
    }

    // --- GETTERS Y SETTERS ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    // Un truco clave para la interfaz gráfica:
    // Cuando JavaFX intente pintar este objeto en un menú desplegable, 
    // usará este método toString() para saber qué texto mostrar.
    @Override
    public String toString() {
        return this.nombre; 
    }
}