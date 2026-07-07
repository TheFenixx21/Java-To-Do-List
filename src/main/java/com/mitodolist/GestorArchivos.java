package com.mitodolist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class GestorArchivos {
    
    private final String ARCHIVO_JSON = "tareas.json";
    private Gson gson;

    public GestorArchivos() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void guardarTareas(ArrayList<Tarea> listaTareas) {
        try (FileWriter escritor = new FileWriter(ARCHIVO_JSON)) {
            gson.toJson(listaTareas, escritor);
        } catch (IOException e) {
            System.out.println("Error crítico al intentar guardar las tareas: " + e.getMessage());
        }
    }

    public ArrayList<Tarea> cargarTareas() {
        try (FileReader lector = new FileReader(ARCHIVO_JSON)) {
            Type tipoLista = new TypeToken<ArrayList<Tarea>>(){}.getType();
            
            ArrayList<Tarea> tareasRecuperadas = gson.fromJson(lector, tipoLista);
            
            if (tareasRecuperadas != null) {
                return tareasRecuperadas;
            }
        } catch (IOException e) {
        }
        
        return new ArrayList<>();
    }
}