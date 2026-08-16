package com.mitodolist;

/**
 * Objeto de Transferencia de Datos (DTO) para la sincronización en la Nube.
 * Solo contiene datos primitivos universales compatibles con Android y JSON.
 */
public class TareaDTO {
    
    // Identificadores universales
    public String uuid;
    public String categoria_uuid; 
    public String tarea_padre_uuid; // Null si es tarea principal
    
    // Datos puros de la tarea
    public String descripcion; // Ya irá encriptada en AES-128
    public boolean completada;
    public String fecha_vencimiento; // Formato ISO: "YYYY-MM-DD"
    public String hora_vencimiento;  // Formato ISO: "HH:MM"
    public String tipo_repeticion;
    
    // Banderas de Sincronización
    public long fecha_modificacion; // Timestamp Epoch
    public boolean eliminado; // Soft Delete para la Regla de los 30 días

    // Constructor vacío requerido por bibliotecas JSON como Gson
    public TareaDTO() {}
}