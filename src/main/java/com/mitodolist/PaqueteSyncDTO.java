package com.mitodolist;

import java.util.List;
import java.util.ArrayList;

/**
 * El "Sobre" maestro que contendrá absolutamente toda la información del usuario
 * lista para viajar por la red o guardarse en Google Drive.
 */
public class PaqueteSyncDTO {
    public long timestamp_sincronizacion; // Para saber cuándo fue la última vez que se generó este archivo
    
    // Las listas con nuestros objetos puros de red
    public List<CategoriaDTO> categorias;
    public List<TareaDTO> tareas;
    public List<HabitoDTO> habitos;

    public PaqueteSyncDTO() {
        this.categorias = new ArrayList<>();
        this.tareas = new ArrayList<>();
        this.habitos = new ArrayList<>();
    }
}